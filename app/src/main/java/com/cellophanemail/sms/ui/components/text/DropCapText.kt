package com.cellophanemail.sms.ui.components.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.IlluminatedStyle
import com.cellophanemail.sms.domain.model.TextAnnotation

private const val MIN_DROP_CAP_LENGTH = 10

@Composable
fun DropCapText(
    text: String,
    annotations: List<TextAnnotation>,
    illuminatedStyle: IlluminatedStyle,
    onEntityClick: (AnnotationType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstChar = text.firstOrNull()

    // Fall back to plain enriched text if text is too short or doesn't start with a letter
    if (text.length < MIN_DROP_CAP_LENGTH || firstChar == null || !firstChar.isLetter()) {
        EnrichedMessageText(
            text = text,
            annotations = annotations,
            onEntityClick = onEntityClick,
            modifier = modifier
        )
        return
    }

    val bodyText = text.substring(1)

    // Shift annotation indices by -1 for the body portion
    val shiftedAnnotations = annotations.mapNotNull { ann ->
        val newStart = ann.startIndex - 1
        val newEnd = ann.endIndex - 1
        when {
            // Annotation is entirely in the first character — skip
            ann.endIndex <= 1 -> null
            // Annotation starts at the first character — truncate
            ann.startIndex == 0 -> ann.copy(
                startIndex = 0,
                endIndex = newEnd
            ).takeIf { it.endIndex > 0 }
            // Normal shift
            else -> ann.copy(
                startIndex = newStart,
                endIndex = newEnd
            )
        }
    }

    Row(modifier = modifier) {
        IlluminatedInitial(
            letter = firstChar,
            style = illuminatedStyle
        )

        Spacer(modifier = Modifier.width(4.dp))

        EnrichedMessageText(
            text = bodyText,
            annotations = shiftedAnnotations,
            onEntityClick = onEntityClick,
            modifier = Modifier.weight(1f)
        )
    }
}
