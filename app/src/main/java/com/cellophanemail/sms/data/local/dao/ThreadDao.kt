package com.cellophanemail.sms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cellophanemail.sms.data.local.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: ThreadEntity)

    @Update
    suspend fun update(thread: ThreadEntity)

    @Query("SELECT * FROM threads WHERE thread_id = :threadId")
    suspend fun getById(threadId: String): ThreadEntity?

    @Query("SELECT * FROM threads WHERE address = :address")
    suspend fun getByAddress(address: String): ThreadEntity?

    @Query("""
        SELECT * FROM threads
        WHERE is_archived = 0
        ORDER BY is_pinned DESC, last_message_time DESC
    """)
    fun getAllThreads(): Flow<List<ThreadEntity>>

    @Query("""
        SELECT * FROM threads
        WHERE is_archived = 1
        ORDER BY last_message_time DESC
    """)
    fun getArchivedThreads(): Flow<List<ThreadEntity>>

    @Query("UPDATE threads SET is_archived = 1, updated_at = :timestamp WHERE thread_id = :threadId")
    suspend fun archiveThread(threadId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET is_archived = 0, updated_at = :timestamp WHERE thread_id = :threadId")
    suspend fun unarchiveThread(threadId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET is_pinned = :isPinned, updated_at = :timestamp WHERE thread_id = :threadId")
    suspend fun setPinned(threadId: String, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET is_muted = :isMuted, updated_at = :timestamp WHERE thread_id = :threadId")
    suspend fun setMuted(threadId: String, isMuted: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE threads SET
            unread_count = :unreadCount,
            updated_at = :timestamp
        WHERE thread_id = :threadId
    """)
    suspend fun updateUnreadCount(threadId: String, unreadCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM threads WHERE thread_id = :threadId")
    suspend fun delete(threadId: String)

    @Query("DELETE FROM threads")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM threads
        WHERE contact_name LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY last_message_time DESC
    """)
    fun searchThreads(query: String): Flow<List<ThreadEntity>>
}
