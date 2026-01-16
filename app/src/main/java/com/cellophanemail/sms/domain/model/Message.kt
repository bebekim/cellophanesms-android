package com.cellophanemail.sms.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val address: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val originalContent: String,
    val filteredContent: String?,
    val isFiltered: Boolean,
    val toxicityScore: Float?,
    val classification: ToxicityClass?,
    val horsemen: List<Horseman>,
    val reasoning: String?,
    val processingState: ProcessingState,
    val isSent: Boolean,
    val isRead: Boolean,
    val isArchived: Boolean
) {
    val displayContent: String
        get() = if (isFiltered && filteredContent != null) filteredContent else originalContent
}

enum class ProcessingState {
    PENDING,
    PROCESSING,
    FILTERED,
    SAFE,
    ERROR;

    companion object {
        fun fromString(value: String): ProcessingState {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

enum class ToxicityClass {
    SAFE,
    WARNING,
    HARMFUL,
    ABUSIVE;

    companion object {
        fun fromString(value: String?): ToxicityClass? {
            return value?.let { entries.find { e -> e.name.equals(it, ignoreCase = true) } }
        }
    }
}

enum class Horseman {
    CRITICISM,
    CONTEMPT,
    DEFENSIVENESS,
    STONEWALLING;

    companion object {
        fun fromString(value: String): Horseman? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }

        fun fromList(values: List<String>): List<Horseman> {
            return values.mapNotNull { fromString(it) }
        }
    }
}
