package com.cellophanemail.sms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sender_summaries",
    indices = [
        Index(value = ["filtered_count"]),
        Index(value = ["last_filtered_timestamp"]),
        Index(value = ["total_toxicity_score"])
    ]
)
data class SenderSummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "contact_name")
    val contactName: String? = null,

    @ColumnInfo(name = "contact_photo_uri")
    val contactPhotoUri: String? = null,

    @ColumnInfo(name = "total_message_count")
    val totalMessageCount: Int = 0,

    @ColumnInfo(name = "filtered_count")
    val filteredCount: Int = 0,

    @ColumnInfo(name = "noise_count")
    val noiseCount: Int = 0,

    @ColumnInfo(name = "toxic_logistics_count")
    val toxicLogisticsCount: Int = 0,

    @ColumnInfo(name = "criticism_count")
    val criticismCount: Int = 0,

    @ColumnInfo(name = "contempt_count")
    val contemptCount: Int = 0,

    @ColumnInfo(name = "defensiveness_count")
    val defensivenessCount: Int = 0,

    @ColumnInfo(name = "stonewalling_count")
    val stonewallingCount: Int = 0,

    @ColumnInfo(name = "total_toxicity_score")
    val totalToxicityScore: Float = 0f,

    @ColumnInfo(name = "last_message_timestamp")
    val lastMessageTimestamp: Long? = null,

    @ColumnInfo(name = "last_filtered_timestamp")
    val lastFilteredTimestamp: Long? = null,

    @ColumnInfo(name = "engine_version")
    val engineVersion: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val averageToxicityScore: Float
        get() = if (totalMessageCount > 0) totalToxicityScore / totalMessageCount else 0f

    val totalHorsemenCount: Int
        get() = criticismCount + contemptCount + defensivenessCount + stonewallingCount

    val dominantHorseman: String?
        get() {
            val counts = mapOf(
                "CRITICISM" to criticismCount,
                "CONTEMPT" to contemptCount,
                "DEFENSIVENESS" to defensivenessCount,
                "STONEWALLING" to stonewallingCount
            )
            return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
        }
}
