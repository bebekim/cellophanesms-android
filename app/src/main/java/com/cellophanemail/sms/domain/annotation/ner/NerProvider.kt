package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation

data class NerEntity(
    val text: String,
    val type: AnnotationType,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Float
) {
    fun toTextAnnotation(sourceId: String, priority: Int): TextAnnotation = TextAnnotation(
        type = type,
        startIndex = startIndex,
        endIndex = endIndex,
        label = type.name,
        confidence = confidence,
        source = sourceId,
        priority = priority,
        metadata = mapOf("matched" to text)
    )
}

interface NerProvider {
    val providerId: String
    val requiresNetwork: Boolean
    suspend fun isAvailable(): Boolean
    suspend fun extractEntities(text: String): List<NerEntity>
}
