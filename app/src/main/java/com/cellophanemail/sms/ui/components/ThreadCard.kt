package com.cellophanemail.sms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.domain.model.Thread
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.ui.theme.ToxicityAbusive
import com.cellophanemail.sms.ui.theme.ToxicityHarmful
import com.cellophanemail.sms.ui.theme.ToxicitySafe
import com.cellophanemail.sms.ui.theme.ToxicityWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThreadCard(
    thread: Thread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Avatar
            ContactAvatar(
                name = thread.displayName,
                toxicityLevel = thread.toxicityLevel
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (thread.unreadCount > 0)
                        FontWeight.Bold else FontWeight.Normal
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (thread.toxicityLevel != ToxicityClass.SAFE) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Filtered",
                            tint = thread.toxicityLevel.toColor(),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = thread.lastMessagePreview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (thread.unreadCount > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timestamp and Badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTimestamp(thread.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = thread.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(
    name: String,
    toxicityLevel: ToxicityClass,
    modifier: Modifier = Modifier
) {
    val initials = name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { name.take(1).uppercase() }

    val backgroundColor = when (toxicityLevel) {
        ToxicityClass.SAFE -> MaterialTheme.colorScheme.primaryContainer
        ToxicityClass.WARNING -> ToxicityWarning.copy(alpha = 0.2f)
        ToxicityClass.HARMFUL -> ToxicityHarmful.copy(alpha = 0.2f)
        ToxicityClass.ABUSIVE -> ToxicityAbusive.copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

fun ToxicityClass.toColor() = when (this) {
    ToxicityClass.SAFE -> ToxicitySafe
    ToxicityClass.WARNING -> ToxicityWarning
    ToxicityClass.HARMFUL -> ToxicityHarmful
    ToxicityClass.ABUSIVE -> ToxicityAbusive
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
