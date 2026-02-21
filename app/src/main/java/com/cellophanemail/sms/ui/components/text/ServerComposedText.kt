package com.cellophanemail.sms.ui.components.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.cellophanemail.sms.data.remote.DocumentDto
import com.cellophanemail.sms.data.remote.TextBlockDto
import com.cellophanemail.sms.domain.model.AnnotationType

/**
 * Renders a server-composed Document AST into Compose UI.
 *
 * Walks the AST deterministically: blocks → spans → styled AnnotatedString.
 * Reuses the same color mapping and tap handling as [EnrichedMessageText].
 */
@Composable
fun ServerComposedText(
    document: DocumentDto,
    onEntityClick: (AnnotationType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(modifier = modifier) {
        for (block in document.blocks) {
            ServerTextBlock(
                block = block,
                isDark = isDark,
                onEntityClick = onEntityClick
            )
        }

        // Tone badge from document AST
        document.tone?.tone?.let { tone ->
            ToneBadge(tone = tone)
        }
    }
}

@Composable
private fun ServerTextBlock(
    block: TextBlockDto,
    isDark: Boolean,
    onEntityClick: (AnnotationType, String) -> Unit
) {
    val annotatedString = buildAnnotatedStringFromSpans(block, isDark)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.pointerInput(annotatedString) {
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

/**
 * Maps server AST span styles to [AnnotationType] for color lookup.
 */
fun parseDecorationStyle(style: String): AnnotationType? = when (style) {
    "person_name" -> AnnotationType.PERSON_NAME
    "location" -> AnnotationType.LOCATION
    "organization" -> AnnotationType.ORGANIZATION
    "date_time" -> AnnotationType.DATE_TIME
    "url" -> AnnotationType.URL
    "email" -> AnnotationType.EMAIL
    "phone_number" -> AnnotationType.PHONE_NUMBER
    else -> null
}

private fun buildAnnotatedStringFromSpans(
    block: TextBlockDto,
    isDark: Boolean
): AnnotatedString = buildAnnotatedString {
    append(block.text)

    for (span in block.spans) {
        val annotationType = parseDecorationStyle(span.style) ?: continue
        val color = AnnotatedStringBuilder.colorForType(annotationType, isDark) ?: continue

        val start = span.start.coerceIn(0, block.text.length)
        val end = span.end.coerceIn(start, block.text.length)
        if (start >= end) continue

        addStyle(
            style = SpanStyle(
                color = color,
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )

        addStringAnnotation(
            tag = AnnotatedStringBuilder.ENTITY_TAG,
            annotation = "${annotationType.name}|${block.text.substring(start, end)}",
            start = start,
            end = end
        )
    }
}
