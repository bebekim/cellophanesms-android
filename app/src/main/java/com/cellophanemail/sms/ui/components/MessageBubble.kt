package com.cellophanemail.sms.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.R
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.ui.theme.BubbleFiltered
import com.cellophanemail.sms.ui.theme.BubbleIncoming
import com.cellophanemail.sms.ui.theme.BubbleOutgoing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    showOriginal: Boolean = false,
    onToggleOriginal: () -> Unit,
    contactName: String? = null,
    modifier: Modifier = Modifier
) {
    val isOwn = !message.isIncoming
    val isFiltered = message.isFiltered && message.isIncoming

    // Determine which content to display
    val displayText = if (isFiltered && showOriginal) {
        message.originalContent
    } else {
        message.displayContent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwn) 16.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isOwn -> BubbleOutgoing
                    isFiltered -> BubbleFiltered
                    else -> BubbleIncoming
                }
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Filtered Header (only show when not viewing original)
                if (isFiltered && !showOriginal) {
                    FilteredMessageHeader(message, contactName)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Toggle Original Button
                if (isFiltered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onToggleOriginal,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = stringResource(
                                if (showOriginal) R.string.hide_original else R.string.view_original
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Timestamp
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun FilteredMessageHeader(message: Message, contactName: String? = null) {
    val displayName = contactName ?: message.address
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Filtered",
                tint = message.classification?.toColor()
                    ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = stringResource(R.string.filtered_by),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = message.classification?.toColor()
                        ?: MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.from_sender, displayName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}
