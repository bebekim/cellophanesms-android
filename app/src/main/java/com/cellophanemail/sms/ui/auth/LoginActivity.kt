package com.cellophanemail.sms.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cellophanemail.sms.data.repository.IdentifierType
import com.cellophanemail.sms.ui.main.MainActivity
import com.cellophanemail.sms.ui.theme.CellophaneSMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CellophaneSMSTheme {
                LoginScreen(
                    onAuthSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val identifier by viewModel.identifier.collectAsState()
    val password by viewModel.password.collectAsState()
    val identifierType by viewModel.identifierType.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    var isRegisterMode by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> onAuthSuccess()
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CellophaneSMS",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRegisterMode) "Create Account" else "Sign In",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email / Phone toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = { viewModel.setIdentifierType(IdentifierType.EMAIL) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (identifierType == IdentifierType.EMAIL)
                            MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        contentColor = if (identifierType == IdentifierType.EMAIL)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Text("Email")
                }

                TextButton(
                    onClick = { viewModel.setIdentifierType(IdentifierType.PHONE) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (identifierType == IdentifierType.PHONE)
                            MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        contentColor = if (identifierType == IdentifierType.PHONE)
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Text("Phone")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = viewModel::setIdentifier,
                label = {
                    Text(
                        if (identifierType == IdentifierType.EMAIL) "Email"
                        else "Phone Number"
                    )
                },
                placeholder = {
                    Text(
                        if (identifierType == IdentifierType.EMAIL) "you@example.com"
                        else "+61412345678"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (identifierType == IdentifierType.EMAIL)
                        KeyboardType.Email
                    else KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = viewModel::setPassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isRegisterMode) viewModel.register() else viewModel.login()
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { if (isRegisterMode) viewModel.register() else viewModel.login() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "Create Account" else "Sign In")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { isRegisterMode = !isRegisterMode },
                enabled = uiState !is AuthUiState.Loading
            ) {
                Text(
                    if (isRegisterMode) "Already have an account? Sign in"
                    else "Don't have an account? Create one"
                )
            }
        }
    }
}
