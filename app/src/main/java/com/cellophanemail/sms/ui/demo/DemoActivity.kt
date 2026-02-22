package com.cellophanemail.sms.ui.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import com.cellophanemail.sms.ui.auth.LoginActivity
import com.cellophanemail.sms.ui.components.text.EnrichedMessageText
import com.cellophanemail.sms.ui.main.MainActivity
import com.cellophanemail.sms.ui.theme.CellophaneSMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CellophaneSMSTheme {
                DemoScreen(
                    onOpenMessages = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSignIn = {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoScreen(
    viewModel: DemoViewModel = hiltViewModel(),
    onOpenMessages: () -> Unit,
    onSignIn: () -> Unit
) {
    val text by viewModel.textState.collectAsState()
    val annotations by viewModel.annotationsState.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App name
            Text(
                text = "Cellophane",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "See what's in your messages",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Editable sample SMS field
            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.onTextChanged(it) },
                label = { Text("Try it — edit this message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live entity highlight output
            if (annotations.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Detected entities",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        EnrichedMessageText(
                            text = text,
                            annotations = annotations,
                            onEntityClick = { _, _ -> }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Entity chip row
                EntityChipRow(annotations = annotations)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Primary CTA
            Button(
                onClick = onOpenMessages,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open my messages →")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary link
            TextButton(onClick = onSignIn) {
                Text("Sign in / Create account")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun AnnotationType.displayLabel(): String? = when (this) {
    AnnotationType.DATE_TIME -> "DATE"
    AnnotationType.URL -> "URL"
    AnnotationType.EMAIL -> "EMAIL"
    AnnotationType.PHONE_NUMBER -> "PHONE"
    AnnotationType.PERSON_NAME -> "PERSON"
    AnnotationType.LOCATION -> "LOCATION"
    AnnotationType.ORGANIZATION -> "ORG"
    AnnotationType.TOXICITY_SPAN,
    AnnotationType.HORSEMAN_SPAN -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntityChipRow(annotations: List<TextAnnotation>) {
    val counts = annotations
        .groupBy { it.type }
        .filterKeys { it.displayLabel() != null }
        .mapValues { it.value.size }

    if (counts.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        counts.forEach { (type, count) ->
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "${type.displayLabel()} $count",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
