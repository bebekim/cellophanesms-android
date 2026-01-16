package com.cellophanemail.sms.util

import com.cellophanemail.sms.domain.model.Horseman
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.domain.model.Thread
import com.cellophanemail.sms.domain.model.ToxicityClass
import java.util.UUID

/**
 * Test fixtures for creating sample domain objects in tests.
 * Provides factory methods with sensible defaults that can be overridden.
 */
object TestFixtures {

    // ==================== Message Fixtures ====================

    fun createMessage(
        id: String = UUID.randomUUID().toString(),
        threadId: String = "1234567890",
        address: String = "+1234567890",
        timestamp: Long = System.currentTimeMillis(),
        isIncoming: Boolean = true,
        originalContent: String = "Test message content",
        filteredContent: String? = null,
        isFiltered: Boolean = false,
        toxicityScore: Float? = null,
        classification: ToxicityClass? = null,
        horsemen: List<Horseman> = emptyList(),
        reasoning: String? = null,
        processingState: ProcessingState = ProcessingState.PENDING,
        isSent: Boolean = true,
        isRead: Boolean = false,
        isArchived: Boolean = false
    ): Message = Message(
        id = id,
        threadId = threadId,
        address = address,
        timestamp = timestamp,
        isIncoming = isIncoming,
        originalContent = originalContent,
        filteredContent = filteredContent,
        isFiltered = isFiltered,
        toxicityScore = toxicityScore,
        classification = classification,
        horsemen = horsemen,
        reasoning = reasoning,
        processingState = processingState,
        isSent = isSent,
        isRead = isRead,
        isArchived = isArchived
    )

    fun createSafeMessage(
        originalContent: String = "Hello, how are you?",
        address: String = "+1234567890"
    ): Message = createMessage(
        originalContent = originalContent,
        address = address,
        isFiltered = false,
        classification = ToxicityClass.SAFE,
        processingState = ProcessingState.SAFE
    )

    fun createToxicMessage(
        originalContent: String = "You are worthless and stupid!",
        filteredContent: String = "The sender expressed negative feelings.",
        toxicityScore: Float = 0.85f,
        classification: ToxicityClass = ToxicityClass.HARMFUL,
        horsemen: List<Horseman> = listOf(Horseman.CRITICISM, Horseman.CONTEMPT)
    ): Message = createMessage(
        originalContent = originalContent,
        filteredContent = filteredContent,
        isFiltered = true,
        toxicityScore = toxicityScore,
        classification = classification,
        horsemen = horsemen,
        reasoning = "Message contains personal attacks and contemptuous language",
        processingState = ProcessingState.FILTERED
    )

    fun createOutgoingMessage(
        originalContent: String = "I understand, let's discuss this calmly.",
        address: String = "+1234567890"
    ): Message = createMessage(
        originalContent = originalContent,
        address = address,
        isIncoming = false,
        processingState = ProcessingState.SAFE
    )

    fun createPendingMessage(
        originalContent: String = "Message waiting for analysis"
    ): Message = createMessage(
        originalContent = originalContent,
        processingState = ProcessingState.PENDING
    )

    fun createErrorMessage(
        originalContent: String = "Message that failed analysis"
    ): Message = createMessage(
        originalContent = originalContent,
        processingState = ProcessingState.ERROR
    )

    // ==================== Thread Fixtures ====================

    fun createThread(
        threadId: String = "1234567890",
        address: String = "+1234567890",
        contactName: String? = null,
        contactPhotoUri: String? = null,
        lastMessageTime: Long = System.currentTimeMillis(),
        lastMessagePreview: String = "Last message preview",
        unreadCount: Int = 0,
        messageCount: Int = 1,
        isArchived: Boolean = false,
        isPinned: Boolean = false,
        isMuted: Boolean = false,
        toxicityLevel: ToxicityClass = ToxicityClass.SAFE
    ): Thread = Thread(
        threadId = threadId,
        address = address,
        contactName = contactName,
        contactPhotoUri = contactPhotoUri,
        lastMessageTime = lastMessageTime,
        lastMessagePreview = lastMessagePreview,
        unreadCount = unreadCount,
        messageCount = messageCount,
        isArchived = isArchived,
        isPinned = isPinned,
        isMuted = isMuted,
        toxicityLevel = toxicityLevel
    )

    fun createThreadWithContact(
        contactName: String = "John Doe",
        address: String = "+1234567890"
    ): Thread = createThread(
        address = address,
        contactName = contactName
    )

    fun createToxicThread(
        address: String = "+9876543210",
        toxicityLevel: ToxicityClass = ToxicityClass.HARMFUL
    ): Thread = createThread(
        address = address,
        toxicityLevel = toxicityLevel,
        lastMessagePreview = "Filtered message summary"
    )

    fun createUnreadThread(
        unreadCount: Int = 3,
        address: String = "+5555555555"
    ): Thread = createThread(
        address = address,
        unreadCount = unreadCount
    )

    // ==================== Sample Data Lists ====================

    fun createMessageList(count: Int = 5): List<Message> {
        return (1..count).map { index ->
            createMessage(
                id = "msg-$index",
                timestamp = System.currentTimeMillis() - (index * 60000), // 1 minute apart
                originalContent = "Test message $index"
            )
        }
    }

    fun createThreadList(count: Int = 3): List<Thread> {
        val addresses = listOf("+1111111111", "+2222222222", "+3333333333", "+4444444444", "+5555555555")
        return (0 until count).map { index ->
            createThread(
                threadId = addresses[index % addresses.size].takeLast(10),
                address = addresses[index % addresses.size],
                lastMessageTime = System.currentTimeMillis() - (index * 3600000) // 1 hour apart
            )
        }
    }

    // ==================== Phone Number Samples ====================

    object PhoneNumbers {
        const val VALID_US = "+12025551234"
        const val VALID_UK = "+442071234567"
        const val VALID_AU = "+61412345678"
        const val SHORT_CODE = "12345"
        const val INVALID = "not-a-number"
    }

    // ==================== Content Samples ====================

    object Content {
        const val SAFE = "Hello! Hope you're having a great day."
        const val CRITICISM = "You never do anything right. You always mess things up."
        const val CONTEMPT = "You're pathetic and worthless. I can't believe I'm dealing with you."
        const val DEFENSIVENESS = "It's not my fault! You're the one who caused this problem!"
        const val STONEWALLING = "Whatever. I'm done talking about this."
        const val MULTIPART_LONG = "This is a very long message that exceeds the SMS character limit of 160 characters. " +
            "It will need to be split into multiple parts when sent over the SMS network. " +
            "This tests the multipart message handling capability of the SMS receiver."
    }
}
