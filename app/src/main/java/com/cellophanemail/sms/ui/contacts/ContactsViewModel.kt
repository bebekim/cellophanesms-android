package com.cellophanemail.sms.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import com.cellophanemail.sms.data.repository.AnalysisRepository
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.RiskLevel
import com.cellophanemail.sms.domain.model.Thread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactWithRisk(
    val senderId: String,
    val displayName: String,
    val contactPhotoUri: String?,
    val messageCount: Int,
    val filteredCount: Int,
    val riskLevel: RiskLevel,
    val dominantHorseman: String?,
    val lastMessageTime: Long?
)

data class ContactsUiState(
    val contacts: List<ContactWithRisk> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    messageRepository: MessageRepository,
    analysisRepository: AnalysisRepository
) : ViewModel() {

    val uiState: StateFlow<ContactsUiState> = combine(
        messageRepository.getAllThreads(),
        analysisRepository.observeRiskSendersRanked(limit = 100)
    ) { threads, riskSenders ->
        buildContactsState(threads, riskSenders)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContactsUiState()
    )

    private fun buildContactsState(
        threads: List<Thread>,
        riskSenders: List<SenderSummaryEntity>
    ): ContactsUiState {
        // Create a map of senderId to risk data
        val riskMap = riskSenders.associateBy {
            MessageRepository.normalizePhoneNumber(it.senderId)
        }

        // Merge thread data with risk data
        val contacts = threads.map { thread ->
            val normalizedAddress = MessageRepository.normalizePhoneNumber(thread.address)
            val riskData = riskMap[normalizedAddress]

            ContactWithRisk(
                senderId = thread.address,
                displayName = thread.displayName,
                contactPhotoUri = thread.contactPhotoUri,
                messageCount = thread.messageCount,
                filteredCount = riskData?.filteredCount ?: 0,
                riskLevel = riskData?.toRiskLevel() ?: RiskLevel.NONE,
                dominantHorseman = riskData?.dominantHorseman,
                lastMessageTime = thread.lastMessageTime
            )
        }.sortedWith(
            // Sort: risk senders first, then by message count
            compareByDescending<ContactWithRisk> { it.riskLevel.ordinal }
                .thenByDescending { it.filteredCount }
                .thenByDescending { it.messageCount }
        )

        return ContactsUiState(
            contacts = contacts,
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
