package com.cellophanemail.sms.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import com.cellophanemail.sms.data.repository.AnalysisRepository
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.HorsemenCounts
import com.cellophanemail.sms.domain.model.RiskLevel
import com.cellophanemail.sms.domain.model.SenderSummary
import com.cellophanemail.sms.domain.model.Thread
import com.cellophanemail.sms.workers.AnalysisWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SenderStats(
    val displayName: String,
    val address: String,
    val messageCount: Int,
    val contactPhotoUri: String?
)

data class RiskSenderStats(
    val senderId: String,
    val displayName: String,
    val contactPhotoUri: String?,
    val filteredCount: Int,
    val toxicLogisticsCount: Int, // Toxic but important (high signal)
    val riskLevel: RiskLevel,
    val dominantHorseman: String?
)

/**
 * 2x2 Matrix of message categories:
 *
 *                 │ Actionable (Logistics) │ Noise (No Logistics)
 * ────────────────┼────────────────────────┼─────────────────────
 * Toxic           │ toxicLogistics         │ toxicNoise
 * (Four Horsemen) │ ⚠️ NEEDS REVIEW        │ 🚫 FILTERED OUT
 * ────────────────┼────────────────────────┼─────────────────────
 * Safe            │ safeLogistics          │ safeNoise
 * (No Horsemen)   │ ✅ CLEAR               │ 😐 LOW PRIORITY
 */
data class MessageMatrix(
    val toxicLogistics: Int = 0,   // Important but harmful - MUST SEE with armor
    val toxicNoise: Int = 0,       // Harmful, no info - FILTER OUT
    val safeLogistics: Int = 0,    // Important and safe - SHOW
    val safeNoise: Int = 0         // Not important - LOW PRIORITY
) {
    val totalToxic: Int get() = toxicLogistics + toxicNoise
    val totalSafe: Int get() = safeLogistics + safeNoise
    val totalLogistics: Int get() = toxicLogistics + safeLogistics
    val totalNoise: Int get() = toxicNoise + safeNoise
    val total: Int get() = toxicLogistics + toxicNoise + safeLogistics + safeNoise
}

data class AnalysisMetrics(
    val riskSenders: Int = 0,
    val messageMatrix: MessageMatrix = MessageMatrix(),
    val horsemenCounts: HorsemenCounts = HorsemenCounts()
)

data class ScanState(
    val isInitialScanCompleted: Boolean = false,
    val isScanInProgress: Boolean = false,
    val scanProgress: Float = 0f,
    val messagesScanned: Int = 0,
    val totalMessagesToScan: Int = 0
)

data class DashboardUiState(
    val totalMessages: Int = 0,
    val totalSenders: Int = 0,
    val senderStats: List<SenderStats> = emptyList(),
    val riskSenders: List<RiskSenderStats> = emptyList(),
    val analysisMetrics: AnalysisMetrics = AnalysisMetrics(),
    val scanState: ScanState = ScanState(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val analysisRepository: AnalysisRepository,
    private val analysisWorkManager: AnalysisWorkManager
) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState())

    val uiState: StateFlow<DashboardUiState> = combine(
        messageRepository.getAllThreads(),
        analysisRepository.observeTopRiskSenders(5),
        analysisRepository.observeAnalysisState(),
        analysisRepository.observeToxicSenderCount(),
        analysisRepository.observeTotalFilteredCount()
    ) { threads, riskSenders, analysisState, toxicCount, filteredCount ->
        buildDashboardState(
            threads = threads,
            riskSenders = riskSenders,
            toxicSenderCount = toxicCount,
            filteredCount = filteredCount ?: 0,
            isInitialScanCompleted = analysisState?.initialScanCompleted ?: false,
            scanProgress = analysisState?.scanProgress ?: 0f,
            isScanInProgress = analysisState?.isScanInProgress ?: false,
            messagesScanned = analysisState?.messagesScanned ?: 0,
            totalMessagesToScan = analysisState?.totalMessagesToScan ?: 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        // Load initial metrics
        viewModelScope.launch {
            loadAnalysisMetrics()
        }
    }

    fun startInitialScan() {
        analysisWorkManager.startInitialScan()
    }

    fun triggerIncrementalAnalysis() {
        analysisWorkManager.triggerIncrementalAnalysis()
    }

    private suspend fun loadAnalysisMetrics() {
        // Metrics are loaded via Flow in uiState, this ensures initial state is ready
        analysisRepository.getDashboardMetrics()
    }

    @Suppress("UNUSED_PARAMETER") // filteredCount reserved for future use
    private fun buildDashboardState(
        threads: List<Thread>,
        riskSenders: List<SenderSummaryEntity>,
        toxicSenderCount: Int,
        filteredCount: Int,
        isInitialScanCompleted: Boolean,
        scanProgress: Float,
        isScanInProgress: Boolean,
        messagesScanned: Int,
        totalMessagesToScan: Int
    ): DashboardUiState {
        val senderStats = threads
            .sortedByDescending { it.messageCount }
            .map { thread ->
                SenderStats(
                    displayName = thread.displayName,
                    address = thread.address,
                    messageCount = thread.messageCount,
                    contactPhotoUri = thread.contactPhotoUri
                )
            }

        val riskSenderStats = riskSenders.map { entity ->
            RiskSenderStats(
                senderId = entity.senderId,
                displayName = entity.contactName ?: entity.senderId,
                contactPhotoUri = entity.contactPhotoUri,
                filteredCount = entity.filteredCount,
                toxicLogisticsCount = entity.toxicLogisticsCount,
                riskLevel = entity.toRiskLevel(),
                dominantHorseman = entity.dominantHorseman
            )
        }

        // Calculate matrix counts from sender summaries
        val totalToxicLogistics = riskSenders.sumOf { it.toxicLogisticsCount }
        val totalNoise = riskSenders.sumOf { it.noiseCount }
        val totalFiltered = riskSenders.sumOf { it.filteredCount }
        // toxicNoise = filtered - toxicLogistics (filtered includes both toxic categories)
        val toxicNoise = (totalFiltered - totalToxicLogistics).coerceAtLeast(0)

        return DashboardUiState(
            totalMessages = threads.sumOf { it.messageCount },
            totalSenders = threads.size,
            senderStats = senderStats,
            riskSenders = riskSenderStats,
            analysisMetrics = AnalysisMetrics(
                riskSenders = toxicSenderCount,
                messageMatrix = MessageMatrix(
                    toxicLogistics = totalToxicLogistics,
                    toxicNoise = toxicNoise,
                    safeLogistics = 0, // TODO: track safe logistics separately
                    safeNoise = totalNoise
                ),
                horsemenCounts = HorsemenCounts(
                    criticism = riskSenders.sumOf { it.criticismCount },
                    contempt = riskSenders.sumOf { it.contemptCount },
                    defensiveness = riskSenders.sumOf { it.defensivenessCount },
                    stonewalling = riskSenders.sumOf { it.stonewallingCount }
                )
            ),
            scanState = ScanState(
                isInitialScanCompleted = isInitialScanCompleted,
                isScanInProgress = isScanInProgress,
                scanProgress = scanProgress,
                messagesScanned = messagesScanned,
                totalMessagesToScan = totalMessagesToScan
            ),
            isLoading = false
        )
    }

    private fun SenderSummaryEntity.toRiskLevel(): RiskLevel {
        return when {
            filteredCount >= 10 || toxicLogisticsCount >= 5 -> RiskLevel.HIGH
            filteredCount >= 5 || toxicLogisticsCount >= 2 -> RiskLevel.MEDIUM
            filteredCount > 0 -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }
    }
}
