package com.cellophanemail.sms.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cellophanemail.sms.data.local.db.AppDatabase
import com.cellophanemail.sms.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumented tests for MessageDao.
 * Uses Room in-memory database for isolation.
 */
@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Insert Tests ====================

    @Test
    fun insert_singleMessage_canBeRetrievedById() = runTest {
        val message = createTestMessage()

        messageDao.insert(message)
        val retrieved = messageDao.getById(message.id)

        assertNotNull(retrieved)
        assertEquals(message.id, retrieved?.id)
        assertEquals(message.address, retrieved?.address)
    }

    @Test
    fun insertAll_multipleMessages_allCanBeRetrieved() = runTest {
        val messages = listOf(
            createTestMessage(threadId = "thread1"),
            createTestMessage(threadId = "thread1"),
            createTestMessage(threadId = "thread1")
        )

        messageDao.insertAll(messages)
        val retrieved = messageDao.getMessagesByThread("thread1").first()

        assertEquals(3, retrieved.size)
    }

    @Test
    fun insert_withConflict_replacesExisting() = runTest {
        val id = UUID.randomUUID().toString()
        val original = createTestMessage(id = id, filteredContent = "original")
        val updated = createTestMessage(id = id, filteredContent = "updated")

        messageDao.insert(original)
        messageDao.insert(updated)
        val retrieved = messageDao.getById(id)

        assertEquals("updated", retrieved?.filteredContent)
    }

    // ==================== Query Tests ====================

    @Test
    fun getById_nonExistent_returnsNull() = runTest {
        val retrieved = messageDao.getById("non-existent-id")

        assertNull(retrieved)
    }

    @Test
    fun getMessagesByThread_returnsOnlyMatchingThread() = runTest {
        messageDao.insert(createTestMessage(threadId = "thread1"))
        messageDao.insert(createTestMessage(threadId = "thread1"))
        messageDao.insert(createTestMessage(threadId = "thread2"))

        val thread1Messages = messageDao.getMessagesByThread("thread1").first()
        val thread2Messages = messageDao.getMessagesByThread("thread2").first()

        assertEquals(2, thread1Messages.size)
        assertEquals(1, thread2Messages.size)
    }

    @Test
    fun getMessagesByThread_excludesDeletedMessages() = runTest {
        val deletedMessage = createTestMessage(threadId = "thread1", isDeleted = true)
        val activeMessage = createTestMessage(threadId = "thread1", isDeleted = false)

        messageDao.insert(deletedMessage)
        messageDao.insert(activeMessage)

        val messages = messageDao.getMessagesByThread("thread1").first()

        assertEquals(1, messages.size)
        assertFalse(messages[0].isDeleted)
    }

    @Test
    fun getMessagesByThread_orderedByTimestampDescending() = runTest {
        val older = createTestMessage(threadId = "thread1", timestamp = 1000L)
        val newer = createTestMessage(threadId = "thread1", timestamp = 2000L)
        val newest = createTestMessage(threadId = "thread1", timestamp = 3000L)

        messageDao.insert(older)
        messageDao.insert(newest)
        messageDao.insert(newer)

        val messages = messageDao.getMessagesByThread("thread1").first()

        assertEquals(3000L, messages[0].timestamp)
        assertEquals(2000L, messages[1].timestamp)
        assertEquals(1000L, messages[2].timestamp)
    }

    @Test
    fun getRecentMessages_respectsLimit() = runTest {
        repeat(10) { i ->
            messageDao.insert(createTestMessage(threadId = "thread1", timestamp = i.toLong()))
        }

        val recent = messageDao.getRecentMessages("thread1", limit = 3)

        assertEquals(3, recent.size)
    }

    @Test
    fun getMessagesByState_returnsMatchingState() = runTest {
        messageDao.insert(createTestMessage(processingState = "PENDING"))
        messageDao.insert(createTestMessage(processingState = "PENDING"))
        messageDao.insert(createTestMessage(processingState = "PROCESSED"))

        val pending = messageDao.getMessagesByState("PENDING")

        assertEquals(2, pending.size)
        assertTrue(pending.all { it.processingState == "PENDING" })
    }

    @Test
    fun getNextPendingMessage_returnsOldestPending() = runTest {
        val older = createTestMessage(processingState = "PENDING", timestamp = 1000L)
        val newer = createTestMessage(processingState = "PENDING", timestamp = 2000L)

        messageDao.insert(newer)
        messageDao.insert(older)

        val next = messageDao.getNextPendingMessage()

        assertNotNull(next)
        assertEquals(1000L, next?.timestamp)
    }

    @Test
    fun getNextPendingMessage_skipsNonPending() = runTest {
        messageDao.insert(createTestMessage(processingState = "PROCESSED", timestamp = 1000L))
        messageDao.insert(createTestMessage(processingState = "PENDING", timestamp = 2000L))

        val next = messageDao.getNextPendingMessage()

        assertEquals("PENDING", next?.processingState)
        assertEquals(2000L, next?.timestamp)
    }

    // ==================== Update Tests ====================

    @Test
    fun update_modifiesExistingMessage() = runTest {
        val message = createTestMessage(isRead = false)
        messageDao.insert(message)

        val updated = message.copy(isRead = true)
        messageDao.update(updated)

        val retrieved = messageDao.getById(message.id)
        assertTrue(retrieved?.isRead == true)
    }

    @Test
    fun markThreadAsRead_updatesAllMessagesInThread() = runTest {
        val thread1Msg1 = createTestMessage(threadId = "thread1", isRead = false)
        val thread1Msg2 = createTestMessage(threadId = "thread1", isRead = false)
        val thread2Msg = createTestMessage(threadId = "thread2", isRead = false)

        messageDao.insert(thread1Msg1)
        messageDao.insert(thread1Msg2)
        messageDao.insert(thread2Msg)

        messageDao.markThreadAsRead("thread1")

        val thread1Messages = messageDao.getMessagesByThread("thread1").first()
        val thread2Messages = messageDao.getMessagesByThread("thread2").first()

        assertTrue(thread1Messages.all { it.isRead })
        assertFalse(thread2Messages[0].isRead)
    }

    // ==================== Delete Tests ====================

    @Test
    fun softDelete_setsIsDeletedFlag() = runTest {
        val message = createTestMessage()
        messageDao.insert(message)

        messageDao.softDelete(message.id)

        val retrieved = messageDao.getById(message.id)
        assertTrue(retrieved?.isDeleted == true)
    }

    @Test
    fun softDelete_excludesFromThreadQuery() = runTest {
        val message = createTestMessage(threadId = "thread1")
        messageDao.insert(message)

        messageDao.softDelete(message.id)

        val messages = messageDao.getMessagesByThread("thread1").first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun hardDelete_removesCompletely() = runTest {
        val message = createTestMessage()
        messageDao.insert(message)

        messageDao.hardDelete(message.id)

        val retrieved = messageDao.getById(message.id)
        assertNull(retrieved)
    }

    // ==================== Count Tests ====================

    @Test
    fun getUnreadCount_countsOnlyUnreadIncoming() = runTest {
        // Unread incoming - should count
        messageDao.insert(createTestMessage(threadId = "thread1", isRead = false, isIncoming = true))
        messageDao.insert(createTestMessage(threadId = "thread1", isRead = false, isIncoming = true))
        // Read incoming - should not count
        messageDao.insert(createTestMessage(threadId = "thread1", isRead = true, isIncoming = true))
        // Unread outgoing - should not count
        messageDao.insert(createTestMessage(threadId = "thread1", isRead = false, isIncoming = false))

        val count = messageDao.getUnreadCount("thread1")

        assertEquals(2, count)
    }

    @Test
    fun getMessageCount_countsNonDeletedOnly() = runTest {
        messageDao.insert(createTestMessage(threadId = "thread1", isDeleted = false))
        messageDao.insert(createTestMessage(threadId = "thread1", isDeleted = false))
        messageDao.insert(createTestMessage(threadId = "thread1", isDeleted = true))

        val count = messageDao.getMessageCount("thread1")

        assertEquals(2, count)
    }

    // ==================== Helper Functions ====================

    private fun createTestMessage(
        id: String = UUID.randomUUID().toString(),
        threadId: String = "test-thread",
        address: String = "5551234567",
        timestamp: Long = System.currentTimeMillis(),
        isIncoming: Boolean = true,
        originalContent: ByteArray = "Test message".toByteArray(),
        filteredContent: String? = null,
        isFiltered: Boolean = false,
        processingState: String = "PENDING",
        isRead: Boolean = false,
        isDeleted: Boolean = false
    ) = MessageEntity(
        id = id,
        threadId = threadId,
        address = address,
        timestamp = timestamp,
        isIncoming = isIncoming,
        originalContent = originalContent,
        filteredContent = filteredContent,
        isFiltered = isFiltered,
        processingState = processingState,
        isRead = isRead,
        isDeleted = isDeleted
    )
}
