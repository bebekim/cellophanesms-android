package com.cellophanemail.sms.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cellophanemail.sms.R
import com.cellophanemail.sms.data.contact.ContactResolver
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.ui.thread.ThreadActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactResolver: ContactResolver
) {
    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_FILTERED = "filtered_messages"

        private const val GROUP_MESSAGES = "com.cellophanemail.sms.MESSAGES"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Regular messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_messages_desc)
                enableLights(true)
                enableVibration(true)
            }

            // Filtered messages channel (slightly lower importance)
            val filteredChannel = NotificationChannel(
                CHANNEL_FILTERED,
                "Filtered Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Messages that have been filtered for toxic content"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(filteredChannel)
        }
    }

    fun showMessageNotification(message: Message, isError: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val channel = if (message.isFiltered) CHANNEL_FILTERED else CHANNEL_MESSAGES
        val notificationId = message.threadId.hashCode()

        // Intent to open thread
        val intent = Intent(context, ThreadActivity::class.java).apply {
            putExtra(ThreadActivity.EXTRA_THREAD_ID, message.threadId)
            putExtra(ThreadActivity.EXTRA_ADDRESS, message.address)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification content
        val (title, content) = buildNotificationContent(message, isError)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_MESSAGES)

        // Add filtered indicator
        if (message.isFiltered) {
            val colorRes = when (message.classification) {
                ToxicityClass.WARNING -> R.color.toxicity_warning
                ToxicityClass.HARMFUL -> R.color.toxicity_harmful
                ToxicityClass.ABUSIVE -> R.color.toxicity_abusive
                else -> R.color.toxicity_safe
            }
            builder.setColor(context.getColor(colorRes))
            builder.setSubText("Filtered")
        }

        // Add error indicator
        if (isError) {
            builder.setSubText(context.getString(R.string.analysis_failed))
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun buildNotificationContent(message: Message, isError: Boolean): Pair<String, String> {
        val senderName = contactResolver.lookupContact(message.address)?.displayName
            ?: message.address

        return if (message.isFiltered && message.filteredContent != null && !isError) {
            val title = "$senderName (Filtered)"
            val content = message.filteredContent
            title to content
        } else {
            val title = senderName
            val content = if (isError) {
                "${context.getString(R.string.analysis_failed)}: ${message.originalContent.take(100)}"
            } else {
                message.originalContent.take(200)
            }
            title to content
        }
    }

    fun cancelNotification(threadId: String) {
        val notificationId = threadId.hashCode()
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
