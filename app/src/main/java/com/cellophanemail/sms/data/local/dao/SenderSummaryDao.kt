package com.cellophanemail.sms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SenderSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<SenderSummaryEntity>)

    @Update
    suspend fun update(summary: SenderSummaryEntity)

    @Query("SELECT * FROM sender_summaries WHERE sender_id = :senderId")
    suspend fun getBySenderId(senderId: String): SenderSummaryEntity?

    @Query("SELECT * FROM sender_summaries WHERE sender_id = :senderId")
    fun observeBySenderId(senderId: String): Flow<SenderSummaryEntity?>

    @Query("SELECT * FROM sender_summaries ORDER BY filtered_count DESC")
    fun getAllByFilteredCount(): Flow<List<SenderSummaryEntity>>

    @Query("SELECT * FROM sender_summaries ORDER BY total_toxicity_score DESC")
    fun getAllByToxicity(): Flow<List<SenderSummaryEntity>>

    @Query("SELECT * FROM sender_summaries ORDER BY last_filtered_timestamp DESC")
    fun getAllByRecency(): Flow<List<SenderSummaryEntity>>

    @Query("SELECT * FROM sender_summaries WHERE filtered_count > 0 ORDER BY filtered_count DESC LIMIT :limit")
    fun getTopRiskSenders(limit: Int): Flow<List<SenderSummaryEntity>>

    @Query("SELECT * FROM sender_summaries WHERE filtered_count > 0 ORDER BY last_filtered_timestamp DESC LIMIT :limit")
    fun getRecentRiskSenders(limit: Int): Flow<List<SenderSummaryEntity>>

    @Query("""
        SELECT * FROM sender_summaries
        WHERE filtered_count > 0
        ORDER BY
            CASE WHEN last_filtered_timestamp > :recentThreshold THEN 1 ELSE 0 END DESC,
            filtered_count DESC,
            total_toxicity_score DESC
        LIMIT :limit
    """)
    fun getRiskSendersRanked(limit: Int, recentThreshold: Long): Flow<List<SenderSummaryEntity>>

    @Query("SELECT COUNT(*) FROM sender_summaries WHERE filtered_count > 0")
    suspend fun getToxicSenderCount(): Int

    @Query("SELECT COUNT(*) FROM sender_summaries WHERE filtered_count > 0")
    fun observeToxicSenderCount(): Flow<Int>

    @Query("SELECT SUM(filtered_count) FROM sender_summaries")
    suspend fun getTotalFilteredCount(): Int?

    @Query("SELECT SUM(filtered_count) FROM sender_summaries")
    fun observeTotalFilteredCount(): Flow<Int?>

    @Query("SELECT SUM(noise_count) FROM sender_summaries")
    suspend fun getTotalNoiseCount(): Int?

    @Query("SELECT SUM(toxic_logistics_count) FROM sender_summaries")
    suspend fun getTotalToxicLogisticsCount(): Int?

    @Query("""
        SELECT SUM(criticism_count) as criticism,
               SUM(contempt_count) as contempt,
               SUM(defensiveness_count) as defensiveness,
               SUM(stonewalling_count) as stonewalling
        FROM sender_summaries
    """)
    suspend fun getAggregatedHorsemenCounts(): HorsemenAggregation?

    @Query("DELETE FROM sender_summaries WHERE sender_id = :senderId")
    suspend fun delete(senderId: String)

    @Query("DELETE FROM sender_summaries")
    suspend fun deleteAll()

    @Query("""
        UPDATE sender_summaries
        SET contact_name = :contactName,
            contact_photo_uri = :photoUri,
            updated_at = :updatedAt
        WHERE sender_id = :senderId
    """)
    suspend fun updateContactInfo(
        senderId: String,
        contactName: String?,
        photoUri: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}

data class HorsemenAggregation(
    val criticism: Int?,
    val contempt: Int?,
    val defensiveness: Int?,
    val stonewalling: Int?
)
