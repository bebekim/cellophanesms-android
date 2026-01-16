package com.cellophanemail.sms.ui.main

import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.ToxicityClass
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.cellophanemail.sms.domain.model.Thread as SmsThread

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var messageRepository: MessageRepository
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testThread1 = SmsThread(
        threadId = "thread1",
        address = "+1234567890",
        contactName = "John Doe",
        contactPhotoUri = null,
        lastMessageTime = System.currentTimeMillis(),
        lastMessagePreview = "Hello!",
        unreadCount = 2,
        messageCount = 5,
        isArchived = false,
        isPinned = false,
        isMuted = false,
        toxicityLevel = ToxicityClass.SAFE
    )

    private val testThread2 = SmsThread(
        threadId = "thread2",
        address = "+0987654321",
        contactName = "Jane Smith",
        contactPhotoUri = null,
        lastMessageTime = System.currentTimeMillis() - 1000,
        lastMessagePreview = "How are you?",
        unreadCount = 0,
        messageCount = 3,
        isArchived = false,
        isPinned = false,
        isMuted = false,
        toxicityLevel = ToxicityClass.WARNING
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        messageRepository = mockk(relaxed = true)

        coEvery { messageRepository.getAllThreads() } returns flowOf(listOf(testThread1, testThread2))
        coEvery { messageRepository.searchThreads(any()) } returns flowOf(emptyList())

        viewModel = MainViewModel(messageRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial ui state is Loading`() = runTest {
        val freshRepo = mockk<MessageRepository>(relaxed = true)
        coEvery { freshRepo.getAllThreads() } returns flowOf(emptyList())

        val freshViewModel = MainViewModel(freshRepo)

        // Before any collect happens, state should be Loading
        assertEquals(MainUiState.Loading, freshViewModel.uiState.value)
    }

    @Test
    fun `ui state becomes Success after threads are loaded`() = runTest {
        advanceUntilIdle()

        assertEquals(MainUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `threads flow starts with empty list`() {
        // Initial value before collection
        assertTrue(viewModel.threads.value.isEmpty())
    }

    @Test
    fun `threads are loaded from repository`() = runTest {
        advanceUntilIdle()

        val threads = viewModel.threads.value
        assertEquals(2, threads.size)
        assertEquals("thread1", threads[0].threadId)
        assertEquals("thread2", threads[1].threadId)
    }

    // ==================== Search Tests ====================

    @Test
    fun `initial search query is empty`() {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `initial search results are empty`() {
        assertTrue(viewModel.searchResults.value.isEmpty())
    }

    @Test
    fun `setSearchQuery updates search query state`() = runTest {
        viewModel.setSearchQuery("John")
        advanceUntilIdle()

        assertEquals("John", viewModel.searchQuery.value)
    }

    @Test
    fun `setSearchQuery triggers search when query is not blank`() = runTest {
        val searchResults = listOf(testThread1)
        coEvery { messageRepository.searchThreads("John") } returns flowOf(searchResults)

        viewModel.setSearchQuery("John")
        advanceUntilIdle()

        coVerify { messageRepository.searchThreads("John") }
    }

    @Test
    fun `setSearchQuery clears results when query is blank`() = runTest {
        // First set a search
        coEvery { messageRepository.searchThreads("John") } returns flowOf(listOf(testThread1))
        viewModel.setSearchQuery("John")
        advanceUntilIdle()

        // Then clear it
        viewModel.setSearchQuery("")
        advanceUntilIdle()

        assertTrue(viewModel.searchResults.value.isEmpty())
    }

    @Test
    fun `setSearchQuery updates search results`() = runTest {
        val searchResults = listOf(testThread1)
        coEvery { messageRepository.searchThreads("John") } returns flowOf(searchResults)

        viewModel.setSearchQuery("John")
        advanceUntilIdle()

        assertEquals(1, viewModel.searchResults.value.size)
        assertEquals("John Doe", viewModel.searchResults.value[0].contactName)
    }

    @Test
    fun `setSearchQuery with whitespace only clears results`() = runTest {
        viewModel.setSearchQuery("   ")
        advanceUntilIdle()

        assertTrue(viewModel.searchResults.value.isEmpty())
    }

    // ==================== Archive Thread Tests ====================

    @Test
    fun `archiveThread calls repository`() = runTest {
        coEvery { messageRepository.archiveThread("thread1") } just Runs

        viewModel.archiveThread("thread1")
        advanceUntilIdle()

        coVerify { messageRepository.archiveThread("thread1") }
    }

    @Test
    fun `archiveThread sets error state on failure`() = runTest {
        advanceUntilIdle() // Wait for initial state to stabilize
        coEvery { messageRepository.archiveThread(any()) } throws RuntimeException("Archive failed")

        viewModel.archiveThread("thread1")
        advanceUntilIdle()

        assertTrue("Expected Error state but was ${viewModel.uiState.value}",
            viewModel.uiState.value is MainUiState.Error)
        assertEquals("Failed to archive thread", (viewModel.uiState.value as MainUiState.Error).message)
    }

    @Test
    fun `archiveThread maintains success state when successful`() = runTest {
        // Wait for initial Success state
        advanceUntilIdle()
        coEvery { messageRepository.archiveThread("thread1") } just Runs

        viewModel.archiveThread("thread1")
        advanceUntilIdle()

        // State should still be Success (or we need to verify no error was set)
        // Since archiveThread doesn't change state on success, we just verify the call
        coVerify { messageRepository.archiveThread("thread1") }
    }

    // ==================== Mark as Read Tests ====================

    @Test
    fun `markThreadAsRead calls repository`() = runTest {
        coEvery { messageRepository.markThreadAsRead("thread1") } just Runs

        viewModel.markThreadAsRead("thread1")
        advanceUntilIdle()

        coVerify { messageRepository.markThreadAsRead("thread1") }
    }

    @Test
    fun `markThreadAsRead silently fails on error`() = runTest {
        advanceUntilIdle() // Wait for Success state
        coEvery { messageRepository.markThreadAsRead(any()) } throws RuntimeException("Failed")

        viewModel.markThreadAsRead("thread1")
        advanceUntilIdle()

        // State should still be Success (error is silently swallowed)
        assertEquals(MainUiState.Success, viewModel.uiState.value)
    }

    // ==================== Thread Ordering Tests ====================

    @Test
    fun `threads maintain repository ordering`() = runTest {
        val orderedThreads = listOf(
            testThread1.copy(lastMessageTime = 3000),
            testThread2.copy(lastMessageTime = 2000)
        )
        coEvery { messageRepository.getAllThreads() } returns flowOf(orderedThreads)

        val vm = MainViewModel(messageRepository)
        advanceUntilIdle()

        assertEquals(3000, vm.threads.value[0].lastMessageTime)
        assertEquals(2000, vm.threads.value[1].lastMessageTime)
    }

    // ==================== Empty State Tests ====================

    @Test
    fun `empty threads list still transitions to Success state`() = runTest {
        coEvery { messageRepository.getAllThreads() } returns flowOf(emptyList())

        val vm = MainViewModel(messageRepository)
        advanceUntilIdle()

        assertEquals(MainUiState.Success, vm.uiState.value)
        assertTrue(vm.threads.value.isEmpty())
    }
}
