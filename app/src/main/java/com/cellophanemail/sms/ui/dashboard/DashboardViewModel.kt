package com.cellophanemail.sms.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Thread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SenderStats(
    val displayName: String,
    val address: String,
    val messageCount: Int,
    val contactPhotoUri: String?
)

data class DashboardUiState(
    val totalMessages: Int = 0,
    val totalSenders: Int = 0,
    val senderStats: List<SenderStats> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    messageRepository: MessageRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = messageRepository.getAllThreads()
        .map { threads -> buildDashboardState(threads) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )

    private fun buildDashboardState(threads: List<Thread>): DashboardUiState {
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

        return DashboardUiState(
            totalMessages = threads.sumOf { it.messageCount },
            totalSenders = threads.size,
            senderStats = senderStats,
            isLoading = false
        )
    }
}
