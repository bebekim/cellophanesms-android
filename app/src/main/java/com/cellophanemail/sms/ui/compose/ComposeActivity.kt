package com.cellophanemail.sms.ui.compose

import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.R
import com.cellophanemail.sms.data.repository.MessageRepository
import com.cellophanemail.sms.domain.model.Message
import com.cellophanemail.sms.domain.model.ProcessingState
import com.cellophanemail.sms.ui.theme.CellophaneSMSTheme
import com.cellophanemail.sms.ui.thread.ThreadActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ComposeActivity : ComponentActivity() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract phone number from intent if present
        val initialRecipient = extractRecipient(intent)

        setContent {
            CellophaneSMSTheme {
                ComposeScreen(
                    initialRecipient = initialRecipient,
                    onBackClick = { finish() },
                    onSendMessage = { recipient, message ->
                        sendMessage(recipient, message)
                    }
                )
            }
        }
    }

    private fun extractRecipient(intent: Intent): String {
        // Handle sms:, smsto:, mms:, mmsto: URIs
        val data = intent.data?.toString() ?: return ""

        return data
            .removePrefix("sms:")
            .removePrefix("smsto:")
            .removePrefix("mms:")
            .removePrefix("mmsto:")
            .takeIf { it.isNotBlank() } ?: ""
    }

    private fun sendMessage(recipient: String, messageText: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()

            if (messageText.length > 160) {
                val parts = smsManager.divideMessage(messageText)
                smsManager.sendMultipartTextMessage(
                    recipient,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    recipient,
                    null,
                    messageText,
                    null,
                    null
                )
            }

            // Store the sent message
            val threadId = MessageRepository.generateThreadId(recipient)
            val message = Message(
                threadId = threadId,
                address = recipient,
                timestamp = System.currentTimeMillis(),
                isIncoming = false,
                originalContent = messageText,
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

            kotlinx.coroutines.runBlocking {
                messageRepository.insertMessage(message)
            }

            // Navigate to thread
            val threadIntent = Intent(this, ThreadActivity::class.java).apply {
                putExtra(ThreadActivity.EXTRA_THREAD_ID, threadId)
                putExtra(ThreadActivity.EXTRA_ADDRESS, recipient)
            }
            startActivity(threadIntent)
            finish()

            true
        } catch (e: Exception) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    initialRecipient: String,
    onBackClick: () -> Unit,
    onSendMessage: (String, String) -> Boolean
) {
    var recipient by remember { mutableStateOf(initialRecipient) }
    var messageText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compose_message)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (recipient.isNotBlank() && messageText.isNotBlank()) {
                                val success = onSendMessage(recipient, messageText)
                                if (!success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to send message")
                                    }
                                }
                            }
                        },
                        enabled = recipient.isNotBlank() && messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Recipient Field
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.recipient)) },
                placeholder = { Text(stringResource(R.string.enter_phone_number)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message Field
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Message") },
                placeholder = { Text(stringResource(R.string.type_message)) }
            )

            // Character count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${messageText.length}/160",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (messageText.length > 160)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
