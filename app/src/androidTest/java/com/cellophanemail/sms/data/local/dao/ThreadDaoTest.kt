package com.cellophanemail.sms.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cellophanemail.sms.data.local.db.AppDatabase
import com.cellophanemail.sms.data.local.entity.ThreadEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ThreadDao.
 * Uses Room in-memory database for isolation.
 */
@RunWith(AndroidJUnit4::class)
class ThreadDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var threadDao: ThreadDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        threadDao = database.threadDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==================== Insert Tests ====================

    @Test
    fun insert_singleThread_canBeRetrievedById() = runTest {
        val thread = createTestThread()

        threadDao.insert(thread)
        val retrieved = threadDao.getById(thread.threadId)

        assertNotNull(retrieved)
        assertEquals(thread.threadId, retrieved?.threadId)
        assertEquals(thread.address, retrieved?.address)
    }

    @Test
    fun insert_withConflict_replacesExisting() = runTest {
        val original = createTestThread(threadId = "thread1", contactName = "Original")
        val updated = createTestThread(threadId = "thread1", contactName = "Updated")

        threadDao.insert(original)
        threadDao.insert(updated)
        val retrieved = threadDao.getById("thread1")

        assertEquals("Updated", retrieved?.contactName)
    }

    // ==================== Query Tests ====================

    @Test
    fun getById_nonExistent_returnsNull() = runTest {
        val retrieved = threadDao.getById("non-existent")

        assertNull(retrieved)
    }

    @Test
    fun getByAddress_returnsMatchingThread() = runTest {
        threadDao.insert(createTestThread(threadId = "thread1", address = "5551234567"))
        threadDao.insert(createTestThread(threadId = "thread2", address = "5559876543"))

        val retrieved = threadDao.getByAddress("5551234567")

        assertNotNull(retrieved)
        assertEquals("thread1", retrieved?.threadId)
    }

    @Test
    fun getAllThreads_excludesArchived() = runTest {
        threadDao.insert(createTestThread(threadId = "active1", isArchived = false))
        threadDao.insert(createTestThread(threadId = "active2", isArchived = false))
        threadDao.insert(createTestThread(threadId = "archived", isArchived = true))

        val threads = threadDao.getAllThreads().first()

        assertEquals(2, threads.size)
        assertTrue(threads.none { it.isArchived })
    }

    @Test
    fun getAllThreads_pinnedFirst_thenByTime() = runTest {
        threadDao.insert(createTestThread(threadId = "old", isPinned = false, lastMessageTime = 1000L))
        threadDao.insert(createTestThread(threadId = "new", isPinned = false, lastMessageTime = 3000L))
        threadDao.insert(createTestThread(threadId = "pinned", isPinned = true, lastMessageTime = 2000L))

        val threads = threadDao.getAllThreads().first()

        assertEquals("pinned", threads[0].threadId)
        assertEquals("new", threads[1].threadId)
        assertEquals("old", threads[2].threadId)
    }

    @Test
    fun getArchivedThreads_returnsOnlyArchived() = runTest {
        threadDao.insert(createTestThread(threadId = "active", isArchived = false))
        threadDao.insert(createTestThread(threadId = "archived1", isArchived = true))
        threadDao.insert(createTestThread(threadId = "archived2", isArchived = true))

        val archived = threadDao.getArchivedThreads().first()

        assertEquals(2, archived.size)
        assertTrue(archived.all { it.isArchived })
    }

    @Test
    fun getArchivedThreads_orderedByTimeDescending() = runTest {
        threadDao.insert(createTestThread(threadId = "old", isArchived = true, lastMessageTime = 1000L))
        threadDao.insert(createTestThread(threadId = "new", isArchived = true, lastMessageTime = 3000L))

        val archived = threadDao.getArchivedThreads().first()

        assertEquals("new", archived[0].threadId)
        assertEquals("old", archived[1].threadId)
    }

    // ==================== Update Tests ====================

    @Test
    fun update_modifiesExistingThread() = runTest {
        val thread = createTestThread(contactName = "Original")
        threadDao.insert(thread)

        val updated = thread.copy(contactName = "Updated")
        threadDao.update(updated)

        val retrieved = threadDao.getById(thread.threadId)
        assertEquals("Updated", retrieved?.contactName)
    }

    @Test
    fun archiveThread_setsArchivedFlag() = runTest {
        val thread = createTestThread(isArchived = false)
        threadDao.insert(thread)

        threadDao.archiveThread(thread.threadId)

        val retrieved = threadDao.getById(thread.threadId)
        assertTrue(retrieved?.isArchived == true)
    }

    @Test
    fun archiveThread_removesFromAllThreads() = runTest {
        val thread = createTestThread(isArchived = false)
        threadDao.insert(thread)

        threadDao.archiveThread(thread.threadId)

        val allThreads = threadDao.getAllThreads().first()
        assertTrue(allThreads.isEmpty())
    }

    @Test
    fun unarchiveThread_clearsArchivedFlag() = runTest {
        val thread = createTestThread(isArchived = true)
        threadDao.insert(thread)

        threadDao.unarchiveThread(thread.threadId)

        val retrieved = threadDao.getById(thread.threadId)
        assertFalse(retrieved?.isArchived == true)
    }

    @Test
    fun setPinned_true_pinsThread() = runTest {
        val thread = createTestThread(isPinned = false)
        threadDao.insert(thread)

        threadDao.setPinned(thread.threadId, true)

        val retrieved = threadDao.getById(thread.threadId)
        assertTrue(retrieved?.isPinned == true)
    }

    @Test
    fun setPinned_false_unpinsThread() = runTest {
        val thread = createTestThread(isPinned = true)
        threadDao.insert(thread)

        threadDao.setPinned(thread.threadId, false)

        val retrieved = threadDao.getById(thread.threadId)
        assertFalse(retrieved?.isPinned == true)
    }

    @Test
    fun setMuted_true_mutesThread() = runTest {
        val thread = createTestThread(isMuted = false)
        threadDao.insert(thread)

        threadDao.setMuted(thread.threadId, true)

        val retrieved = threadDao.getById(thread.threadId)
        assertTrue(retrieved?.isMuted == true)
    }

    @Test
    fun updateUnreadCount_updatesCount() = runTest {
        val thread = createTestThread(unreadCount = 0)
        threadDao.insert(thread)

        threadDao.updateUnreadCount(thread.threadId, 5)

        val retrieved = threadDao.getById(thread.threadId)
        assertEquals(5, retrieved?.unreadCount)
    }

    // ==================== Delete Tests ====================

    @Test
    fun delete_removesThread() = runTest {
        val thread = createTestThread()
        threadDao.insert(thread)

        threadDao.delete(thread.threadId)

        val retrieved = threadDao.getById(thread.threadId)
        assertNull(retrieved)
    }

    @Test
    fun delete_onlyRemovesTargetThread() = runTest {
        threadDao.insert(createTestThread(threadId = "thread1"))
        threadDao.insert(createTestThread(threadId = "thread2"))

        threadDao.delete("thread1")

        val threads = threadDao.getAllThreads().first()
        assertEquals(1, threads.size)
        assertEquals("thread2", threads[0].threadId)
    }

    // ==================== Search Tests ====================

    @Test
    fun searchThreads_matchesByContactName() = runTest {
        threadDao.insert(createTestThread(threadId = "t1", contactName = "John Smith"))
        threadDao.insert(createTestThread(threadId = "t2", contactName = "Jane Doe"))
        threadDao.insert(createTestThread(threadId = "t3", contactName = "Bob Johnson"))

        val results = threadDao.searchThreads("John").first()

        assertEquals(2, results.size) // John Smith and Bob Johnson
    }

    @Test
    fun searchThreads_matchesByAddress() = runTest {
        threadDao.insert(createTestThread(threadId = "t1", address = "5551234567"))
        threadDao.insert(createTestThread(threadId = "t2", address = "5559876543"))

        val results = threadDao.searchThreads("1234").first()

        assertEquals(1, results.size)
        assertEquals("t1", results[0].threadId)
    }

    @Test
    fun searchThreads_caseInsensitive() = runTest {
        threadDao.insert(createTestThread(threadId = "t1", contactName = "John Smith"))

        val lowercase = threadDao.searchThreads("john").first()
        val uppercase = threadDao.searchThreads("JOHN").first()
        val mixed = threadDao.searchThreads("JoHn").first()

        assertEquals(1, lowercase.size)
        assertEquals(1, uppercase.size)
        assertEquals(1, mixed.size)
    }

    @Test
    fun searchThreads_emptyQuery_returnsAll() = runTest {
        threadDao.insert(createTestThread(threadId = "t1"))
        threadDao.insert(createTestThread(threadId = "t2"))

        val results = threadDao.searchThreads("").first()

        assertEquals(2, results.size)
    }

    @Test
    fun searchThreads_orderedByTimeDescending() = runTest {
        threadDao.insert(createTestThread(threadId = "old", contactName = "Test", lastMessageTime = 1000L))
        threadDao.insert(createTestThread(threadId = "new", contactName = "Test", lastMessageTime = 3000L))

        val results = threadDao.searchThreads("Test").first()

        assertEquals("new", results[0].threadId)
        assertEquals("old", results[1].threadId)
    }

    // ==================== Helper Functions ====================

    private fun createTestThread(
        threadId: String = "test-thread",
        address: String = "5551234567",
        contactName: String? = null,
        contactPhotoUri: String? = null,
        lastMessageId: String? = null,
        lastMessageTime: Long = System.currentTimeMillis(),
        lastMessagePreview: String = "Test message preview",
        unreadCount: Int = 0,
        messageCount: Int = 1,
        isArchived: Boolean = false,
        isPinned: Boolean = false,
        isMuted: Boolean = false,
        toxicityLevel: String = "SAFE"
    ) = ThreadEntity(
        threadId = threadId,
        address = address,
        contactName = contactName,
        contactPhotoUri = contactPhotoUri,
        lastMessageId = lastMessageId,
        lastMessageTime = lastMessageTime,
        lastMessagePreview = lastMessagePreview,
        unreadCount = unreadCount,
        messageCount = messageCount,
        isArchived = isArchived,
        isPinned = isPinned,
        isMuted = isMuted,
        toxicityLevel = toxicityLevel
    )
}
