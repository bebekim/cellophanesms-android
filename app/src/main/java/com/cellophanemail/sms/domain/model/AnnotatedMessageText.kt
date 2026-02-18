package com.cellophanemail.sms.domain.model

data class AnnotatedMessageText(
    val text: String,
    val annotations: List<TextAnnotation>
) {
    companion object {
        fun plain(text: String): AnnotatedMessageText =
            AnnotatedMessageText(text = text, annotations = emptyList())
    }
}
