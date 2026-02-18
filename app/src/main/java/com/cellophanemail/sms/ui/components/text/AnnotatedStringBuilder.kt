package com.cellophanemail.sms.ui.components.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import com.cellophanemail.sms.ui.theme.EntityDateTime
import com.cellophanemail.sms.ui.theme.EntityDateTimeDark
import com.cellophanemail.sms.ui.theme.EntityEmail
import com.cellophanemail.sms.ui.theme.EntityEmailDark
import com.cellophanemail.sms.ui.theme.EntityPhone
import com.cellophanemail.sms.ui.theme.EntityPhoneDark
import com.cellophanemail.sms.ui.theme.EntityUrl
import com.cellophanemail.sms.ui.theme.EntityUrlDark
import com.cellophanemail.sms.ui.theme.EntityPersonName
import com.cellophanemail.sms.ui.theme.EntityPersonNameDark
import com.cellophanemail.sms.ui.theme.EntityLocation
import com.cellophanemail.sms.ui.theme.EntityLocationDark
import com.cellophanemail.sms.ui.theme.EntityOrganization
import com.cellophanemail.sms.ui.theme.EntityOrganizationDark

object AnnotatedStringBuilder {

    fun build(
        text: String,
        annotations: List<TextAnnotation>,
        isDarkTheme: Boolean
    ): AnnotatedString = buildAnnotatedString {
        append(text)

        for (annotation in annotations) {
            val start = annotation.startIndex.coerceIn(0, text.length)
            val end = annotation.endIndex.coerceIn(start, text.length)
            if (start >= end) continue

            val color = colorForType(annotation.type, isDarkTheme) ?: continue

            addStyle(
                style = SpanStyle(
                    color = color,
                    textDecoration = TextDecoration.Underline
                ),
                start = start,
                end = end
            )

            // Tag for click handling — stores type and matched text
            addStringAnnotation(
                tag = ENTITY_TAG,
                annotation = "${annotation.type.name}|${text.substring(start, end)}",
                start = start,
                end = end
            )
        }
    }

    fun colorForType(type: AnnotationType, isDarkTheme: Boolean): Color? = when (type) {
        AnnotationType.DATE_TIME -> if (isDarkTheme) EntityDateTimeDark else EntityDateTime
        AnnotationType.URL -> if (isDarkTheme) EntityUrlDark else EntityUrl
        AnnotationType.EMAIL -> if (isDarkTheme) EntityEmailDark else EntityEmail
        AnnotationType.PHONE_NUMBER -> if (isDarkTheme) EntityPhoneDark else EntityPhone
        AnnotationType.PERSON_NAME -> if (isDarkTheme) EntityPersonNameDark else EntityPersonName
        AnnotationType.LOCATION -> if (isDarkTheme) EntityLocationDark else EntityLocation
        AnnotationType.ORGANIZATION -> if (isDarkTheme) EntityOrganizationDark else EntityOrganization
        else -> null // Future types not styled yet
    }

    const val ENTITY_TAG = "entity"
}
