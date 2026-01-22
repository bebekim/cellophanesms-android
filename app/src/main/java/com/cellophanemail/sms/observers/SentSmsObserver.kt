package com.cellophanemail.sms.observers

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.util.PhoneNumberNormalizer
import com.cellophanemail.sms.workers.AnalysisWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes the SMS content provider for outbound (sent) messages.
 * This complements the SmsReceiver which handles inbound messages.
 */
@Singleton
class SentSmsObserver @Inject constructor(
    private val messageRepository: MessageRepository,
    private val analysisWorkManager: AnalysisWorkManager,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastProcessedId: Long = -1
    private var contentResolver: ContentResolver? = null

    fun register(contentResolver: ContentResolver) {
        this.contentResolver = contentResolver
        // Observe the sent SMS content URI
        contentResolver.registerContentObserver(
            Telephony.Sms.Sent.CONTENT_URI,
            true,
            this
        )
        // Also observe the general SMS URI for sent messages
        contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            this
        )
        Log.d(TAG, "Registered sent SMS observer")

        // Initialize with the latest sent message ID
        initializeLastProcessedId(contentResolver)
    }

    fun unregister() {
        contentResolver?.unregisterContentObserver(this)
        contentResolver = null
        Log.d(TAG, "Unregistered sent SMS observer")
    }

    private fun initializeLastProcessedId(contentResolver: ContentResolver) {
        try {
            contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                null,
                null,
                "${Telephony.Sms._ID} DESC LIMIT 1"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    lastProcessedId = cursor.getLong(0)
                    Log.d(TAG, "Initialized lastProcessedId: $lastProcessedId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing lastProcessedId", e)
        }
    }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        scope.launch {
            try {
                processNewSentMessages()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing sent messages", e)
            }
        }
    }

    private suspend fun processNewSentMessages() {
        val resolver = contentResolver ?: return

        try {
            // Query for new sent messages since our last processed ID
            val selection = if (lastProcessedId > 0) {
                "${Telephony.Sms._ID} > ? AND ${Telephony.Sms.TYPE} = ?"
            } else {
                "${Telephony.Sms.TYPE} = ?"
            }

            val selectionArgs = if (lastProcessedId > 0) {
                arrayOf(lastProcessedId.toString(), Telephony.Sms.MESSAGE_TYPE_SENT.toString())
            } else {
                arrayOf(Telephony.Sms.MESSAGE_TYPE_SENT.toString())
            }

            resolver.query(
                Telephony.Sms.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${Telephony.Sms._ID} ASC"
            )?.use { cursor ->
                var newMessagesCount = 0

                while (cursor.moveToNext()) {
                    val message = cursorToMessage(cursor)
                    if (message != null) {
                        // Check if we already have this message
                        val existingMessage = messageRepository.getMessageById(message.id)
                        if (existingMessage == null) {
                            messageRepository.insertMessage(message)
                            newMessagesCount++
                            Log.d(TAG, "Stored outbound SMS to ${message.address}")
                        }

                        // Update last processed ID
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
                        if (id > lastProcessedId) {
                            lastProcessedId = id
                        }
                    }
                }

                if (newMessagesCount > 0) {
                    Log.d(TAG, "Processed $newMessagesCount new outbound messages")
                    // Trigger incremental analysis for new messages
                    analysisWorkManager.triggerIncrementalAnalysis()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying sent messages", e)
        }
    }

    private fun cursorToMessage(cursor: Cursor): Message? {
        return try {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: return null
            val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))

            val normalizedAddress = phoneNumberNormalizer.normalize(address)
            val threadId = MessageRepository.generateThreadId(normalizedAddress)

            Message(
                id = "sent_$id", // Prefix to avoid collision with received messages
                threadId = threadId,
                address = address,
                timestamp = timestamp,
                isIncoming = false, // Outbound message
                originalContent = body,
                filteredContent = null,
                isFiltered = false,
                toxicityScore = null,
                classification = null,
                horsemen = emptyList(),
                reasoning = null,
                processingState = ProcessingState.PENDING,
                isSent = true,
                isRead = true, // Sent messages are always "read"
                isArchived = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sent message from cursor", e)
            null
        }
    }

    companion object {
        private const val TAG = "SentSmsObserver"

        private val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
    }
}
