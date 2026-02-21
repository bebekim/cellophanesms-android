package com.cellophanemail.sms.domain.annotation.ner

data class NerExtractionResult(
    val entities: List<NerEntity>,
    val tone: String? = null
)
