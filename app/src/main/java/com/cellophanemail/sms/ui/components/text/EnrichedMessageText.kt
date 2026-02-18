package com.cellophanemail.sms.ui.components.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation

@Composable
fun EnrichedMessageText(
    text: String,
    annotations: List<TextAnnotation>,
    onEntityClick: (AnnotationType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val annotatedString = AnnotatedStringBuilder.build(text, annotations, isDark)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString
                        .getStringAnnotations(AnnotatedStringBuilder.ENTITY_TAG, position, position)
                        .firstOrNull()
                        ?.let { annotation ->
                            val parts = annotation.item.split("|", limit = 2)
                            if (parts.size == 2) {
                                val type = runCatching {
                                    AnnotationType.valueOf(parts[0])
                                }.getOrNull() ?: return@let
                                onEntityClick(type, parts[1])
                            }
                        }
                }
            }
        },
        onTextLayout = { layoutResult = it }
    )
}
