package com.cellophanemail.sms.domain.model

import java.util.UUID

data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val type: AnnotationType,
    val startIndex: Int,
    val endIndex: Int,
    val label: String = "",
    val confidence: Float = 1.0f,
    val source: String = "",
    val priority: Int = 0,
    val metadata: Map<String, String> = emptyMap()
) {
    val length: Int get() = endIndex - startIndex

    fun overlaps(other: TextAnnotation): Boolean =
        startIndex < other.endIndex && endIndex > other.startIndex
}

enum class AnnotationType {
    // Phase 1 — regex-detected entities
    DATE_TIME,
    URL,
    EMAIL,
    PHONE_NUMBER,

    // Phase 2 — Gemini Nano NER (forward-declared)
    PERSON_NAME,
    LOCATION,
    ORGANIZATION,

    // Phase 3-4 — toxicity overlays (forward-declared)
    TOXICITY_SPAN,
    HORSEMAN_SPAN
}
