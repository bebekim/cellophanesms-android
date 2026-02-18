package com.cellophanemail.sms.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.data.local.NerProviderMode

@Composable
fun NerProviderSection(
    selectedMode: NerProviderMode,
    onModeSelected: (NerProviderMode) -> Unit,
    modelDownloaded: Boolean,
    downloadProgress: Int?,
    isDownloading: Boolean,
    wifiOnlyDownload: Boolean,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onStartDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    activeProvider: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "AI Entity Recognition",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Detect people, places, and organizations in messages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Provider mode radio group
        NerProviderMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selectedMode,
                        onClick = { onModeSelected(mode) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mode == selectedMode,
                    onClick = null
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = mode.displayLabel(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = mode.description(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active provider indicator
        if (activeProvider != null && selectedMode != NerProviderMode.OFF) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active: $activeProvider",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Model download card
        ModelDownloadCard(
            modelDownloaded = modelDownloaded,
            downloadProgress = downloadProgress,
            isDownloading = isDownloading,
            wifiOnlyDownload = wifiOnlyDownload,
            onWifiOnlyChanged = onWifiOnlyChanged,
            onStartDownload = onStartDownload,
            onDeleteModel = onDeleteModel
        )
    }
}

@Composable
private fun ModelDownloadCard(
    modelDownloaded: Boolean,
    downloadProgress: Int?,
    isDownloading: Boolean,
    wifiOnlyDownload: Boolean,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onStartDownload: () -> Unit,
    onDeleteModel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Local Model",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Qwen3-0.6B (Q4_K_M)",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "~480 MB on-device storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (modelDownloaded) {
                Text(
                    text = "Downloaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDeleteModel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Model")
                }
            } else if (isDownloading) {
                Text(
                    text = "Downloading${downloadProgress?.let { " $it%" } ?: "..."}",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (downloadProgress ?: 0) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wi-Fi only",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = wifiOnlyDownload,
                        onCheckedChange = onWifiOnlyChanged
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onStartDownload) {
                    Text("Download Model")
                }
            }
        }
    }
}

private fun NerProviderMode.displayLabel(): String = when (this) {
    NerProviderMode.AUTO -> "Auto (Recommended)"
    NerProviderMode.GEMINI_NANO -> "Gemini Nano"
    NerProviderMode.QWEN3_LOCAL -> "Local Model"
    NerProviderMode.CLAUDE_CLOUD -> "Cloud"
    NerProviderMode.OFF -> "Off"
}

private fun NerProviderMode.description(): String = when (this) {
    NerProviderMode.AUTO -> "Tries on-device first, falls back to cloud"
    NerProviderMode.GEMINI_NANO -> "Fastest, requires flagship device with AICore"
    NerProviderMode.QWEN3_LOCAL -> "Qwen3-0.6B on-device, works on any device"
    NerProviderMode.CLAUDE_CLOUD -> "Claude Haiku via server, requires network"
    NerProviderMode.OFF -> "Disable AI entity recognition"
}
