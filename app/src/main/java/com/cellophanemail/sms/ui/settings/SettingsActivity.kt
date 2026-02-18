package com.cellophanemail.sms.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cellophanemail.sms.BuildConfig
import com.cellophanemail.sms.R
import com.cellophanemail.sms.data.local.NerProviderMode
import com.cellophanemail.sms.data.local.TextRenderingPreferences
import com.cellophanemail.sms.debug.TestDataSeeder
import com.cellophanemail.sms.domain.model.IlluminatedStylePack
import com.cellophanemail.sms.ui.auth.LoginActivity
import com.cellophanemail.sms.ui.components.text.IlluminatedInitial
import com.cellophanemail.sms.ui.theme.CellophaneSMSTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var testDataSeeder: TestDataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CellophaneSMSTheme {
                SettingsScreen(
                    onBackClick = { finish() },
                    onSeedTestData = { testDataSeeder },
                    isDebugBuild = BuildConfig.DEBUG,
                    onLoggedOut = {
                        val intent = Intent(this, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onSeedTestData: () -> TestDataSeeder,
    isDebugBuild: Boolean = false,
    onLoggedOut: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSeeding by remember { mutableStateOf(false) }

    // Dialog state
    var showAccountDialog by remember { mutableStateOf(false) }
    var showFilteringDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showTextStyleDialog by remember { mutableStateOf(false) }
    var showNerSettingsDialog by remember { mutableStateOf(false) }

    val profileState by viewModel.profileState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
                .verticalScroll(rememberScrollState())
        ) {
            SettingsItem(
                icon = Icons.Default.AccountCircle,
                title = stringResource(R.string.settings_account),
                subtitle = "Manage your account and subscription",
                onClick = {
                    viewModel.loadProfile()
                    showAccountDialog = true
                }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.FilterList,
                title = stringResource(R.string.settings_filtering),
                subtitle = "Configure message filtering preferences",
                onClick = { showFilteringDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_notifications),
                subtitle = "Notification sounds and alerts",
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.settings_privacy),
                subtitle = "Data storage and encryption settings",
                onClick = { showPrivacyDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.FormatSize,
                title = stringResource(R.string.settings_text_style),
                subtitle = stringResource(R.string.settings_text_style_subtitle),
                onClick = { showTextStyleDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Face,
                title = "AI Entity Recognition",
                subtitle = "Detect people, places, and organizations",
                onClick = { showNerSettingsDialog = true }
            )

            HorizontalDivider()

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_about),
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                onClick = { showAboutDialog = true }
            )

            HorizontalDivider()

            // Logout
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out of your account",
                onClick = { showLogoutConfirm = true }
            )

            // Debug section - only visible in debug builds
            if (isDebugBuild) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )

                Text(
                    text = "Debug Tools",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Seed Test Data",
                    subtitle = if (isSeeding) "Seeding..." else "Populate database with test messages",
                    onClick = {
                        if (!isSeeding) {
                            isSeeding = true
                            scope.launch {
                                try {
                                    onSeedTestData().seedTestData()
                                    Toast.makeText(context, "Test data seeded successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSeeding = false
                                }
                            }
                        }
                    }
                )

                HorizontalDivider()

                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear All Data",
                    subtitle = "Remove all messages and sender data",
                    onClick = {
                        scope.launch {
                            try {
                                onSeedTestData().clearAllData()
                                Toast.makeText(context, "Data cleared!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                HorizontalDivider()

                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Reset & Reseed",
                    subtitle = "Clear data and seed fresh test data",
                    onClick = {
                        if (!isSeeding) {
                            isSeeding = true
                            scope.launch {
                                try {
                                    val seeder = onSeedTestData()
                                    seeder.clearAllData()
                                    seeder.seedTestData()
                                    Toast.makeText(context, "Data reset and reseeded!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isSeeding = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    // Account Dialog
    if (showAccountDialog) {
        AccountDialog(
            profileState = profileState,
            onDismiss = { showAccountDialog = false }
        )
    }

    // Filtering Dialog
    if (showFilteringDialog) {
        FilteringDialog(onDismiss = { showFilteringDialog = false })
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Text Style Dialog
    if (showTextStyleDialog) {
        TextStyleDialog(
            viewModel = viewModel,
            onDismiss = { showTextStyleDialog = false }
        )
    }

    // NER Settings Dialog
    if (showNerSettingsDialog) {
        NerSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showNerSettingsDialog = false }
        )
    }

    // Logout Confirmation
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out? You'll need to sign in again to use the app.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                    onLoggedOut()
                }) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AccountDialog(
    profileState: ProfileState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_account)) },
        text = {
            when (profileState) {
                is ProfileState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ProfileState.Loaded -> {
                    val profile = profileState.profile
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileRow("Email", profile.email)
                        profile.username?.let { ProfileRow("Username", it) }
                        profile.subscriptionStatus?.let { ProfileRow("Plan", it) }
                        profile.apiQuota?.let { quota ->
                            ProfileRow("API Usage", "${quota.used} / ${quota.limit}")
                        }
                    }
                }
                is ProfileState.Error -> {
                    Text(
                        text = profileState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilteringDialog(onDismiss: () -> Unit) {
    var filterCriticism by remember { mutableStateOf(true) }
    var filterContempt by remember { mutableStateOf(true) }
    var filterDefensiveness by remember { mutableStateOf(true) }
    var filterStonewalling by remember { mutableStateOf(true) }
    var alwaysPassLogistics by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_filtering)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Four Horsemen Detection",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                FilterToggle("Criticism", filterCriticism) { filterCriticism = it }
                FilterToggle("Contempt", filterContempt) { filterContempt = it }
                FilterToggle("Defensiveness", filterDefensiveness) { filterDefensiveness = it }
                FilterToggle("Stonewalling", filterStonewalling) { filterStonewalling = it }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Logistics Handling",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                FilterToggle(
                    label = "Always show logistics",
                    checked = alwaysPassLogistics,
                    onCheckedChange = { alwaysPassLogistics = it }
                )

                Text(
                    text = "When enabled, messages with actionable info (dates, addresses, amounts) are always shown, even if toxic patterns are detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun FilterToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_privacy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileRow("Encryption", "AES-256-GCM")
                ProfileRow("Key Storage", "Android Keystore")
                ProfileRow("Message Storage", "On-device only")
                ProfileRow("API Processing", "Ephemeral (5-min TTL)")

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your message content is encrypted at rest using AES-256-GCM with keys stored in the Android Keystore. Messages sent for analysis are processed ephemerally and not stored on our servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_about)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileRow("Version", BuildConfig.VERSION_NAME)
                ProfileRow("Build", BuildConfig.VERSION_CODE.toString())
                ProfileRow("Build Type", BuildConfig.BUILD_TYPE)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "CellophaneSMS protects you from toxic communication patterns using the Gottman Four Horsemen framework. Messages are analyzed for criticism, contempt, defensiveness, and stonewalling - filtering harmful noise while ensuring you never miss important logistics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TextStyleDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val illuminatedEnabled by viewModel.illuminatedEnabled.collectAsState()
    val selectedPack by viewModel.selectedStylePack.collectAsState()
    val entityHighlightsEnabled by viewModel.entityHighlightsEnabled.collectAsState()
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_text_style)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Illuminated Initials toggle
                FilterToggle(
                    label = stringResource(R.string.settings_illuminated_initials),
                    checked = illuminatedEnabled,
                    onCheckedChange = { viewModel.setIlluminatedEnabled(it) }
                )
                Text(
                    text = stringResource(R.string.settings_illuminated_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )

                // Style pack selector (only visible when illuminated is enabled)
                if (illuminatedEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_style_pack),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    IlluminatedStylePack.entries.forEach { pack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = pack == selectedPack,
                                    onClick = { viewModel.setSelectedStylePack(pack) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = pack == selectedPack,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pack.style.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            // Preview letter
                            Box(modifier = Modifier.size(36.dp)) {
                                IlluminatedInitial(
                                    letter = 'A',
                                    style = pack.style,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Entity Highlights toggle
                FilterToggle(
                    label = stringResource(R.string.settings_entity_highlights),
                    checked = entityHighlightsEnabled,
                    onCheckedChange = { viewModel.setEntityHighlightsEnabled(it) }
                )
                Text(
                    text = stringResource(R.string.settings_entity_highlights_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun NerSettingsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val selectedMode by viewModel.nerProviderMode.collectAsState()
    val modelDownloaded by viewModel.nerModelDownloaded.collectAsState()
    val wifiOnly by viewModel.nerWifiOnlyDownload.collectAsState()
    val downloadProgress by viewModel.nerDownloadProgress.collectAsState()
    val isDownloading by viewModel.nerIsDownloading.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Entity Recognition") },
        text = {
            NerProviderSection(
                selectedMode = selectedMode,
                onModeSelected = { viewModel.setNerProviderMode(it) },
                modelDownloaded = modelDownloaded,
                downloadProgress = downloadProgress,
                isDownloading = isDownloading,
                wifiOnlyDownload = wifiOnly,
                onWifiOnlyChanged = { viewModel.setNerWifiOnlyDownload(it) },
                onStartDownload = {
                    viewModel.startNerModelDownload(MODEL_DOWNLOAD_URL)
                },
                onDeleteModel = { viewModel.deleteNerModel() },
                activeProvider = when (selectedMode) {
                    NerProviderMode.AUTO -> "Auto-detect"
                    NerProviderMode.GEMINI_NANO -> "Gemini Nano"
                    NerProviderMode.QWEN3_LOCAL -> if (modelDownloaded) "Qwen3 Local" else "Not available"
                    NerProviderMode.CLAUDE_CLOUD -> "Cloud"
                    NerProviderMode.OFF -> null
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// Model download URL — update when hosting is finalized
private const val MODEL_DOWNLOAD_URL = "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/main/qwen3-0.6b-q4_k_m.gguf"

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
