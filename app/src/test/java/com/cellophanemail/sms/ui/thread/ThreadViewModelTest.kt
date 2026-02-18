package com.cellophanemail.sms.ui.thread

import androidx.lifecycle.SavedStateHandle
import com.cellophanemail.sms.data.local.TextRenderingPreferences
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.annotation.AnnotationPipeline
import com.cellophanemail.sms.domain.annotation.RegexEntitySource
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.Horseman
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.util.NotificationHelper
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModelTest {

    private lateinit var messageRepository: MessageRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var contactResolver: com.cellophanemail.sms.data.contact.ContactResolver
    private lateinit var annotationPipeline: AnnotationPipeline
    private lateinit var textRenderingPreferences: TextRenderingPreferences
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ThreadViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testMessage1 = Message(
        id = "msg1",
        threadId = "thread1",
        address = "+1234567890",
        timestamp = System.currentTimeMillis(),
        isIncoming = true,
        originalContent = "Hello there!",
        filteredContent = null,
        isFiltered = false,
        toxicityScore = null,
        classification = null,
        horsemen = emptyList(),
        reasoning = null,
        processingState = ProcessingState.SAFE,
        isSent = true,
        isRead = false,
        isArchived = false
    )

    private val testMessage2 = Message(
        id = "msg2",
        threadId = "thread1",
        address = "+1234567890",
        timestamp = System.currentTimeMillis() - 1000,
        isIncoming = true,
        originalContent = "You are terrible!",
        filteredContent = "The sender expressed frustration.",
        isFiltered = true,
        toxicityScore = 0.7f,
        classification = ToxicityClass.WARNING,
        horsemen = listOf(Horseman.CRITICISM),
        reasoning = "Contains critical language",
        processingState = ProcessingState.FILTERED,
        isSent = true,
        isRead = false,
        isArchived = false
    )

    private val testMessage3 = Message(
        id = "msg3",
        threadId = "thread1",
        address = "+1234567890",
        timestamp = System.currentTimeMillis() - 2000,
        isIncoming = false,
        originalContent = "Hi! How are you?",
        filteredContent = null,
        isFiltered = false,
        toxicityScore = null,
        classification = null,
        horsemen = emptyList(),
        reasoning = null,
        processingState = ProcessingState.SAFE,
        isSent = true,
        isRead = true,
        isArchived = false
    )

    private val testMessageWithEntities = Message(
        id = "msg4",
        threadId = "thread1",
        address = "+1234567890",
        timestamp = System.currentTimeMillis() - 3000,
        isIncoming = true,
        originalContent = "Visit https://example.com or call 555-123-4567 tomorrow",
        filteredContent = null,
        isFiltered = false,
        toxicityScore = null,
        classification = null,
        horsemen = emptyList(),
        reasoning = null,
        processingState = ProcessingState.SAFE,
        isSent = true,
        isRead = false,
        isArchived = false
    )

    private fun createViewModel(
        messages: List<Message> = listOf(testMessage1, testMessage2, testMessage3),
        handle: SavedStateHandle = savedStateHandle
    ): ThreadViewModel {
        coEvery { messageRepository.getMessagesByThread("thread1") } returns flowOf(messages)
        return ThreadViewModel(
            messageRepository,
            notificationHelper,
            contactResolver,
            annotationPipeline,
            textRenderingPreferences,
            handle
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        messageRepository = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        contactResolver = mockk(relaxed = true)
        annotationPipeline = AnnotationPipeline(listOf(RegexEntitySource()))
        textRenderingPreferences = mockk(relaxed = true)

        every { textRenderingPreferences.illuminatedEnabled } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        every { textRenderingPreferences.entityHighlightsEnabled } returns kotlinx.coroutines.flow.MutableStateFlow(true)
        every { textRenderingPreferences.selectedStylePack } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.cellophanemail.sms.domain.model.IlluminatedStylePack.CLASSIC_ILLUMINATED
        )

        savedStateHandle = SavedStateHandle(
            mapOf(
                ThreadActivity.EXTRA_THREAD_ID to "thread1",
                ThreadActivity.EXTRA_ADDRESS to "+1234567890"
            )
        )

        coEvery { messageRepository.getMessagesByThread("thread1") } returns flowOf(
            listOf(testMessage1, testMessage2, testMessage3)
        )
        coEvery { messageRepository.markThreadAsRead(any()) } just Runs
        every { notificationHelper.cancelNotification(any()) } just Runs

        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `initial ui state is Loading`() {
        assertEquals(ThreadUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `ui state becomes Success after messages load`() = runTest {
        advanceUntilIdle()
        assertEquals(ThreadUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `messages flow starts with empty list`() {
        assertTrue(viewModel.messages.value.isEmpty())
    }

    @Test
    fun `messages are loaded from repository`() = runTest {
        advanceUntilIdle()

        val messages = viewModel.messages.value
        assertEquals(3, messages.size)
    }

    @Test
    fun `getThreadAddress returns correct address`() {
        assertEquals("+1234567890", viewModel.getThreadAddress())
    }

    // ==================== Thread Initialization Tests ====================

    @Test
    fun `marks thread as read on init`() = runTest {
        advanceUntilIdle()
        coVerify { messageRepository.markThreadAsRead("thread1") }
    }

    @Test
    fun `cancels notification on init`() = runTest {
        advanceUntilIdle()
        verify { notificationHelper.cancelNotification("thread1") }
    }

    // ==================== Composing Text Tests ====================

    @Test
    fun `initial composing text is empty`() {
        assertEquals("", viewModel.composingText.value)
    }

    @Test
    fun `updateComposingText updates state`() = runTest {
        viewModel.updateComposingText("Hello world")
        assertEquals("Hello world", viewModel.composingText.value)
    }

    @Test
    fun `updateComposingText allows empty string`() = runTest {
        viewModel.updateComposingText("test")
        viewModel.updateComposingText("")
        assertEquals("", viewModel.composingText.value)
    }

    // ==================== Send Message Tests ====================

    @Test
    fun `sendMessage does nothing when text is blank`() = runTest {
        viewModel.updateComposingText("")
        viewModel.sendMessage()
        advanceUntilIdle()

        coVerify(exactly = 0) { messageRepository.insertMessage(any()) }
    }

    @Test
    fun `sendMessage does nothing when text is only whitespace`() = runTest {
        viewModel.updateComposingText("   ")
        viewModel.sendMessage()
        advanceUntilIdle()

        coVerify(exactly = 0) { messageRepository.insertMessage(any()) }
    }

    // Note: sendMessage tests that actually send SMS require instrumented tests
    // because SmsManager.getDefault() is an Android framework class.
    // The validation tests (blank text, whitespace, empty address) are tested above.

    // ==================== View Original Tests ====================

    @Test
    fun `initial showOriginalDialog is null`() {
        assertNull(viewModel.showOriginalDialog.value)
    }

    @Test
    fun `showOriginalMessage sets dialog state`() = runTest {
        viewModel.showOriginalMessage(testMessage2)
        assertEquals(testMessage2, viewModel.showOriginalDialog.value)
    }

    @Test
    fun `dismissOriginalDialog clears dialog state`() = runTest {
        viewModel.showOriginalMessage(testMessage2)
        viewModel.dismissOriginalDialog()
        assertNull(viewModel.showOriginalDialog.value)
    }

    // ==================== Message Reveal Toggle Tests ====================

    @Test
    fun `initial revealed message ids is empty`() {
        assertTrue(viewModel.revealedMessageIds.value.isEmpty())
    }

    @Test
    fun `toggleMessageReveal adds message id to set`() = runTest {
        viewModel.toggleMessageReveal("msg2")
        assertTrue(viewModel.revealedMessageIds.value.contains("msg2"))
    }

    @Test
    fun `toggleMessageReveal removes message id when already present`() = runTest {
        viewModel.toggleMessageReveal("msg2")
        viewModel.toggleMessageReveal("msg2")
        assertFalse(viewModel.revealedMessageIds.value.contains("msg2"))
    }

    @Test
    fun `toggleMessageReveal can handle multiple messages`() = runTest {
        viewModel.toggleMessageReveal("msg1")
        viewModel.toggleMessageReveal("msg2")

        assertTrue(viewModel.revealedMessageIds.value.contains("msg1"))
        assertTrue(viewModel.revealedMessageIds.value.contains("msg2"))
        assertEquals(2, viewModel.revealedMessageIds.value.size)
    }

    @Test
    fun `isMessageRevealed returns correct state`() = runTest {
        assertFalse(viewModel.isMessageRevealed("msg2"))

        viewModel.toggleMessageReveal("msg2")
        assertTrue(viewModel.isMessageRevealed("msg2"))

        viewModel.toggleMessageReveal("msg2")
        assertFalse(viewModel.isMessageRevealed("msg2"))
    }

    // ==================== Empty Address Handling ====================

    @Test
    fun `sendMessage does nothing when address is blank`() = runTest {
        val emptyAddressHandle = SavedStateHandle(
            mapOf(
                ThreadActivity.EXTRA_THREAD_ID to "thread1",
                ThreadActivity.EXTRA_ADDRESS to ""
            )
        )
        val vm = createViewModel(handle = emptyAddressHandle)

        vm.updateComposingText("Test")
        vm.sendMessage()
        advanceUntilIdle()

        coVerify(exactly = 0) { messageRepository.insertMessage(any()) }
    }

    // ==================== Missing SavedState Handling ====================

    @Test
    fun `handles missing thread id gracefully`() = runTest {
        val emptyHandle = SavedStateHandle()
        coEvery { messageRepository.getMessagesByThread("") } returns flowOf(emptyList())
        val vm = ThreadViewModel(
            messageRepository, notificationHelper, contactResolver,
            annotationPipeline, textRenderingPreferences, emptyHandle
        )

        // Should default to empty string and not crash
        assertEquals("", vm.getThreadAddress())
    }

    // ==================== Annotation Tests ====================

    @Test
    fun `annotations computed for messages with entities`() = runTest {
        val vm = createViewModel(
            messages = listOf(testMessageWithEntities)
        )
        advanceUntilIdle()

        val annotations = vm.annotationsMap.value[testMessageWithEntities.id]
        assertNotNull(annotations)
        assertTrue(annotations!!.isNotEmpty())
    }

    @Test
    fun `annotations empty for plain text messages`() = runTest {
        val vm = createViewModel(
            messages = listOf(testMessage1) // "Hello there!" — no entities
        )
        advanceUntilIdle()

        val annotations = vm.annotationsMap.value[testMessage1.id]
        // No annotations means not in the map at all
        assertNull(annotations)
    }

    @Test
    fun `initial annotations map is empty`() {
        assertTrue(viewModel.annotationsMap.value.isEmpty())
    }

    // ==================== Entity Sheet State Tests ====================

    @Test
    fun `initial entity sheet state is null`() {
        assertNull(viewModel.entitySheetState.value)
    }

    @Test
    fun `onEntityClick sets entity sheet state`() {
        viewModel.onEntityClick(AnnotationType.URL, "https://example.com")
        val state = viewModel.entitySheetState.value
        assertNotNull(state)
        assertEquals(AnnotationType.URL, state!!.type)
        assertEquals("https://example.com", state.text)
    }

    @Test
    fun `dismissEntitySheet clears entity sheet state`() {
        viewModel.onEntityClick(AnnotationType.EMAIL, "test@example.com")
        viewModel.dismissEntitySheet()
        assertNull(viewModel.entitySheetState.value)
    }

    // ==================== Preference Propagation Tests ====================

    @Test
    fun `entity highlights enabled reflects preferences`() {
        assertTrue(viewModel.entityHighlightsEnabled.value)
    }

    @Test
    fun `illuminated style is null when disabled`() {
        assertNull(viewModel.illuminatedStyle.value)
    }
}
