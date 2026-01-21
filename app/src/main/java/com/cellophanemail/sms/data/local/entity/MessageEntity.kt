package com.cellophanemail.sms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["thread_id"]),
        Index(value = ["address"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "thread_id")
    val threadId: String,

    val address: String,

    val timestamp: Long,

    @ColumnInfo(name = "is_incoming")
    val isIncoming: Boolean,

    @ColumnInfo(name = "original_content", typeAffinity = ColumnInfo.BLOB)
    val originalContent: ByteArray,

    @ColumnInfo(name = "filtered_content")
    val filteredContent: String?,

    @ColumnInfo(name = "is_filtered")
    val isFiltered: Boolean = false,

    @ColumnInfo(name = "toxicity_score")
    val toxicityScore: Float? = null,

    val classification: String? = null,

    @ColumnInfo(name = "horsemen_detected")
    val horsemenDetected: String? = null,

    @ColumnInfo(name = "analysis_reasoning")
    val analysisReasoning: String? = null,

    val category: String? = null,

    val severity: String? = null,

    @ColumnInfo(name = "has_logistics")
    val hasLogistics: Boolean = false,

    @ColumnInfo(name = "engine_version")
    val engineVersion: String? = null,

    @ColumnInfo(name = "analyzed_at")
    val analyzedAt: Long? = null,

    @ColumnInfo(name = "processing_state")
    val processingState: String = "PENDING",

    @ColumnInfo(name = "is_sent")
    val isSent: Boolean = true,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()
}
