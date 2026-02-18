package com.cellophanemail.sms.ui.thread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cellophanemail.sms.R
import com.cellophanemail.sms.ui.components.MessageBubble
import com.cellophanemail.sms.ui.components.text.EntityActionSheet
import com.cellophanemail.sms.ui.theme.CellophaneSMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThreadActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CellophaneSMSTheme {
                ThreadScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val composingText by viewModel.composingText.collectAsState()
    val revealedMessageIds by viewModel.revealedMessageIds.collectAsState()
    val annotationsMap by viewModel.annotationsMap.collectAsState()
    val illuminatedStyle by viewModel.illuminatedStyle.collectAsState()
    val entityHighlightsEnabled by viewModel.entityHighlightsEnabled.collectAsState()
    val entitySheetState by viewModel.entitySheetState.collectAsState()

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.getDisplayName()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                reverseLayout = true,
                verticalArrangement = Arrangement.Top
            ) {
                items(
                    items = messages.sortedByDescending { it.timestamp },
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        showOriginal = message.id in revealedMessageIds,
                        onToggleOriginal = {
                            viewModel.toggleMessageReveal(message.id)
                        },
                        contactName = viewModel.getDisplayName(),
                        annotations = annotationsMap[message.id] ?: emptyList(),
                        illuminatedStyle = illuminatedStyle,
                        entityHighlightsEnabled = entityHighlightsEnabled,
                        onEntityClick = { type, text ->
                            viewModel.onEntityClick(type, text)
                        }
                    )
                }
            }

            // Compose Bar
            ComposeBar(
                text = composingText,
                onTextChange = { viewModel.updateComposingText(it) },
                onSend = { viewModel.sendMessage() }
            )
        }
    }

    // Entity Action Bottom Sheet
    entitySheetState?.let { state ->
        EntityActionSheet(
            state = state,
            onDismiss = { viewModel.dismissEntitySheet() }
        )
    }
}

@Composable
fun ComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.type_message)) },
                maxLines = 4
            )

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = if (text.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
