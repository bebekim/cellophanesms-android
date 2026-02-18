package com.cellophanemail.sms.ui.thread

import android.telephony.SmsManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.contact.ContactResolver
import com.cellophanemail.sms.data.local.TextRenderingPreferences
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.annotation.AnnotationPipeline
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.IlluminatedStyle
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.domain.model.TextAnnotation
import com.cellophanemail.sms.ui.components.text.EntitySheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val notificationHelper: com.cellophanemail.sms.util.NotificationHelper,
    private val contactResolver: ContactResolver,
    private val annotationPipeline: AnnotationPipeline,
    private val textRenderingPreferences: TextRenderingPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = savedStateHandle.get<String>(ThreadActivity.EXTRA_THREAD_ID) ?: ""
    private val address: String = savedStateHandle.get<String>(ThreadActivity.EXTRA_ADDRESS) ?: ""
    private val contactName: String by lazy {
        contactResolver.lookupContact(address)?.displayName ?: address
    }

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

    // Annotation state — lazily computed per message
    private val _annotationsMap = MutableStateFlow<Map<String, List<TextAnnotation>>>(emptyMap())
    val annotationsMap: StateFlow<Map<String, List<TextAnnotation>>> = _annotationsMap.asStateFlow()

    // Entity bottom sheet state
    private val _entitySheetState = MutableStateFlow<EntitySheetState?>(null)
    val entitySheetState: StateFlow<EntitySheetState?> = _entitySheetState.asStateFlow()

    // Text rendering preferences
    val illuminatedEnabled: StateFlow<Boolean> = textRenderingPreferences.illuminatedEnabled
    val entityHighlightsEnabled: StateFlow<Boolean> = textRenderingPreferences.entityHighlightsEnabled

    val illuminatedStyle: StateFlow<IlluminatedStyle?> = combine(
        textRenderingPreferences.illuminatedEnabled,
        textRenderingPreferences.selectedStylePack
    ) { enabled, pack ->
        if (enabled) pack.style else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            // Mark thread as read when opened
            messageRepository.markThreadAsRead(threadId)
            notificationHelper.cancelNotification(threadId)

            messages.collect { messageList ->
                _uiState.value = ThreadUiState.Success
                computeAnnotations(messageList)
            }
        }
    }

    private suspend fun computeAnnotations(messageList: List<Message>) {
        val activeIds = messageList.map { it.id }.toSet()

        // Prune entries for messages no longer in the list
        val current = _annotationsMap.value
        if (current.keys.any { it !in activeIds }) {
            _annotationsMap.value = current.filterKeys { it in activeIds }
        }

        for (message in messageList) {
            if (_annotationsMap.value.containsKey(message.id)) continue

            annotationPipeline.annotateProgressive(message.displayContent)
                .collect { annotations ->
                    if (annotations.isNotEmpty()) {
                        _annotationsMap.value = _annotationsMap.value.toMutableMap().apply {
                            put(message.id, annotations)
                        }
                    }
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

    fun getDisplayName(): String = contactName

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

    fun onEntityClick(type: AnnotationType, text: String) {
        _entitySheetState.value = EntitySheetState(type, text)
    }

    fun dismissEntitySheet() {
        _entitySheetState.value = null
    }
}
