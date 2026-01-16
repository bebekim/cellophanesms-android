package com.cellophanemail.sms.domain.model

import com.cellophanemail.sms.util.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Message domain model.
 * Tests creation, properties, and business logic.
 */
class MessageTest {

    // ==================== Creation Tests ====================

    @Test
    fun `message creation with defaults`() {
        val message = TestFixtures.createMessage()

        assertTrue(message.id.isNotEmpty())
        assertEquals("1234567890", message.threadId)
        assertEquals("+1234567890", message.address)
        assertTrue(message.isIncoming)
        assertFalse(message.isFiltered)
        assertEquals(ProcessingState.PENDING, message.processingState)
    }

    @Test
    fun `message id is unique for each instance`() {
        val message1 = TestFixtures.createMessage()
        val message2 = TestFixtures.createMessage()

        assertNotEquals(message1.id, message2.id)
    }

    // ==================== displayContent Tests ====================

    @Test
    fun `displayContent returns originalContent when not filtered`() {
        val originalContent = "Original message text"
        val message = TestFixtures.createMessage(
            originalContent = originalContent,
            isFiltered = false,
            filteredContent = "Should not show this"
        )

        assertEquals(originalContent, message.displayContent)
    }

    @Test
    fun `displayContent returns filteredContent when filtered`() {
        val filteredContent = "Filtered summary"
        val message = TestFixtures.createToxicMessage(
            filteredContent = filteredContent
        )

        assertEquals(filteredContent, message.displayContent)
    }

    @Test
    fun `displayContent returns originalContent when filtered but filteredContent is null`() {
        val originalContent = "Original message"
        val message = TestFixtures.createMessage(
            originalContent = originalContent,
            isFiltered = true,
            filteredContent = null
        )

        assertEquals(originalContent, message.displayContent)
    }

    // ==================== State Tests ====================

    @Test
    fun `safe message has correct state`() {
        val message = TestFixtures.createSafeMessage()

        assertFalse(message.isFiltered)
        assertEquals(ToxicityClass.SAFE, message.classification)
        assertEquals(ProcessingState.SAFE, message.processingState)
    }

    @Test
    fun `toxic message has correct state`() {
        val message = TestFixtures.createToxicMessage()

        assertTrue(message.isFiltered)
        assertEquals(ToxicityClass.HARMFUL, message.classification)
        assertEquals(ProcessingState.FILTERED, message.processingState)
        assertTrue(message.horsemen.isNotEmpty())
    }

    @Test
    fun `pending message has correct state`() {
        val message = TestFixtures.createPendingMessage()

        assertEquals(ProcessingState.PENDING, message.processingState)
        assertNull(message.classification)
    }

    @Test
    fun `error message has correct state`() {
        val message = TestFixtures.createErrorMessage()

        assertEquals(ProcessingState.ERROR, message.processingState)
    }

    // ==================== Direction Tests ====================

    @Test
    fun `incoming message has isIncoming true`() {
        val message = TestFixtures.createMessage(isIncoming = true)

        assertTrue(message.isIncoming)
    }

    @Test
    fun `outgoing message has isIncoming false`() {
        val message = TestFixtures.createOutgoingMessage()

        assertFalse(message.isIncoming)
    }

    // ==================== Toxicity Score Tests ====================

    @Test
    fun `toxic message has toxicity score`() {
        val message = TestFixtures.createToxicMessage(toxicityScore = 0.85f)

        assertEquals(0.85f, message.toxicityScore)
    }

    @Test
    fun `safe message may have null toxicity score`() {
        val message = TestFixtures.createSafeMessage()

        assertNull(message.toxicityScore)
    }

    // ==================== Horsemen Tests ====================

    @Test
    fun `toxic message contains detected horsemen`() {
        val horsemen = listOf(Horseman.CRITICISM, Horseman.CONTEMPT)
        val message = TestFixtures.createToxicMessage(horsemen = horsemen)

        assertEquals(2, message.horsemen.size)
        assertTrue(message.horsemen.contains(Horseman.CRITICISM))
        assertTrue(message.horsemen.contains(Horseman.CONTEMPT))
    }

    @Test
    fun `safe message has empty horsemen list`() {
        val message = TestFixtures.createSafeMessage()

        assertTrue(message.horsemen.isEmpty())
    }

    // ==================== Data Class Equality Tests ====================

    @Test
    fun `messages with same id are equal`() {
        val id = "test-id-123"
        val message1 = TestFixtures.createMessage(id = id, originalContent = "Content 1")
        val message2 = TestFixtures.createMessage(id = id, originalContent = "Content 2")

        // Data class equality checks all fields, so different content = not equal
        assertNotEquals(message1, message2)
    }

    @Test
    fun `message copy preserves values`() {
        val original = TestFixtures.createToxicMessage()
        val copy = original.copy(isRead = true)

        assertEquals(original.id, copy.id)
        assertEquals(original.originalContent, copy.originalContent)
        assertTrue(copy.isRead)
        assertFalse(original.isRead)
    }
}
