package com.cellophanemail.sms.data.repository

import android.os.Build
import com.cellophanemail.sms.BuildConfig
import com.cellophanemail.sms.data.contact.ContactResolver
import com.cellophanemail.sms.data.local.dao.AnalysisStateDao
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.SenderSummaryDao
import com.cellophanemail.sms.data.local.entity.AnalysisStateEntity
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.AnalysisConfig
import com.cellophanemail.sms.data.remote.model.BatchAnalysisRequest
import com.cellophanemail.sms.data.remote.model.BatchAnalysisResponse
import com.cellophanemail.sms.data.remote.model.BatchMessageRequest
import com.cellophanemail.sms.data.remote.model.ClientInfo
import com.cellophanemail.sms.data.remote.model.DashboardRollup
import com.cellophanemail.sms.data.remote.model.HorsemanSignal
import com.cellophanemail.sms.data.remote.model.MessageAnalysisResult
import com.cellophanemail.sms.data.remote.model.SenderSummaryResponse
import com.cellophanemail.sms.domain.model.Horseman
import com.cellophanemail.sms.util.MessageEncryption
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(
    private val api: CellophoneMailApi,
    private val messageDao: MessageDao,
    private val senderSummaryDao: SenderSummaryDao,
    private val analysisStateDao: AnalysisStateDao,
    private val encryption: MessageEncryption,
    private val contactResolver: ContactResolver,
    private val gson: Gson
) {
    companion object {
        const val DEFAULT_BATCH_SIZE = 100
        const val ENGINE_VERSION = "v1"
    }

    // Analysis State

    suspend fun getOrCreateAnalysisState(): AnalysisStateEntity {
        return analysisStateDao.getById() ?: AnalysisStateEntity().also {
            analysisStateDao.insert(it)
        }
    }

    fun observeAnalysisState(): Flow<AnalysisStateEntity?> {
        return analysisStateDao.observeById()
    }

    suspend fun isInitialScanCompleted(): Boolean {
        return analysisStateDao.isInitialScanCompleted() ?: false
    }

    suspend fun startInitialScan(totalMessages: Int) {
        val state = getOrCreateAnalysisState()
        analysisStateDao.insert(
            state.copy(
                initialScanStartedAt = System.currentTimeMillis(),
                totalMessagesToScan = totalMessages,
                messagesScanned = 0,
                initialScanCompleted = false
            )
        )
    }

    suspend fun updateScanProgress(scanned: Int) {
        analysisStateDao.updateScanProgress(scanned)
    }

    suspend fun completeInitialScan() {
        analysisStateDao.completeInitialScan()
    }

    suspend fun updateIncrementalAnalysisTimestamp(lastMessageTimestamp: Long) {
        analysisStateDao.updateIncrementalAnalysis(
            timestamp = System.currentTimeMillis(),
            lastMessageTimestamp = lastMessageTimestamp
        )
    }

    // Batch Analysis

    suspend fun analyzeBatch(messages: List<MessageEntity>): Result<BatchAnalysisResponse> {
        if (messages.isEmpty()) {
            return Result.failure(IllegalArgumentException("No messages to analyze"))
        }

        return try {
            val request = buildBatchRequest(messages)
            val response = api.analyzeBatch(request)

            if (response.isSuccessful && response.body() != null) {
                val analysisResponse = response.body()!!

                // Process results
                processAnalysisResults(messages, analysisResponse)

                Result.success(analysisResponse)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildBatchRequest(messages: List<MessageEntity>): BatchAnalysisRequest {
        val clientInfo = ClientInfo(
            platform = "android",
            appVersion = BuildConfig.VERSION_NAME,
            timezone = TimeZone.getDefault().id
        )

        val analysisConfig = AnalysisConfig(
            engineVersion = ENGINE_VERSION
        )

        val batchMessages = messages.map { msg ->
            BatchMessageRequest(
                clientMessageId = "sms:${msg.id}",
                senderId = MessageRepository.normalizePhoneNumber(msg.address),
                direction = if (msg.isIncoming) "INBOUND" else "OUTBOUND",
                timestampMs = msg.timestamp,
                body = encryption.decrypt(msg.originalContent)
            )
        }

        return BatchAnalysisRequest(
            client = clientInfo,
            analysis = analysisConfig,
            messages = batchMessages
        )
    }

    private suspend fun processAnalysisResults(
        originalMessages: List<MessageEntity>,
        response: BatchAnalysisResponse
    ) {
        // Create lookup map for quick access
        val messageMap = originalMessages.associateBy { "sms:${it.id}" }

        // Update individual messages
        response.results.forEach { result ->
            val originalMessage = messageMap[result.clientMessageId] ?: return@forEach
            val updatedMessage = applyAnalysisToMessage(originalMessage, result, response.engineVersion)
            messageDao.update(updatedMessage)
        }

        // Update sender summaries
        response.senderSummaries?.forEach { summary ->
            updateSenderSummary(summary)
        }

        // Update dashboard rollup
        response.dashboardRollup?.let { rollup ->
            updateDashboardRollup(rollup)
        }
    }

    private fun applyAnalysisToMessage(
        message: MessageEntity,
        result: MessageAnalysisResult,
        engineVersion: String
    ): MessageEntity {
        val horsemenJson = result.signals.fourHorsemen?.let { signals ->
            gson.toJson(signals.map { it.type })
        }

        val toxicityScore = result.signals.fourHorsemen?.let { signals ->
            signals.map { it.confidence }.average().toFloat()
        }

        return message.copy(
            isFiltered = result.classification.isFiltered,
            category = result.classification.category,
            severity = result.classification.severity,
            toxicityScore = toxicityScore,
            horsemenDetected = horsemenJson,
            hasLogistics = result.signals.hasLogistics ?: false,
            analysisReasoning = result.explain?.shortReason,
            engineVersion = engineVersion,
            analyzedAt = System.currentTimeMillis(),
            processingState = if (result.classification.isFiltered) "FILTERED" else "SAFE",
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun updateSenderSummary(response: SenderSummaryResponse) {
        val existing = senderSummaryDao.getBySenderId(response.senderId)
        val contactInfo = contactResolver.lookupContact(response.senderId)

        val summary = SenderSummaryEntity(
            senderId = response.senderId,
            contactName = contactInfo?.displayName ?: existing?.contactName,
            contactPhotoUri = contactInfo?.photoUri ?: existing?.contactPhotoUri,
            filteredCount = response.filteredCount,
            noiseCount = response.noiseCount,
            toxicLogisticsCount = response.toxicLogisticsCount,
            criticismCount = response.horsemenCounts?.criticism ?: 0,
            contemptCount = response.horsemenCounts?.contempt ?: 0,
            defensivenessCount = response.horsemenCounts?.defensiveness ?: 0,
            stonewallingCount = response.horsemenCounts?.stonewalling ?: 0,
            lastFilteredTimestamp = response.lastFilteredTimestampMs,
            engineVersion = ENGINE_VERSION,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        senderSummaryDao.insert(summary)
    }

    private suspend fun updateDashboardRollup(rollup: DashboardRollup) {
        analysisStateDao.updateDashboardRollup(
            toxicSenders = rollup.toxicSenders,
            filteredMessages = rollup.filteredMessages,
            noiseMessages = rollup.noiseMessages,
            toxicLogisticsMessages = rollup.toxicLogisticsMessages
        )
    }

    // Message queries for analysis

    suspend fun getUnanalyzedMessages(limit: Int = DEFAULT_BATCH_SIZE): List<MessageEntity> {
        return messageDao.getUnanalyzedMessages(limit)
    }

    suspend fun getPendingMessages(limit: Int = DEFAULT_BATCH_SIZE): List<MessageEntity> {
        return messageDao.getPendingMessages(limit)
    }

    suspend fun getMessagesSince(timestamp: Long, limit: Int = DEFAULT_BATCH_SIZE): List<MessageEntity> {
        return messageDao.getMessagesSince(timestamp, limit)
    }

    suspend fun getAllPendingMessagesCount(): Int {
        return messageDao.getPendingCount()
    }

    suspend fun getUnanalyzedCount(): Int {
        return messageDao.getUnanalyzedCount()
    }

    suspend fun getTotalMessageCount(): Int {
        return messageDao.getTotalMessageCount()
    }

    suspend fun getLastAnalyzedMessageTimestamp(): Long? {
        return messageDao.getLastAnalyzedMessageTimestamp()
    }

    // Sender summary queries

    fun observeTopRiskSenders(limit: Int = 5): Flow<List<SenderSummaryEntity>> {
        return senderSummaryDao.getTopRiskSenders(limit)
    }

    fun observeRiskSendersRanked(
        limit: Int = 10,
        recentThresholdMs: Long = 7 * 24 * 60 * 60 * 1000L // 7 days
    ): Flow<List<SenderSummaryEntity>> {
        val threshold = System.currentTimeMillis() - recentThresholdMs
        return senderSummaryDao.getRiskSendersRanked(limit, threshold)
    }

    suspend fun getSenderSummary(senderId: String): SenderSummaryEntity? {
        return senderSummaryDao.getBySenderId(senderId)
    }

    fun observeSenderSummary(senderId: String): Flow<SenderSummaryEntity?> {
        return senderSummaryDao.observeBySenderId(senderId)
    }

    // Dashboard metrics

    suspend fun getDashboardMetrics(): DashboardMetrics {
        val state = getOrCreateAnalysisState()
        val toxicSenderCount = senderSummaryDao.getToxicSenderCount()
        val filteredCount = senderSummaryDao.getTotalFilteredCount() ?: 0
        val noiseCount = senderSummaryDao.getTotalNoiseCount() ?: 0
        val toxicLogisticsCount = senderSummaryDao.getTotalToxicLogisticsCount() ?: 0
        val horsemenCounts = senderSummaryDao.getAggregatedHorsemenCounts()

        return DashboardMetrics(
            toxicSenders = toxicSenderCount,
            filteredMessages = filteredCount,
            noiseMessages = noiseCount,
            toxicLogisticsMessages = toxicLogisticsCount,
            criticismCount = horsemenCounts?.criticism ?: 0,
            contemptCount = horsemenCounts?.contempt ?: 0,
            defensivenessCount = horsemenCounts?.defensiveness ?: 0,
            stonewallingCount = horsemenCounts?.stonewalling ?: 0,
            initialScanCompleted = state.initialScanCompleted,
            scanProgress = state.scanProgress
        )
    }

    fun observeToxicSenderCount(): Flow<Int> {
        return senderSummaryDao.observeToxicSenderCount()
    }

    fun observeTotalFilteredCount(): Flow<Int?> {
        return senderSummaryDao.observeTotalFilteredCount()
    }
}

data class DashboardMetrics(
    val toxicSenders: Int,
    val filteredMessages: Int,
    val noiseMessages: Int,
    val toxicLogisticsMessages: Int,
    val criticismCount: Int,
    val contemptCount: Int,
    val defensivenessCount: Int,
    val stonewallingCount: Int,
    val initialScanCompleted: Boolean,
    val scanProgress: Float
)
