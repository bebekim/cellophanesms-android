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
        Index(value = ["sender_id_normalized"]),
        Index(value = ["timestamp"]),
        Index(value = ["analyzed_at"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "thread_id")
    val threadId: String,

    val address: String,

    /**
     * Normalized sender ID for consistent aggregation.
     * Format: E.164 for international, or last 10 digits for AU numbers.
     * Example: +61412345678 -> "0412345678" or "61412345678"
     */
    @ColumnInfo(name = "sender_id_normalized")
    val senderIdNormalized: String,

    /**
     * Message direction: INBOUND or OUTBOUND
     */
    val direction: String = "INBOUND",

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

    /**
     * JSON array of detected horsemen names: ["CRITICISM", "CONTEMPT"]
     */
    @ColumnInfo(name = "horsemen_detected")
    val horsemenDetected: String? = null,

    /**
     * JSON object of horsemen confidence scores: {"CRITICISM": 0.85, "CONTEMPT": 0.72}
     */
    @ColumnInfo(name = "horsemen_confidences")
    val horsemenConfidences: String? = null,

    @ColumnInfo(name = "analysis_reasoning")
    val analysisReasoning: String? = null,

    /**
     * Message category after analysis:
     * - SAFE_LOGISTICS: Important, safe message
     * - SAFE_NOISE: Not important, safe
     * - TOXIC_LOGISTICS: Important but harmful (MUST SEE with armor)
     * - TOXIC_NOISE: Harmful, no info (FILTER OUT)
     * - null: Not yet analyzed
     */
    val category: String? = null,

    /**
     * Severity level: LOW, MEDIUM, HIGH, CRITICAL
     */
    val severity: String? = null,

    /**
     * Whether message contains logistics/actionable information
     */
    @ColumnInfo(name = "has_logistics")
    val hasLogistics: Boolean = false,

    @ColumnInfo(name = "engine_version")
    val engineVersion: String? = null,

    @ColumnInfo(name = "analyzed_at")
    val analyzedAt: Long? = null,

    /**
     * Retry count for failed analysis attempts
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    /**
     * Last error message if analysis failed
     */
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

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
