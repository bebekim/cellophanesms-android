package com.cellophanemail.sms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cellophanemail.sms.data.local.entity.AnalysisStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: AnalysisStateEntity)

    @Update
    suspend fun update(state: AnalysisStateEntity)

    @Query("SELECT * FROM analysis_state WHERE id = :id")
    suspend fun getById(id: String = "default"): AnalysisStateEntity?

    @Query("SELECT * FROM analysis_state WHERE id = :id")
    fun observeById(id: String = "default"): Flow<AnalysisStateEntity?>

    @Query("SELECT initial_scan_completed FROM analysis_state WHERE id = 'default'")
    suspend fun isInitialScanCompleted(): Boolean?

    @Query("SELECT initial_scan_completed FROM analysis_state WHERE id = 'default'")
    fun observeInitialScanCompleted(): Flow<Boolean?>

    @Query("""
        UPDATE analysis_state
        SET initial_scan_started_at = :startedAt,
            total_messages_to_scan = :totalMessages,
            messages_scanned = 0,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun startInitialScan(
        startedAt: Long = System.currentTimeMillis(),
        totalMessages: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE analysis_state
        SET messages_scanned = :scanned,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun updateScanProgress(
        scanned: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE analysis_state
        SET initial_scan_completed = 1,
            initial_scan_completed_at = :completedAt,
            messages_scanned = total_messages_to_scan,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun completeInitialScan(
        completedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE analysis_state
        SET last_incremental_analysis_at = :timestamp,
            last_analyzed_message_timestamp = :lastMessageTimestamp,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun updateIncrementalAnalysis(
        timestamp: Long = System.currentTimeMillis(),
        lastMessageTimestamp: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE analysis_state
        SET current_job_id = :jobId,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun setCurrentJobId(
        jobId: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE analysis_state
        SET dashboard_toxic_senders = :toxicSenders,
            dashboard_filtered_messages = :filteredMessages,
            dashboard_noise_messages = :noiseMessages,
            dashboard_toxic_logistics_messages = :toxicLogisticsMessages,
            updated_at = :updatedAt
        WHERE id = 'default'
    """)
    suspend fun updateDashboardRollup(
        toxicSenders: Int,
        filteredMessages: Int,
        noiseMessages: Int,
        toxicLogisticsMessages: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT last_analyzed_message_timestamp FROM analysis_state WHERE id = 'default'")
    suspend fun getLastAnalyzedMessageTimestamp(): Long?

    @Query("DELETE FROM analysis_state WHERE id = :id")
    suspend fun delete(id: String = "default")
}
