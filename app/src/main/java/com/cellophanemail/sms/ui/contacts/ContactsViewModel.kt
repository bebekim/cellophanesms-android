package com.cellophanemail.sms.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactWithRisk(
    val senderId: String,
    val displayName: String,
    val contactPhotoUri: String?,
    val messageCount: Int,
    val lastMessageTime: Long?
)

data class ContactsUiState(
    val contacts: List<ContactWithRisk> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    messageRepository: MessageRepository
) : ViewModel() {

    val uiState: StateFlow<ContactsUiState> = messageRepository.getAllThreads()
        .map { threads ->
            val contacts = threads.map { thread ->
                ContactWithRisk(
                    senderId = thread.address,
                    displayName = thread.displayName,
                    contactPhotoUri = thread.contactPhotoUri,
                    messageCount = thread.messageCount,
                    lastMessageTime = thread.lastMessageTime
                )
            }.sortedByDescending { it.lastMessageTime }
            ContactsUiState(contacts = contacts, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactsUiState()
        )
}
