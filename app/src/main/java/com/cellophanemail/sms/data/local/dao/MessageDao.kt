package com.cellophanemail.sms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cellophanemail.sms.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getMessagesByThread(threadId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE thread_id = :threadId AND is_deleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(threadId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE processing_state = :state ORDER BY timestamp ASC")
    suspend fun getMessagesByState(state: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE processing_state = 'PENDING' ORDER BY timestamp ASC LIMIT 1")
    suspend fun getNextPendingMessage(): MessageEntity?

    @Query("UPDATE messages SET is_read = 1 WHERE thread_id = :threadId")
    suspend fun markThreadAsRead(threadId: String)

    @Query("UPDATE messages SET is_deleted = 1, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND is_read = 0 AND is_incoming = 1")
    suspend fun getUnreadCount(threadId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE thread_id = :threadId AND is_deleted = 0")
    suspend fun getMessageCount(threadId: String): Int
}
