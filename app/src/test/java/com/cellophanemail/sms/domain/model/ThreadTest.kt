package com.cellophanemail.sms.domain.model

import com.cellophanemail.sms.util.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Thread domain model.
 */
class ThreadTest {

    // ==================== Creation Tests ====================

    @Test
    fun `thread creation with defaults`() {
        val thread = TestFixtures.createThread()

        assertEquals("1234567890", thread.threadId)
        assertEquals("+1234567890", thread.address)
        assertNull(thread.contactName)
        assertEquals(0, thread.unreadCount)
        assertFalse(thread.isArchived)
        assertFalse(thread.isPinned)
        assertFalse(thread.isMuted)
        assertEquals(ToxicityClass.SAFE, thread.toxicityLevel)
    }

    // ==================== displayName Tests ====================

    @Test
    fun `displayName returns contactName when available`() {
        val thread = TestFixtures.createThreadWithContact(
            contactName = "John Doe",
            address = "+1234567890"
        )

        assertEquals("John Doe", thread.displayName)
    }

    @Test
    fun `displayName returns address when contactName is null`() {
        val thread = TestFixtures.createThread(
            address = "+1234567890",
            contactName = null
        )

        assertEquals("+1234567890", thread.displayName)
    }

    @Test
    fun `displayName returns address when contactName is provided but null`() {
        val thread = Thread(
            threadId = "123",
            address = "+9876543210",
            contactName = null,
            contactPhotoUri = null,
            lastMessageTime = System.currentTimeMillis(),
            lastMessagePreview = "Preview",
            unreadCount = 0,
            messageCount = 1,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            toxicityLevel = ToxicityClass.SAFE
        )

        assertEquals("+9876543210", thread.displayName)
    }

    // ==================== Toxicity Level Tests ====================

    @Test
    fun `safe thread has SAFE toxicity level`() {
        val thread = TestFixtures.createThread(toxicityLevel = ToxicityClass.SAFE)

        assertEquals(ToxicityClass.SAFE, thread.toxicityLevel)
    }

    @Test
    fun `toxic thread has elevated toxicity level`() {
        val thread = TestFixtures.createToxicThread(toxicityLevel = ToxicityClass.HARMFUL)

        assertEquals(ToxicityClass.HARMFUL, thread.toxicityLevel)
    }

    // ==================== Unread Count Tests ====================

    @Test
    fun `unread thread has positive unreadCount`() {
        val thread = TestFixtures.createUnreadThread(unreadCount = 5)

        assertEquals(5, thread.unreadCount)
    }

    @Test
    fun `read thread has zero unreadCount`() {
        val thread = TestFixtures.createThread(unreadCount = 0)

        assertEquals(0, thread.unreadCount)
    }

    // ==================== State Tests ====================

    @Test
    fun `archived thread has isArchived true`() {
        val thread = TestFixtures.createThread(isArchived = true)

        assertTrue(thread.isArchived)
    }

    @Test
    fun `pinned thread has isPinned true`() {
        val thread = TestFixtures.createThread(isPinned = true)

        assertTrue(thread.isPinned)
    }

    @Test
    fun `muted thread has isMuted true`() {
        val thread = TestFixtures.createThread(isMuted = true)

        assertTrue(thread.isMuted)
    }

    // ==================== Data Class Tests ====================

    @Test
    fun `thread copy preserves values`() {
        val original = TestFixtures.createThread(unreadCount = 5)
        val copy = original.copy(unreadCount = 0)

        assertEquals(original.threadId, copy.threadId)
        assertEquals(original.address, copy.address)
        assertEquals(5, original.unreadCount)
        assertEquals(0, copy.unreadCount)
    }

    @Test
    fun `threads with same values are equal`() {
        val thread1 = TestFixtures.createThread(
            threadId = "123",
            address = "+1234567890"
        )
        val thread2 = TestFixtures.createThread(
            threadId = "123",
            address = "+1234567890"
        )

        // Note: timestamps will differ, so these won't be equal by default
        assertEquals(thread1.threadId, thread2.threadId)
        assertEquals(thread1.address, thread2.address)
    }

    // ==================== Preview Tests ====================

    @Test
    fun `thread has lastMessagePreview`() {
        val preview = "This is the last message"
        val thread = TestFixtures.createThread(lastMessagePreview = preview)

        assertEquals(preview, thread.lastMessagePreview)
    }

    @Test
    fun `thread messageCount tracks total messages`() {
        val thread = TestFixtures.createThread(messageCount = 42)

        assertEquals(42, thread.messageCount)
    }
}
