package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.TextAnnotation

interface AnnotationSource {
    val sourceId: String
    val defaultPriority: Int
    val requiresNetwork: Boolean

    suspend fun annotate(text: String): List<TextAnnotation>
}
