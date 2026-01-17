package com.cellophanemail.sms.data.repository

import com.cellophanemail.sms.data.contact.ContactResolver
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.ThreadEntity
import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.SmsAnalysisResponse
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.util.MessageEncryption
import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for MessageRepository.
 * Uses MockK to mock dependencies.
 */
class MessageRepositoryTest {

    @MockK
    private lateinit var messageDao: MessageDao

    @MockK
    private lateinit var threadDao: ThreadDao

    @MockK
    private lateinit var api: CellophoneMailApi

    @MockK
    private lateinit var encryption: MessageEncryption

    @MockK
    private lateinit var contactResolver: ContactResolver

    private lateinit var gson: Gson
    private lateinit var repository: MessageRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        gson = Gson()

        // Default encryption behavior - convert string to bytes and back
        every { encryption.encrypt(any<String>()) } answers {
            (firstArg<String>()).toByteArray(Charsets.UTF_8)
        }
        every { encryption.decrypt(any<ByteArray>()) } answers {
            String(firstArg<ByteArray>(), Charsets.UTF_8)
        }

        // Default contact resolver behavior - return null (no contact found)
        every { contactResolver.lookupContact(any()) } returns null

        repository = MessageRepository(messageDao, threadDao, api, encryption, gson, contactResolver)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Message Operations ====================

    @Test
    fun `insertMessage calls dao insert and updates thread`() = runTest {
        val message = createTestMessage()

        coEvery { messageDao.insert(any()) } just Runs
        coEvery { threadDao.getById(any()) } returns null
        coEvery { threadDao.insert(any()) } just Runs
        coEvery { messageDao.getUnreadCount(any()) } returns 1
        coEvery { messageDao.getMessageCount(any()) } returns 1

        repository.insertMessage(message)

        coVerify { messageDao.insert(any()) }
    }

    @Test
    fun `getMessageById returns domain model when found`() = runTest {
        val entity = createTestMessageEntity()
        coEvery { messageDao.getById("test-id") } returns entity

        val result = repository.getMessageById("test-id")

        assertNotNull(result)
        assertEquals("test-id", result?.id)
    }

    @Test
    fun `getMessageById returns null when not found`() = runTest {
        coEvery { messageDao.getById("non-existent") } returns null

        val result = repository.getMessageById("non-existent")

        assertNull(result)
    }

    @Test
    fun `getMessagesByThread returns flow of domain models`() = runTest {
        val entities = listOf(
            createTestMessageEntity(id = "msg1"),
            createTestMessageEntity(id = "msg2")
        )
        every { messageDao.getMessagesByThread("thread1") } returns flowOf(entities)

        val messages = repository.getMessagesByThread("thread1").first()

        assertEquals(2, messages.size)
        assertEquals("msg1", messages[0].id)
        assertEquals("msg2", messages[1].id)
    }

    @Test
    fun `getNextPendingMessage returns oldest pending message`() = runTest {
        val entity = createTestMessageEntity(processingState = "PENDING")
        coEvery { messageDao.getNextPendingMessage() } returns entity

        val result = repository.getNextPendingMessage()

        assertNotNull(result)
        assertEquals(ProcessingState.PENDING, result?.processingState)
    }

    @Test
    fun `markThreadAsRead updates messages and thread`() = runTest {
        coEvery { messageDao.markThreadAsRead("thread1") } just Runs
        coEvery { threadDao.updateUnreadCount("thread1", 0, any()) } just Runs

        repository.markThreadAsRead("thread1")

        coVerify { messageDao.markThreadAsRead("thread1") }
        coVerify { threadDao.updateUnreadCount("thread1", 0, any()) }
    }

    // ==================== Thread Operations ====================

    @Test
    fun `getAllThreads returns flow of domain models`() = runTest {
        val entities = listOf(
            createTestThreadEntity(threadId = "t1"),
            createTestThreadEntity(threadId = "t2")
        )
        every { threadDao.getAllThreads() } returns flowOf(entities)

        val threads = repository.getAllThreads().first()

        assertEquals(2, threads.size)
        assertEquals("t1", threads[0].threadId)
        assertEquals("t2", threads[1].threadId)
    }

    @Test
    fun `getThreadById returns domain model when found`() = runTest {
        val entity = createTestThreadEntity(threadId = "thread1")
        coEvery { threadDao.getById("thread1") } returns entity

        val result = repository.getThreadById("thread1")

        assertNotNull(result)
        assertEquals("thread1", result?.threadId)
    }

    @Test
    fun `getOrCreateThread returns existing thread`() = runTest {
        val existingEntity = createTestThreadEntity(threadId = "1234567890")
        coEvery { threadDao.getById("1234567890") } returns existingEntity

        val thread = repository.getOrCreateThread("5551234567890")

        assertEquals("1234567890", thread.threadId)
        coVerify(exactly = 0) { threadDao.insert(any()) }
    }

    @Test
    fun `getOrCreateThread creates new thread when not found`() = runTest {
        coEvery { threadDao.getById(any()) } returns null
        coEvery { threadDao.insert(any()) } just Runs

        val thread = repository.getOrCreateThread("5551234567")

        coVerify { threadDao.insert(any()) }
    }

    @Test
    fun `archiveThread calls dao archiveThread`() = runTest {
        coEvery { threadDao.archiveThread("thread1", any()) } just Runs

        repository.archiveThread("thread1")

        coVerify { threadDao.archiveThread("thread1", any()) }
    }

    @Test
    fun `searchThreads returns flow of matching threads`() = runTest {
        val entities = listOf(createTestThreadEntity(contactName = "John"))
        every { threadDao.searchThreads("John") } returns flowOf(entities)

        val results = repository.searchThreads("John").first()

        assertEquals(1, results.size)
        assertEquals("John", results[0].contactName)
    }

