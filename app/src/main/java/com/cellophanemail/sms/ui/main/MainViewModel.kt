package com.cellophanemail.sms.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Thread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainUiState {
    data object Loading : MainUiState()
    data object Success : MainUiState()
    data class Error(val message: String) : MainUiState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val threads: StateFlow<List<Thread>> = messageRepository.getAllThreads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Thread>>(emptyList())
    val searchResults: StateFlow<List<Thread>> = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            threads.collect { threadList ->
                _uiState.value = MainUiState.Success
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                messageRepository.searchThreads(query).collect { results ->
                    _searchResults.value = results
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun archiveThread(threadId: String) {
        viewModelScope.launch {
            try {
                messageRepository.archiveThread(threadId)
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Failed to archive thread")
            }
        }
    }

    fun markThreadAsRead(threadId: String) {
        viewModelScope.launch {
            try {
                messageRepository.markThreadAsRead(threadId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
