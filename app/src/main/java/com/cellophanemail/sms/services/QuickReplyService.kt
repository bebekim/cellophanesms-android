package com.cellophanemail.sms.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickReplyService : Service() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val messageText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val recipients = intent.dataString?.let { extractPhoneNumber(it) }

            if (messageText != null && recipients != null) {
                serviceScope.launch {
                    sendQuickReply(recipients, messageText)
                    stopSelf(startId)
                }
            } else {
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun sendQuickReply(address: String, messageText: String) {
        try {
            // Send the SMS
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()

            if (messageText.length > 160) {
                val parts = smsManager.divideMessage(messageText)
                smsManager.sendMultipartTextMessage(
                    address,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    address,
                    null,
                    messageText,
                    null,
                    null
                )
            }

            Log.d(TAG, "Quick reply sent to $address")

            // Store the sent message
            val threadId = MessageRepository.generateThreadId(address)
            val message = Message(
                threadId = threadId,
                address = address,
                timestamp = System.currentTimeMillis(),
                isIncoming = false,
                originalContent = messageText,
                filteredContent = null,
                isFiltered = false,
                toxicityScore = null,
                classification = null,
                horsemen = emptyList(),
                reasoning = null,
                processingState = ProcessingState.SAFE,
                isSent = true,
                isRead = true,
                isArchived = false
            )

            messageRepository.insertMessage(message)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send quick reply to $address", e)
        }
    }

    private fun extractPhoneNumber(uri: String): String? {
        // Handle sms:, smsto:, mms:, mmsto: URIs
        return uri
            .removePrefix("sms:")
            .removePrefix("smsto:")
            .removePrefix("mms:")
            .removePrefix("mmsto:")
            .takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "QuickReplyService"
    }
}
