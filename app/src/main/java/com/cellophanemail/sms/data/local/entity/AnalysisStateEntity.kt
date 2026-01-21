package com.cellophanemail.sms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_state")
data class AnalysisStateEntity(
    @PrimaryKey
    val id: String = "default",

    @ColumnInfo(name = "initial_scan_completed")
    val initialScanCompleted: Boolean = false,

    @ColumnInfo(name = "initial_scan_started_at")
    val initialScanStartedAt: Long? = null,

    @ColumnInfo(name = "initial_scan_completed_at")
    val initialScanCompletedAt: Long? = null,

    @ColumnInfo(name = "total_messages_to_scan")
    val totalMessagesToScan: Int = 0,

    @ColumnInfo(name = "messages_scanned")
    val messagesScanned: Int = 0,

    @ColumnInfo(name = "last_incremental_analysis_at")
    val lastIncrementalAnalysisAt: Long? = null,

    @ColumnInfo(name = "last_analyzed_message_timestamp")
    val lastAnalyzedMessageTimestamp: Long? = null,

    @ColumnInfo(name = "current_job_id")
    val currentJobId: String? = null,

    @ColumnInfo(name = "engine_version")
    val engineVersion: String = "v1",

    @ColumnInfo(name = "dashboard_toxic_senders")
    val dashboardToxicSenders: Int = 0,

    @ColumnInfo(name = "dashboard_filtered_messages")
    val dashboardFilteredMessages: Int = 0,

    @ColumnInfo(name = "dashboard_noise_messages")
    val dashboardNoiseMessages: Int = 0,

    @ColumnInfo(name = "dashboard_toxic_logistics_messages")
    val dashboardToxicLogisticsMessages: Int = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val scanProgress: Float
        get() = if (totalMessagesToScan > 0) {
            messagesScanned.toFloat() / totalMessagesToScan
        } else 0f

    val isScanInProgress: Boolean
        get() = initialScanStartedAt != null && !initialScanCompleted
}
