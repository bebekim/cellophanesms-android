package com.cellophanemail.sms.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cellophanemail.sms.R
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MessageProcessingService : Service() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val messageId = intent?.getStringExtra(EXTRA_MESSAGE_ID)

        if (messageId != null) {
            serviceScope.launch {
                try {
                    processMessage(messageId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message $messageId", e)
                } finally {
                    stopSelf(startId)
                }
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

    private suspend fun processMessage(messageId: String) {
        val message = messageRepository.getMessageById(messageId)

        if (message == null) {
            Log.w(TAG, "Message $messageId not found")
            return
        }

        Log.d(TAG, "Processing message $messageId from ${message.address}")

        // Update state to processing
        val processingMessage = message.copy(processingState = ProcessingState.PROCESSING)
        messageRepository.updateMessage(processingMessage)

        // TODO: Step 1 - Local LLM check (Phase 3)
        // For now, skip directly to cloud API

        // Step 2: Cloud API Analysis
        val deviceId = retrieveDeviceId()
        val result = messageRepository.analyzeMessage(processingMessage, deviceId)

        // Step 3: Show notification
        result.fold(
            onSuccess = { analyzedMessage ->
                Log.d(TAG, "Message analyzed: classification=${analyzedMessage.classification}")
                notificationHelper.showMessageNotification(analyzedMessage)
            },
            onFailure = { error ->
                Log.e(TAG, "Analysis failed for message $messageId", error)

                // Show notification with original message on error
                val errorMessage = message.copy(
                    processingState = ProcessingState.ERROR,
                    isFiltered = false
                )
                messageRepository.updateMessage(errorMessage)
                notificationHelper.showMessageNotification(errorMessage, isError = true)
            }
        )
    }

    private fun retrieveDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Message Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Processing incoming messages"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "MessageProcessingService"
        private const val EXTRA_MESSAGE_ID = "message_id"
        private const val CHANNEL_ID = "message_processing"

        fun startProcessing(context: Context, messageId: String) {
            val intent = Intent(context, MessageProcessingService::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
            context.startService(intent)
        }
    }
}
