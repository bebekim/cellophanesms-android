package com.cellophanemail.sms.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cellophanemail.sms.data.repository.AuthRepository
import com.cellophanemail.sms.data.repository.IdentifierType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

private val E164_REGEX = Regex("""^\+[1-9]\d{6,14}$""")

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _identifier = MutableStateFlow("")
    val identifier: StateFlow<String> = _identifier.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _identifierType = MutableStateFlow(IdentifierType.EMAIL)
    val identifierType: StateFlow<IdentifierType> = _identifierType.asStateFlow()

    fun setIdentifier(value: String) {
        _identifier.value = value
    }

    fun setPassword(value: String) {
        _password.value = value
    }

    fun setIdentifierType(type: IdentifierType) {
        if (_identifierType.value != type) {
            _identifierType.value = type
            _identifier.value = ""
        }
    }

    private fun validateIdentifier(value: String, type: IdentifierType): String? {
        if (value.isBlank()) return "This field is required"
        return when (type) {
            IdentifierType.EMAIL -> {
                if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) "Invalid email address"
                else null
            }
            IdentifierType.PHONE -> {
                if (!E164_REGEX.matches(value)) "Phone must be in E.164 format (e.g. +61412345678)"
                else null
            }
        }
    }

    fun login() {
        val currentIdentifier = _identifier.value.trim()
        val currentPassword = _password.value
        val currentType = _identifierType.value

        val error = validateIdentifier(currentIdentifier, currentType)
        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }
        if (currentPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Password is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(currentIdentifier, currentPassword).fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun register() {
        val currentIdentifier = _identifier.value.trim()
        val currentPassword = _password.value
        val currentType = _identifierType.value

        val error = validateIdentifier(currentIdentifier, currentType)
        if (error != null) {
            _uiState.value = AuthUiState.Error(error)
            return
        }
        if (currentPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Password is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.register(currentIdentifier, currentPassword, currentType).fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
