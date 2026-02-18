package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import java.util.regex.Pattern

class RegexEntitySource : AnnotationSource {

    override val sourceId: String = SOURCE_ID
    override val defaultPriority: Int = 100
    override val requiresNetwork: Boolean = false

    override suspend fun annotate(text: String): List<TextAnnotation> {
        if (text.isBlank()) return emptyList()

        val annotations = mutableListOf<TextAnnotation>()
        val occupied = mutableSetOf<IntRange>()

        // Extraction order: email first (avoid URL double-match), then URL, phone, date/time
        extractAll(EMAIL_PATTERN, text, AnnotationType.EMAIL, annotations, occupied)
        extractAll(URL_PATTERN, text, AnnotationType.URL, annotations, occupied)
        extractAll(PHONE_PATTERN, text, AnnotationType.PHONE_NUMBER, annotations, occupied)
        extractAll(DATE_TIME_PATTERN, text, AnnotationType.DATE_TIME, annotations, occupied)

        return annotations
    }

    private fun extractAll(
        pattern: Pattern,
        text: String,
        type: AnnotationType,
        results: MutableList<TextAnnotation>,
        occupied: MutableSet<IntRange>
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Skip if this range overlaps with an already-extracted entity
            if (occupied.any { it.first < end && it.last >= start }) continue

            results.add(
                TextAnnotation(
                    type = type,
                    startIndex = start,
                    endIndex = end,
                    label = type.name,
                    confidence = 1.0f,
                    source = SOURCE_ID,
                    priority = defaultPriority,
                    metadata = mapOf("matched" to text.substring(start, end))
                )
            )
            occupied.add(start until end)
        }
    }

    companion object {
        const val SOURCE_ID = "regex_entity"

        // Email — RFC 5322 simplified
        private val EMAIL_PATTERN: Pattern = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
        )

        // URL — http(s) and www prefixes
        private val URL_PATTERN: Pattern = Pattern.compile(
            "(?:https?://|www\\.)[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
        )

        // Phone — international and US formats, with digit-boundary guards
        private val PHONE_PATTERN: Pattern = Pattern.compile(
            "(?<!\\d)(?:\\+\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{4}(?!\\d)"
        )

        // Date/time — common patterns
        // Matches: MM/DD/YYYY, MM-DD-YYYY, Month DD, YYYY, DD Month YYYY,
        //          tomorrow, today, tonight, yesterday, next Monday, etc.,
        //          HH:MM AM/PM, HH:MM (24h)
        private val DATE_TIME_PATTERN: Pattern = Pattern.compile(
            "(?:" +
                // MM/DD/YYYY or DD/MM/YYYY or YYYY-MM-DD
                "\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}" +
                "|" +
                // Month DD, YYYY or Month DD
                "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{1,2}(?:,?\\s+\\d{4})?" +
                "|" +
                // DD Month YYYY
                "\\d{1,2}\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{4}" +
                "|" +
                // Relative dates
                "(?:today|tonight|tomorrow|yesterday)" +
                "|" +
                // "next/last Monday", "this Friday"
                "(?:next|last|this)\\s+(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)" +
                "|" +
                // Time: 12:30 PM, 2:45pm, 14:30
                "\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?" +
                ")",
            Pattern.CASE_INSENSITIVE
        )
    }
}
