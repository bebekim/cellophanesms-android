package com.cellophanemail.sms.data.repository

import com.cellophanemail.sms.data.contact.ContactResolver
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.ThreadEntity
import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.SmsAnalysisRequest
import com.cellophanemail.sms.domain.model.Horseman
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.MessageCategory
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.domain.model.Severity
import com.cellophanemail.sms.domain.model.Thread
import com.cellophanemail.sms.domain.model.ToxicityClass
import com.cellophanemail.sms.util.MessageEncryption
import com.cellophanemail.sms.util.PhoneNumberNormalizer
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val threadDao: ThreadDao,
    private val api: CellophoneMailApi,
    private val encryption: MessageEncryption,
    private val gson: Gson,
    private val contactResolver: ContactResolver,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {

    // Message operations
    suspend fun insertMessage(message: Message) {
        val entity = message.toEntity()
        messageDao.insert(entity)
        updateThreadForMessage(message)
    }

    suspend fun updateMessage(message: Message) {
        val entity = message.toEntity()
        messageDao.update(entity)
        updateThreadForMessage(message)
    }

    suspend fun getMessageById(id: String): Message? {
        return messageDao.getById(id)?.toDomain()
    }

    fun getMessagesByThread(threadId: String): Flow<List<Message>> {
        return messageDao.getMessagesByThread(threadId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getNextPendingMessage(): Message? {
        return messageDao.getNextPendingMessage()?.toDomain()
    }

    suspend fun markThreadAsRead(threadId: String) {
        messageDao.markThreadAsRead(threadId)
        threadDao.updateUnreadCount(threadId, 0)
    }

    // Thread operations
    fun getAllThreads(): Flow<List<Thread>> {
        return threadDao.getAllThreads().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getThreadById(threadId: String): Thread? {
        return threadDao.getById(threadId)?.toDomain()
    }

    suspend fun getOrCreateThread(address: String): Thread {
        val normalizedAddress = normalizePhoneNumber(address)
        val threadId = generateThreadId(normalizedAddress)

        var thread = threadDao.getById(threadId)
        if (thread == null) {
            val contactInfo = contactResolver.lookupContact(normalizedAddress)
            thread = ThreadEntity(
                threadId = threadId,
                address = normalizedAddress,
                contactName = contactInfo?.displayName,
                contactPhotoUri = contactInfo?.photoUri,
                lastMessageId = null,
                lastMessageTime = System.currentTimeMillis(),
                lastMessagePreview = "",
                unreadCount = 0,
                messageCount = 0
            )
            threadDao.insert(thread)
        }

        return thread.toDomain()
    }

    suspend fun archiveThread(threadId: String) {
        threadDao.archiveThread(threadId)
    }

    fun searchThreads(query: String): Flow<List<Thread>> {
        return threadDao.searchThreads(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // API operations
    suspend fun analyzeMessage(message: Message, deviceId: String?): Result<Message> {
        return try {
            val request = SmsAnalysisRequest(
                content = message.originalContent,
                sender = message.address,
                timestamp = message.timestamp,
                deviceId = deviceId
            )

            val response = api.analyzeSms(request)

            if (response.isSuccessful && response.body() != null) {
                val analysis = response.body()!!
                val classification = ToxicityClass.fromString(analysis.classification)
                val horsemen = Horseman.fromList(analysis.horsemen)

                val updatedMessage = message.copy(
                    filteredContent = analysis.filteredSummary,
                    classification = classification,
                    toxicityScore = analysis.toxicityScore,
                    horsemen = horsemen,
                    reasoning = analysis.reasoning,
                    isFiltered = classification != ToxicityClass.SAFE,
                    processingState = when (classification) {
                        ToxicityClass.SAFE -> ProcessingState.SAFE
                        else -> ProcessingState.FILTERED
                    }
                )

                updateMessage(updatedMessage)
                Result.success(updatedMessage)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            val errorMessage = message.copy(
                processingState = ProcessingState.ERROR
            )
            updateMessage(errorMessage)
            Result.failure(e)
        }
    }

    // Helper functions
    private suspend fun updateThreadForMessage(message: Message) {
        val thread = threadDao.getById(message.threadId)
        val unreadCount = messageDao.getUnreadCount(message.threadId)
        val messageCount = messageDao.getMessageCount(message.threadId)

        val preview = if (message.isFiltered && message.filteredContent != null) {
            message.filteredContent
        } else {
            message.originalContent
        }.take(100)

        val toxicityLevel = message.classification?.name ?: thread?.toxicityLevel ?: "SAFE"

        if (thread != null) {
            threadDao.update(
                thread.copy(
                    lastMessageId = message.id,
                    lastMessageTime = message.timestamp,
                    lastMessagePreview = preview,
                    unreadCount = unreadCount,
                    messageCount = messageCount,
                    toxicityLevel = toxicityLevel,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val contactInfo = contactResolver.lookupContact(message.address)
            threadDao.insert(
                ThreadEntity(
                    threadId = message.threadId,
                    address = message.address,
                    contactName = contactInfo?.displayName,
                    contactPhotoUri = contactInfo?.photoUri,
                    lastMessageId = message.id,
                    lastMessageTime = message.timestamp,
                    lastMessagePreview = preview,
                    unreadCount = unreadCount,
                    messageCount = messageCount,
                    toxicityLevel = toxicityLevel
                )
            )
        }
    }

    private fun Message.toEntity(): MessageEntity {
        // Derive category from classification and logistics
        val category = when {
            classification == null -> null
            classification == ToxicityClass.SAFE && hasLogistics -> MessageCategory.SAFE_LOGISTICS.name
            classification == ToxicityClass.SAFE -> MessageCategory.SAFE_NOISE.name
            hasLogistics -> MessageCategory.TOXIC_LOGISTICS.name
            else -> MessageCategory.TOXIC_NOISE.name
        }

        // Derive severity from toxicity score
        val severity = toxicityScore?.let { Severity.fromScore(it).name }

        return MessageEntity(
            id = id,
            threadId = threadId,
            address = address,
            senderIdNormalized = phoneNumberNormalizer.normalize(address),
            direction = if (isIncoming) "INBOUND" else "OUTBOUND",
            timestamp = timestamp,
            isIncoming = isIncoming,
            originalContent = encryption.encrypt(originalContent),
            filteredContent = filteredContent,
            isFiltered = isFiltered,
            toxicityScore = toxicityScore,
            classification = classification?.name,
            horsemenDetected = if (horsemen.isNotEmpty()) {
                gson.toJson(horsemen.map { it.name })
            } else null,
            horsemenConfidences = horsemenConfidences?.let { gson.toJson(it) },
            analysisReasoning = reasoning,
            category = category,
            severity = severity,
            hasLogistics = hasLogistics,
            engineVersion = engineVersion,
            analyzedAt = analyzedAt,
            processingState = processingState.name,
            isSent = isSent,
            isRead = isRead,
            isArchived = isArchived
        )
    }

    private fun MessageEntity.toDomain(): Message {
        val horsemenList = horsemenDetected?.let {
            try {
                val names = gson.fromJson(it, Array<String>::class.java)
                Horseman.fromList(names.toList())
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        // Parse horsemen confidences from JSON
        val confidencesMap = horsemenConfidences?.let {
            try {
                @Suppress("UNCHECKED_CAST")
                val map = gson.fromJson(it, Map::class.java) as? Map<String, Double>
                map?.mapNotNull { (key, value) ->
                    Horseman.fromString(key)?.let { horseman -> horseman to value.toFloat() }
                }?.toMap()
            } catch (e: Exception) {
                null
            }
        }

        return Message(
            id = id,
            threadId = threadId,
            address = address,
            senderIdNormalized = senderIdNormalized,
            timestamp = timestamp,
            isIncoming = isIncoming,
            originalContent = encryption.decrypt(originalContent),
            filteredContent = filteredContent,
            isFiltered = isFiltered,
            toxicityScore = toxicityScore,
            classification = ToxicityClass.fromString(classification),
            horsemen = horsemenList,
            horsemenConfidences = confidencesMap,
            reasoning = analysisReasoning,
            hasLogistics = hasLogistics,
            engineVersion = engineVersion,
            analyzedAt = analyzedAt,
            processingState = ProcessingState.fromString(processingState),
            isSent = isSent,
            isRead = isRead,
            isArchived = isArchived
        )
    }

    private fun ThreadEntity.toDomain(): Thread {
        return Thread(
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
            toxicityLevel = ToxicityClass.fromString(toxicityLevel) ?: ToxicityClass.SAFE
        )
    }

    companion object {
        fun generateThreadId(address: String): String {
            return normalizePhoneNumber(address).takeLast(10)
        }

        fun normalizePhoneNumber(address: String): String {
            return PhoneNumberNormalizer.normalizeStatic(address)
        }
    }
}
