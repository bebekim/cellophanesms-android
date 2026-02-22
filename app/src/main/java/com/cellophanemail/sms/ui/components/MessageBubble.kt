package com.cellophanemail.sms.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.data.remote.DocumentDto
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.IlluminatedStyle
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.TextAnnotation
import com.cellophanemail.sms.ui.components.text.DropCapText
import com.cellophanemail.sms.ui.components.text.EnrichedMessageText
import com.cellophanemail.sms.ui.components.text.ServerComposedText
import com.cellophanemail.sms.ui.theme.BubbleIncoming
import com.cellophanemail.sms.ui.theme.BubbleOutgoing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    annotations: List<TextAnnotation> = emptyList(),
    illuminatedStyle: IlluminatedStyle? = null,
    entityHighlightsEnabled: Boolean = false,
    isGuest: Boolean = false,
    onEntityClick: (AnnotationType, String) -> Unit = { _, _ -> },
    onSignInClick: () -> Unit = {},
    serverComposedDocument: DocumentDto? = null,
    modifier: Modifier = Modifier
) {
    val isOwn = !message.isIncoming
    val displayText = message.displayContent

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
                containerColor = if (isOwn) BubbleOutgoing else BubbleIncoming
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    serverComposedDocument != null -> {
                        ServerComposedText(
                            document = serverComposedDocument,
                            onEntityClick = onEntityClick
                        )
                    }
                    illuminatedStyle != null && annotations.isNotEmpty() && !isGuest -> {
                        DropCapText(
                            text = displayText,
                            annotations = annotations,
                            illuminatedStyle = illuminatedStyle,
                            onEntityClick = onEntityClick
                        )
                    }
                    illuminatedStyle != null && annotations.isNotEmpty() && isGuest -> {
                        var nudgeDismissed by remember { mutableStateOf(false) }
                        EnrichedMessageText(
                            text = displayText,
                            annotations = annotations,
                            onEntityClick = onEntityClick
                        )
                        if (!nudgeDismissed) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "✨ Sign in for illuminated reading",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onSignInClick) {
                                        Text(
                                            text = "Sign in",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    TextButton(onClick = { nudgeDismissed = true }) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    entityHighlightsEnabled && annotations.isNotEmpty() -> {
                        EnrichedMessageText(
                            text = displayText,
                            annotations = annotations,
                            onEntityClick = onEntityClick
                        )
                    }
                    else -> {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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

private fun formatMessageTime(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}