    // ==================== API Operations ====================

    @Test
    fun `analyzeMessage updates message with analysis result on success`() = runTest {
        val message = createTestMessage(processingState = ProcessingState.PENDING)
        val analysisResponse = SmsAnalysisResponse(
            classification = "WARNING",
            toxicityScore = 0.6f,
            filteredSummary = "Filtered content",
            horsemen = listOf("CRITICISM"),
            reasoning = "Contains criticism",
            specificExamples = listOf("example 1")
        )

        coEvery { api.analyzeSms(any()) } returns Response.success(analysisResponse)
        coEvery { messageDao.update(any()) } just Runs
        coEvery { threadDao.getById(any()) } returns createTestThreadEntity()
        coEvery { threadDao.update(any()) } just Runs
        coEvery { messageDao.getUnreadCount(any()) } returns 0
        coEvery { messageDao.getMessageCount(any()) } returns 1

        val result = repository.analyzeMessage(message, "device-123")

        assertTrue(result.isSuccess)
        val updatedMessage = result.getOrNull()
        assertEquals(ToxicityClass.WARNING, updatedMessage?.classification)
        assertEquals(0.6f, updatedMessage?.toxicityScore)
        assertEquals("Filtered content", updatedMessage?.filteredContent)
        assertTrue(updatedMessage?.isFiltered == true)
    }

    @Test
    fun `analyzeMessage returns safe state for safe classification`() = runTest {
        val message = createTestMessage()
        val analysisResponse = SmsAnalysisResponse(
            classification = "SAFE",
            toxicityScore = 0.1f,
            filteredSummary = "",
            horsemen = emptyList(),
            reasoning = "No issues detected",
            specificExamples = emptyList()
        )

        coEvery { api.analyzeSms(any()) } returns Response.success(analysisResponse)
        coEvery { messageDao.update(any()) } just Runs
        coEvery { threadDao.getById(any()) } returns createTestThreadEntity()
        coEvery { threadDao.update(any()) } just Runs
        coEvery { messageDao.getUnreadCount(any()) } returns 0
        coEvery { messageDao.getMessageCount(any()) } returns 1

        val result = repository.analyzeMessage(message, null)

        assertTrue(result.isSuccess)
        assertEquals(ProcessingState.SAFE, result.getOrNull()?.processingState)
        assertFalse(result.getOrNull()?.isFiltered == true)
    }

    @Test
    fun `analyzeMessage returns failure on API error response`() = runTest {
        val message = createTestMessage()

        coEvery { api.analyzeSms(any()) } returns Response.error(500, okhttp3.ResponseBody.create(null, ""))

        val result = repository.analyzeMessage(message, null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API error: 500") == true)
    }

    @Test
    fun `analyzeMessage returns failure and sets error state on exception`() = runTest {
        val message = createTestMessage()

        coEvery { api.analyzeSms(any()) } throws RuntimeException("Network error")
        coEvery { messageDao.update(any()) } just Runs
        coEvery { threadDao.getById(any()) } returns createTestThreadEntity()
        coEvery { threadDao.update(any()) } just Runs
        coEvery { messageDao.getUnreadCount(any()) } returns 0
        coEvery { messageDao.getMessageCount(any()) } returns 1

        val result = repository.analyzeMessage(message, null)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    // ==================== Helper Functions ====================

    @Test
    fun `generateThreadId returns last 10 digits of normalized number`() {
        val threadId = MessageRepository.generateThreadId("+1 (555) 123-4567")

        assertEquals("5551234567", threadId)
    }

    @Test
    fun `normalizePhoneNumber removes non-numeric characters except plus`() {
        val normalized = MessageRepository.normalizePhoneNumber("+1 (555) 123-4567")

        assertEquals("+15551234567", normalized)
    }

    @Test
    fun `normalizePhoneNumber handles already clean numbers`() {
        val normalized = MessageRepository.normalizePhoneNumber("5551234567")

        assertEquals("5551234567", normalized)
    }

    // ==================== Test Data Factories ====================

    private fun createTestMessage(
        id: String = "test-id",
        threadId: String = "test-thread",
        address: String = "5551234567",
        originalContent: String = "Test message",
        processingState: ProcessingState = ProcessingState.PENDING
    ) = Message(
        id = id,
        threadId = threadId,
        address = address,
        timestamp = System.currentTimeMillis(),
        isIncoming = true,
        originalContent = originalContent,
        filteredContent = null,
        isFiltered = false,
        toxicityScore = null,
        classification = null,
        horsemen = emptyList(),
        reasoning = null,
        processingState = processingState,
        isSent = true,
        isRead = false,
        isArchived = false
    )

    private fun createTestMessageEntity(
        id: String = "test-id",
        threadId: String = "test-thread",
        processingState: String = "PENDING"
    ) = MessageEntity(
        id = id,
        threadId = threadId,
        address = "5551234567",
        timestamp = System.currentTimeMillis(),
        isIncoming = true,
        originalContent = "Test message".toByteArray(),
        filteredContent = null,
        isFiltered = false,
        processingState = processingState
    )

    private fun createTestThreadEntity(
        threadId: String = "test-thread",
        address: String = "5551234567",
        contactName: String? = null
    ) = ThreadEntity(
        threadId = threadId,
        address = address,
        contactName = contactName,
        contactPhotoUri = null,
        lastMessageId = null,
        lastMessageTime = System.currentTimeMillis(),
        lastMessagePreview = "Test preview",
        unreadCount = 0,
        messageCount = 1
    )
}
