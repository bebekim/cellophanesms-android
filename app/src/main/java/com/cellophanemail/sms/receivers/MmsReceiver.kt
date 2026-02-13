package com.cellophanemail.sms.receivers

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.workers.AnalysisWorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@AndroidEntryPoint
class MmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var analysisWorkManager: AnalysisWorkManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            return
        }

        Log.d(TAG, "MMS received, processing...")

        val pendingResult = goAsync()

        scope.launch {
            try {
                // Allow the system to write the MMS to the content provider
                delay(MMS_PROCESSING_DELAY_MS)

                val latestMms = getLatestMms(context.contentResolver) ?: run {
                    Log.w(TAG, "Could not find MMS in content provider")
                    return@launch
                }

                val sender = getMmsSender(context.contentResolver, latestMms.id)
                if (sender == null) {
                    Log.w(TAG, "Could not determine MMS sender")
                    return@launch
                }

                val textBody = getMmsTextBody(context.contentResolver, latestMms.id)
                val hasAttachments = hasMmsAttachments(context.contentResolver, latestMms.id)

                // Build display body: text content + attachment indicator
                val body = buildString {
                    if (textBody.isNotBlank()) {
                        append(textBody)
                    }
                    if (hasAttachments) {
                        if (isNotBlank()) append("\n")
                        append("[MMS Attachment]")
                    }
                }

                if (body.isBlank()) {
                    Log.d(TAG, "MMS from $sender has no text content to analyze")
                    return@launch
                }

                Log.d(TAG, "MMS from $sender: ${body.take(50)}...")

                val threadId = MessageRepository.generateThreadId(sender)

                val message = Message(
                    threadId = threadId,
                    address = sender,
                    timestamp = latestMms.timestamp,
                    isIncoming = true,
                    originalContent = body,
                    filteredContent = null,
                    isFiltered = false,
                    toxicityScore = null,
                    classification = null,
                    horsemen = emptyList(),
                    reasoning = null,
                    processingState = ProcessingState.PENDING,
                    isSent = true,
                    isRead = false,
                    isArchived = false
                )

                messageRepository.insertMessage(message)
                analysisWorkManager.triggerIncrementalAnalysis()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing MMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private data class MmsInfo(val id: String, val timestamp: Long)

    private fun getLatestMms(contentResolver: ContentResolver): MmsInfo? {
        val uri = Telephony.Mms.CONTENT_URI
        val projection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE)

        return try {
            contentResolver.query(
                uri, projection, null, null, "${Telephony.Mms.DATE} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms._ID))
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE))
                    // MMS dates are in seconds, convert to millis
                    MmsInfo(id, date * 1000)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MMS", e)
            null
        }
    }

    private fun getMmsSender(contentResolver: ContentResolver, mmsId: String): String? {
        val uri = Uri.parse("content://mms/$mmsId/addr")
        // type 137 = PduHeaders.FROM
        val selection = "${Telephony.Mms.Addr.TYPE}=137"

        return try {
            contentResolver.query(uri, null, selection, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val addressIndex = cursor.getColumnIndex(Telephony.Mms.Addr.ADDRESS)
                    if (addressIndex >= 0) {
                        val address = cursor.getString(addressIndex)
                        // Filter out "insert-address-token" placeholder
                        if (address != null && !address.contains("insert-address-token")) {
                            address
                        } else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MMS sender", e)
            null
        }
    }

    private fun getMmsTextBody(contentResolver: ContentResolver, mmsId: String): String {
        val uri = Uri.parse("content://mms/part")
        val selection = "${Telephony.Mms.Part.MSG_ID}=?"
        val selectionArgs = arrayOf(mmsId)

        val textParts = StringBuilder()

        try {
            contentResolver.query(uri, null, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contentType = getColumnString(cursor, Telephony.Mms.Part.CONTENT_TYPE)

                    if (contentType == "text/plain") {
                        val data = getColumnString(cursor, Telephony.Mms.Part._DATA)
                        val text = if (data != null) {
                            readMmsPartData(contentResolver, cursor)
                        } else {
                            getColumnString(cursor, Telephony.Mms.Part.TEXT)
                        }

                        if (!text.isNullOrBlank()) {
                            if (textParts.isNotEmpty()) textParts.append("\n")
                            textParts.append(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading MMS text", e)
        }

        return textParts.toString()
    }

    private fun readMmsPartData(contentResolver: ContentResolver, cursor: Cursor): String? {
        val partId = getColumnString(cursor, Telephony.Mms.Part._ID) ?: return null
        val partUri = Uri.parse("content://mms/part/$partId")

        return try {
            contentResolver.openInputStream(partUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading MMS part data", e)
            null
        }
    }

    private fun hasMmsAttachments(contentResolver: ContentResolver, mmsId: String): Boolean {
        val uri = Uri.parse("content://mms/part")
        val selection = "${Telephony.Mms.Part.MSG_ID}=?"
        val selectionArgs = arrayOf(mmsId)

        return try {
            contentResolver.query(uri, null, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val contentType = getColumnString(cursor, Telephony.Mms.Part.CONTENT_TYPE)
                    if (contentType != null && contentType.startsWith("image/")) {
                        return@use true
                    }
                }
                false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun getColumnString(cursor: Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        return if (index >= 0) cursor.getString(index) else null
    }

    companion object {
        private const val TAG = "MmsReceiver"
        private const val MMS_PROCESSING_DELAY_MS = 3000L
    }
}
