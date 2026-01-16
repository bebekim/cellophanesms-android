package com.cellophanemail.sms.ui.thread

import android.telephony.SmsManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ThreadUiState {
    data object Loading : ThreadUiState()
    data object Success : ThreadUiState()
    data class Error(val message: String) : ThreadUiState()
}

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val notificationHelper: NotificationHelper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = savedStateHandle.get<String>(ThreadActivity.EXTRA_THREAD_ID) ?: ""
    private val address: String = savedStateHandle.get<String>(ThreadActivity.EXTRA_ADDRESS) ?: ""

    private val _uiState = MutableStateFlow<ThreadUiState>(ThreadUiState.Loading)
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    val messages: StateFlow<List<Message>> = messageRepository.getMessagesByThread(threadId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _composingText = MutableStateFlow("")
    val composingText: StateFlow<String> = _composingText.asStateFlow()

    private val _showOriginalDialog = MutableStateFlow<Message?>(null)
    val showOriginalDialog: StateFlow<Message?> = _showOriginalDialog.asStateFlow()

    // Track which messages have "View Original" toggled on
    private val _revealedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val revealedMessageIds: StateFlow<Set<String>> = _revealedMessageIds.asStateFlow()

    init {
        viewModelScope.launch {
            // Mark thread as read when opened
            messageRepository.markThreadAsRead(threadId)
            notificationHelper.cancelNotification(threadId)

            messages.collect {
                _uiState.value = ThreadUiState.Success
            }
        }
    }

    fun updateComposingText(text: String) {
        _composingText.value = text
    }

    fun sendMessage() {
        val text = _composingText.value.trim()
        if (text.isBlank() || address.isBlank()) return

        viewModelScope.launch {
            try {
                // Send SMS
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()

                if (text.length > 160) {
                    val parts = smsManager.divideMessage(text)
                    smsManager.sendMultipartTextMessage(
                        address,
                        null,
                        parts,
                        null,
                        null
                    )
                } else {
                    smsManager.sendTextMessage(
                        address,
                        null,
                        text,
                        null,
                        null
                    )
                }

                // Store sent message
                val message = Message(
                    threadId = threadId,
                    address = address,
                    timestamp = System.currentTimeMillis(),
                    isIncoming = false,
                    originalContent = text,
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

                messageRepository.insertMessage(message)

                // Clear composing text
                _composingText.value = ""

            } catch (e: Exception) {
                _uiState.value = ThreadUiState.Error("Failed to send message")
            }
        }
    }

    fun showOriginalMessage(message: Message) {
        _showOriginalDialog.value = message
    }

    fun dismissOriginalDialog() {
        _showOriginalDialog.value = null
    }

    fun getThreadAddress(): String = address

    fun toggleMessageReveal(messageId: String) {
        _revealedMessageIds.value = if (messageId in _revealedMessageIds.value) {
            _revealedMessageIds.value - messageId
        } else {
            _revealedMessageIds.value + messageId
        }
    }

    fun isMessageRevealed(messageId: String): Boolean {
        return messageId in _revealedMessageIds.value
    }
}
