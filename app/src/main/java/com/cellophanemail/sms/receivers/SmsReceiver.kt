package com.cellophanemail.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.services.MessageProcessingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val pendingResult = goAsync()

        scope.launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                // Group messages by sender (multi-part SMS)
                val groupedMessages = mutableMapOf<String, StringBuilder>()

                for (smsMessage in messages) {
                    val sender = smsMessage.originatingAddress ?: "Unknown"
                    val body = smsMessage.messageBody ?: ""

                    groupedMessages.getOrPut(sender) { StringBuilder() }.append(body)
                }

                // Process each complete message
                for ((sender, bodyBuilder) in groupedMessages) {
                    val body = bodyBuilder.toString()
                    val timestamp = System.currentTimeMillis()

                    Log.d(TAG, "Received SMS from $sender: ${body.take(50)}...")

                    val threadId = MessageRepository.generateThreadId(sender)

                    val message = Message(
                        threadId = threadId,
                        address = sender,
                        timestamp = timestamp,
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

                    // Store message immediately
                    messageRepository.insertMessage(message)

                    // Start background processing
                    MessageProcessingService.startProcessing(context, message.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
