package com.cellophanemail.sms.domain.model

data class SenderSummary(
    val senderId: String,
    val contactName: String?,
    val contactPhotoUri: String?,
    val totalMessageCount: Int,
    val filteredCount: Int,
    val noiseCount: Int,
    val toxicLogisticsCount: Int,
    val horsemenCounts: HorsemenCounts,
    val lastMessageTimestamp: Long?,
    val lastFilteredTimestamp: Long?
) {
    val displayName: String
        get() = contactName ?: senderId

    val riskLevel: RiskLevel
        get() = when {
            filteredCount >= 10 || toxicLogisticsCount >= 5 -> RiskLevel.HIGH
            filteredCount >= 5 || toxicLogisticsCount >= 2 -> RiskLevel.MEDIUM
            filteredCount > 0 -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }

    val dominantHorseman: Horseman?
        get() {
            val max = maxOf(
                horsemenCounts.criticism,
                horsemenCounts.contempt,
                horsemenCounts.defensiveness,
                horsemenCounts.stonewalling
            )
            if (max == 0) return null
            return when (max) {
                horsemenCounts.contempt -> Horseman.CONTEMPT
                horsemenCounts.criticism -> Horseman.CRITICISM
                horsemenCounts.defensiveness -> Horseman.DEFENSIVENESS
                horsemenCounts.stonewalling -> Horseman.STONEWALLING
                else -> null
            }
        }

    val totalHorsemenCount: Int
        get() = horsemenCounts.total
}

data class HorsemenCounts(
    val criticism: Int = 0,
    val contempt: Int = 0,
    val defensiveness: Int = 0,
    val stonewalling: Int = 0
) {
    val total: Int
        get() = criticism + contempt + defensiveness + stonewalling
}

enum class RiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}
