---
date: 2026-01-07T00:00:00+0000
researcher: user
topic: "CellophoneSMS Android App Design Document"
tags: [prd, android, sms, design, architecture, ui, four-horsemen]
status: draft
last_updated: 2026-01-07
---

# CellophoneSMS - Android App Design Document

## Executive Summary

**Mission**: Safety without silence for professionals who must stay reachable

**Target User**: AOD (Alcohol and Other Drugs) social workers who receive threatening SMS from clients but cannot block communication due to professional obligations.

**Core Value**: Filter toxic SMS content while preserving essential facts, allowing professionals to stay informed without psychological harm.

## Product Flow

```
1. Toxic SMS arrives → Hidden from user immediately
2. [Future: Local LLM check] → Falls through if empty
3. Cloud API Analysis → Four Horsemen classification
4. Filtered Summary → "Facts you need to know"
5. Display with clear "Filtered" tag → Cannot impersonate sender
6. Original archived securely → Accessible if needed with warning
7. User replies → Sent directly to sender, unfiltered
8. Thread continues → Normal SMS conversation
```

## Technical Architecture

### Android Requirements

- **Minimum SDK**: 19 (Android 4.4 KitKat)
- **Target SDK**: 34 (Android 14)
- **Must implement**: All 4 mandatory components for default SMS handler status
- **Google Play**: Full-featured messaging app (not background-only)

### Backend Integration

- **Base URL**: https://api.cellophonemail.com
- **Authentication**: Bearer token
- **Primary Endpoint**: POST /api/v1/sms/analyze
- **Framework**: Litestar (existing CellophoneMail backend)

---

## 1. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.cellophonemail.sms">

    <!-- SMS Permissions (Dangerous - require runtime) -->
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
    <uses-permission android:name="android.permission.READ_SMS"/>
    <uses-permission android:name="android.permission.WRITE_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_MMS"/>
    <uses-permission android:name="android.permission.RECEIVE_WAP_PUSH"/>

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

    <!-- Contacts -->
    <uses-permission android:name="android.permission.READ_CONTACTS"/>

    <!-- Notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

    <!-- Telephony Feature -->
    <uses-feature
        android:name="android.hardware.telephony"
        android:required="true"/>

    <application
        android:name=".CellophoneSmsApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.CellophoneSMS"
        android:usesCleartextTraffic="false">

        <!-- MANDATORY COMPONENT 1: SMS Receiver -->
        <receiver
            android:name=".receivers.SmsReceiver"
            android:permission="android.permission.BROADCAST_SMS"
            android:exported="true">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_DELIVER"/>
            </intent-filter>
        </receiver>

        <!-- MANDATORY COMPONENT 2: MMS Receiver -->
        <receiver
            android:name=".receivers.MmsReceiver"
            android:permission="android.permission.BROADCAST_WAP_PUSH"
            android:exported="true">
            <intent-filter>
                <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER"/>
                <data android:mimeType="application/vnd.wap.mms-message"/>
            </intent-filter>
        </receiver>

        <!-- MANDATORY COMPONENT 3: Compose Activity -->
        <activity
            android:name=".ui.compose.ComposeActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.SEND"/>
                <action android:name="android.intent.action.SENDTO"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data android:scheme="sms"/>
                <data android:scheme="smsto"/>
                <data android:scheme="mms"/>
                <data android:scheme="mmsto"/>
            </intent-filter>
        </activity>

        <!-- MANDATORY COMPONENT 4: Quick Reply Service -->
        <service
            android:name=".services.QuickReplyService"
            android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.RESPOND_VIA_MESSAGE"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <data android:scheme="sms"/>
                <data android:scheme="smsto"/>
            </intent-filter>
        </service>

        <!-- Main Activity -->
        <activity
            android:name=".ui.main.MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- Thread Activity -->
        <activity
            android:name=".ui.thread.ThreadActivity"
            android:exported="false"
            android:parentActivityName=".ui.main.MainActivity"
            android:windowSoftInputMode="adjustResize"/>

        <!-- Settings Activity -->
        <activity
            android:name=".ui.settings.SettingsActivity"
            android:exported="false"
            android:parentActivityName=".ui.main.MainActivity"/>

        <!-- Onboarding Activity -->
        <activity
            android:name=".ui.onboarding.OnboardingActivity"
            android:exported="false"
            android:theme="@style/Theme.CellophoneSMS.Onboarding"/>

        <!-- Background Services -->
        <service
            android:name=".services.MessageProcessingService"
            android:exported="false"/>

    </application>

</manifest>
```

---

## 2. Data Models

### Database Schema (Room)

```kotlin
// data/local/entity/Message.kt
package com.cellophonemail.sms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // SMS Metadata
    val threadId: String,
    val address: String, // Phone number
    val timestamp: Long,
    val isIncoming: Boolean,

    // Content (encrypted at rest)
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val originalContent: ByteArray, // Encrypted original
    val filteredContent: String?, // Summary/facts only

    // Analysis Results
    val isFiltered: Boolean = false,
    val toxicityScore: Float? = null,
    val classification: String? = null, // SAFE, WARNING, HARMFUL, ABUSIVE
    val horsemenDetected: String? = null, // JSON array
    val analysisReasoning: String? = null,

    // Status
    val processingState: String = "PENDING", // PENDING, PROCESSING, FILTERED, SAFE, ERROR
    val isSent: Boolean = true,
    val isRead: Boolean = false,

    // Thread Management
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,

    // Sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity

        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id.hashCode()
}

// data/local/entity/Thread.kt
@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey
    val threadId: String,

    val address: String,
    val contactName: String?,
    val contactPhotoUri: String?,

    val lastMessageId: String?,
    val lastMessageTime: Long,
    val lastMessagePreview: String,

    val unreadCount: Int = 0,
    val messageCount: Int = 0,

    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,

    val toxicityLevel: String = "SAFE", // SAFE, WARNING, HARMFUL, ABUSIVE

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// data/local/entity/Contact.kt
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val phoneNumber: String,
    val displayName: String?,
    val photoUri: String?,

    val isBlocked: Boolean = false,
    val notes: String?,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Domain Models

```kotlin
// domain/model/Message.kt
package com.cellophonemail.sms.domain.model

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val address: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val originalContent: String, // Decrypted for use
    val filteredContent: String?,
    val isFiltered: Boolean,
    val toxicityScore: Float?,
    val classification: ToxicityClass?,
    val horsemen: List<Horseman>,
    val reasoning: String?,
    val processingState: ProcessingState,
    val isSent: Boolean,
    val isRead: Boolean,
    val isArchived: Boolean
)

enum class ProcessingState {
    PENDING,      // Waiting for analysis
    PROCESSING,   // Sent to API
    FILTERED,     // Analysis complete, toxic
    SAFE,         // Analysis complete, clean
    ERROR         // API failed
}

enum class ToxicityClass {
    SAFE,
    WARNING,
    HARMFUL,
    ABUSIVE;

    fun getColor(): androidx.compose.ui.graphics.Color {
        return when (this) {
            SAFE -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
            WARNING -> androidx.compose.ui.graphics.Color(0xFFFFC107)
            HARMFUL -> androidx.compose.ui.graphics.Color(0xFFFF9800)
            ABUSIVE -> androidx.compose.ui.graphics.Color(0xFFF44336)
        }
    }
}

enum class Horseman {
    CRITICISM,
    CONTEMPT,
    DEFENSIVENESS,
    STONEWALLING;

    companion object {
        fun fromString(value: String): Horseman? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

// domain/model/Thread.kt
data class Thread(
    val threadId: String,
    val address: String,
    val contactName: String?,
    val contactPhotoUri: String?,
    val lastMessageTime: Long,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val messageCount: Int,
    val isArchived: Boolean,
    val toxicityLevel: ToxicityClass
)
```

---

## 3. Network Layer

### API Client

```kotlin
// data/remote/api/CellophoneMailApi.kt
package com.cellophonemail.sms.data.remote.api

import retrofit2.http.*

interface CellophoneMailApi {

    @POST("api/v1/sms/analyze")
    suspend fun analyzeSms(
        @Body request: SmsAnalysisRequest
    ): SmsAnalysisResponse

    @GET("api/v1/user/profile")
    suspend fun getUserProfile(): UserProfile

    @POST("api/v1/user/register")
    suspend fun registerUser(
        @Body request: UserRegistration
    ): AuthResponse
}

// data/remote/model/SmsAnalysisRequest.kt
data class SmsAnalysisRequest(
    val content: String,
    val sender: String,
    val timestamp: Long,
    val deviceId: String?
)

// data/remote/model/SmsAnalysisResponse.kt
data class SmsAnalysisResponse(
    val classification: String, // SAFE, WARNING, HARMFUL, ABUSIVE
    val toxicityScore: Float,
    val horsemen: List<String>,
    val reasoning: String,
    val filteredSummary: String,
    val specificExamples: List<String>
)

// data/remote/model/UserProfile.kt
data class UserProfile(
    val id: String,
    val email: String,
    val subscriptionStatus: String,
    val apiQuota: ApiQuota
)

data class ApiQuota(
    val used: Int,
    val limit: Int,
    val resetDate: Long
)
```

### Retrofit Setup

```kotlin
// data/remote/ApiClient.kt
package com.cellophonemail.sms.data.remote

import com.cellophonemail.sms.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.cellophonemail.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val token = TokenManager.getToken() // Implement token storage

        val request = original.newBuilder()
            .apply {
                if (token != null) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "CellophoneSMS-Android/${BuildConfig.VERSION_NAME}")
            .build()

        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CellophoneMailApi = retrofit.create(CellophoneMailApi::class.java)
}
```

---

## 4. Core Components

### SMS Receiver

```kotlin
// receivers/SmsReceiver.kt
package com.cellophonemail.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.cellophonemail.sms.data.repository.MessageRepository
import com.cellophonemail.sms.domain.model.Message
import com.cellophonemail.sms.services.MessageProcessingService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (smsMessage in messages) {
                try {
                    val sender = smsMessage.originatingAddress ?: "Unknown"
                    val body = smsMessage.messageBody ?: ""
                    val timestamp = smsMessage.timestampMillis

                    Log.d("SmsReceiver", "Received SMS from $sender at $timestamp")

                    // Create message entity
                    val message = Message(
                        threadId = generateThreadId(sender),
                        address = sender,
                        timestamp = timestamp,
                        isIncoming = true,
                        originalContent = body,
                        filteredContent = null,
                        isFiltered = false,
                        toxicityScore = null,
                        classification = null,
                        horsemen = emptyList(),
                        reasoning = null,
                        processingState = ProcessingState.PENDING,
                        isSent = true,
                        isRead = false,
                        isArchived = false
                    )

                    // Store immediately
                    messageRepository.insertMessage(message)

                    // Start processing service
                    MessageProcessingService.startProcessing(context, message.id)

                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                }
            }
        }
    }

    private fun generateThreadId(address: String): String {
        // Normalize phone number and generate consistent thread ID
        return address.replace(Regex("[^0-9]"), "").takeLast(10)
    }
}
```

### MMS Receiver

```kotlin
// receivers/MmsReceiver.kt
package com.cellophonemail.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            Log.d("MmsReceiver", "MMS received")

            // TODO: Implement MMS handling in Phase 2
            // For now, just acknowledge receipt
        }
    }
}
```

### Message Processing Service

```kotlin
// services/MessageProcessingService.kt
package com.cellophonemail.sms.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cellophonemail.sms.R
import com.cellophonemail.sms.data.remote.ApiClient
import com.cellophonemail.sms.data.repository.MessageRepository
import com.cellophonemail.sms.domain.model.ProcessingState
import com.cellophonemail.sms.domain.model.ToxicityClass
import com.cellophonemail.sms.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MessageProcessingService : Service() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val EXTRA_MESSAGE_ID = "message_id"
        private const val NOTIFICATION_CHANNEL_ID = "message_processing"

        fun startProcessing(context: Context, messageId: String) {
            val intent = Intent(context, MessageProcessingService::class.java).apply {
                putExtra(EXTRA_MESSAGE_ID, messageId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val messageId = intent?.getStringExtra(EXTRA_MESSAGE_ID)

        if (messageId != null) {
            serviceScope.launch {
                processMessage(messageId)
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun processMessage(messageId: String) {
        try {
            val message = messageRepository.getMessageById(messageId) ?: return

            // Update state to processing
            messageRepository.updateMessage(
                message.copy(processingState = ProcessingState.PROCESSING)
            )

            // TODO: Step 1 - Local LLM check (Phase 3)
            // For now, skip directly to cloud

            // Step 2: Cloud API Analysis
            val response = ApiClient.api.analyzeSms(
                SmsAnalysisRequest(
                    content = message.originalContent,
                    sender = message.address,
                    timestamp = message.timestamp,
                    deviceId = getDeviceId()
                )
            )

            // Step 3: Parse and store results
            val classification = ToxicityClass.valueOf(response.classification)
            val horsemen = response.horsemen.mapNotNull {
                Horseman.fromString(it)
            }

            val updatedMessage = message.copy(
                filteredContent = response.filteredSummary,
                classification = classification,
                toxicityScore = response.toxicityScore,
                horsemen = horsemen,
                reasoning = response.reasoning,
                isFiltered = classification != ToxicityClass.SAFE,
                processingState = when (classification) {
                    ToxicityClass.SAFE -> ProcessingState.SAFE
                    else -> ProcessingState.FILTERED
                }
            )

            messageRepository.updateMessage(updatedMessage)

            // Step 4: Show notification
            notificationHelper.showMessageNotification(updatedMessage)

        } catch (e: Exception) {
            Log.e("MessageProcessing", "Failed to process message $messageId", e)

            // Mark as error and show original
            messageRepository.getMessageById(messageId)?.let { message ->
                messageRepository.updateMessage(
                    message.copy(
                        processingState = ProcessingState.ERROR,
                        isFiltered = false
                    )
                )

                // Show notification with error state
                notificationHelper.showMessageNotification(
                    message.copy(processingState = ProcessingState.ERROR),
                    isError = true
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Message Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Processing incoming messages"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getDeviceId(): String {
        // Implement secure device ID generation
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}
```

### Quick Reply Service

```kotlin
// services/QuickReplyService.kt
package com.cellophonemail.sms.services

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log

class QuickReplyService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "android.intent.action.RESPOND_VIA_MESSAGE") {
            val extras = intent.extras
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)
            val recipients = extras?.getString("android.intent.extra.ADDRESS")

            if (message != null && recipients != null) {
                sendQuickReply(recipients, message)
            }

            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendQuickReply(address: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()

            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
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
                    message,
                    null,
                    null
                )
            }

            Log.d("QuickReplyService", "Quick reply sent to $address")

        } catch (e: Exception) {
            Log.e("QuickReplyService", "Failed to send quick reply", e)
        }
    }
}
```

---

## 5. User Interface

### Main Activity (Feed Screen)

```kotlin
// ui/main/MainActivity.kt
package com.cellophonemail.sms.ui.main

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cellophonemail.sms.ui.theme.CellophoneSMSTheme
import com.cellophonemail.sms.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results
        if (permissions.values.all { it }) {
            checkDefaultSmsApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is first launch
        if (isFirstLaunch()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        checkPermissions()

        setContent {
            CellophoneSMSTheme {
                MainScreen()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )

        requestPermissions.launch(permissions)
    }

    private fun checkDefaultSmsApp() {
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
        if (defaultSmsPackage != packageName) {
            // Show dialog to set as default
            showSetDefaultDialog()
        }
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return !prefs.getBoolean("onboarding_complete", false)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val threads by viewModel.threads.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Compose new message */ }
            ) {
                Icon(Icons.Default.Edit, "New Message")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Success -> {
                ThreadListScreen(
                    threads = threads,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is UiState.Error -> ErrorScreen((uiState as UiState.Error).message)
        }
    }
}

@Composable
fun ThreadListScreen(
    threads: List<Thread>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(threads) { thread ->
            ThreadCard(
                thread = thread,
                onClick = { /* Navigate to thread */ }
            )
        }
    }
}

@Composable
fun ThreadCard(
    thread: Thread,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Avatar
            ContactAvatar(
                name = thread.contactName ?: thread.address,
                toxicityLevel = thread.toxicityLevel
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = thread.contactName ?: thread.address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (thread.unreadCount > 0)
                        FontWeight.Bold else FontWeight.Normal
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (thread.toxicityLevel != ToxicityClass.SAFE) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Filtered",
                            tint = thread.toxicityLevel.getColor(),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = thread.lastMessagePreview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (thread.unreadCount > 0)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timestamp and Badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatTimestamp(thread.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = thread.unreadCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
```

### Thread Activity

```kotlin
// ui/thread/ThreadActivity.kt
package com.cellophonemail.sms.ui.thread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cellophonemail.sms.domain.model.Message
import com.cellophonemail.sms.domain.model.ToxicityClass
import com.cellophonemail.sms.ui.theme.CellophoneSMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThreadActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val threadId = intent.getStringExtra(EXTRA_THREAD_ID) ?: ""
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""

        setContent {
            CellophoneSMSTheme {
                ThreadScreen(
                    threadId = threadId,
                    address = address
                )
            }
        }
    }
}

@Composable
fun ThreadScreen(
    threadId: String,
    address: String,
    viewModel: ThreadViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val composingText by viewModel.composingText.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(address) },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message)
                }
            }

            // Compose Bar
            ComposeBar(
                text = composingText,
                onTextChange = { viewModel.updateComposingText(it) },
                onSend = { viewModel.sendMessage(threadId, address) }
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isOwn = !message.isIncoming

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwn) 16.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isOwn -> MaterialTheme.colorScheme.primaryContainer
                    message.isFiltered -> getFilteredBackgroundColor(message.classification)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Filtered Header
                if (message.isFiltered) {
                    FilteredMessageHeader(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content
                Text(
                    text = if (message.isFiltered && message.filteredContent != null)
                        message.filteredContent
                    else
                        message.originalContent,
                    style = MaterialTheme.typography.bodyMedium
                )

                // View Original Button
                if (message.isFiltered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { /* Show view original dialog */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View Original")
                    }
                }

                // Timestamp
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun FilteredMessageHeader(message: Message) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Filtered",
                tint = message.classification?.getColor()
                    ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "CellophoneSMS Filtered",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = message.classification?.getColor()
                        ?: MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "From: ${message.address}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
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
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

---

## 6. Backend Integration Required

### New API Endpoint (Python/Litestar)

Add this to your existing CellophoneMail backend:

```python
# src/cellophanemail/routes/sms.py

from litestar import post, Controller, Response
from litestar.status_codes import HTTP_200_OK, HTTP_400_BAD_REQUEST
from pydantic import BaseModel
from typing import List, Optional

from ..core.content_analyzer import ContentAnalyzer


class SmsAnalysisRequest(BaseModel):
    """SMS analysis request from Android app."""
    content: str
    sender: str
    timestamp: int
    device_id: Optional[str] = None


class SmsAnalysisResponse(BaseModel):
    """SMS analysis response to Android app."""
    classification: str  # SAFE, WARNING, HARMFUL, ABUSIVE
    toxicity_score: float
    horsemen: List[str]
    reasoning: str
    filtered_summary: str
    specific_examples: List[str]


class SmsController(Controller):
    """SMS analysis endpoints for mobile app."""

    path = "/api/v1/sms"

    @post("/analyze")
    async def analyze_sms(
        self,
        data: SmsAnalysisRequest
    ) -> Response:
        """
        Analyze SMS content using Four Horsemen framework.
        Returns filtered summary with classification.
        """
        try:
            # Reuse existing ContentAnalyzer
            analyzer = ContentAnalyzer()
            analysis = analyzer.analyze_content(
                content=data.content,
                sender=data.sender
            )

            # Generate filtered summary
            filtered_summary = self._generate_summary(
                content=data.content,
                analysis=analysis
            )

            # Calculate toxicity score
            toxicity_score = self._calculate_toxicity(analysis)

            response = SmsAnalysisResponse(
                classification=analysis['classification'],
                toxicity_score=toxicity_score,
                horsemen=analysis['horsemen_detected'],
                reasoning=analysis['reasoning'],
                filtered_summary=filtered_summary,
                specific_examples=analysis.get('specific_examples', [])
            )

            return Response(
                content=response.dict(),
                status_code=HTTP_200_OK
            )

        except Exception as e:
            logger.error(f"SMS analysis failed: {e}", exc_info=True)
            return Response(
                content={"error": "Analysis failed", "detail": str(e)},
                status_code=HTTP_400_BAD_REQUEST
            )

    def _generate_summary(self, content: str, analysis: dict) -> str:
        """
        Extract factual information from toxic message.
        Remove emotional content, keep only essential facts.
        """
        classification = analysis['classification']

        if classification == 'SAFE':
            return content

        # Use LLM to extract facts
        from anthropic import Anthropic
        client = Anthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

        prompt = f"""Extract only factual, actionable information from this message.
Remove all toxic, emotional, or abusive content.
Provide a neutral, professional summary of what the recipient needs to know.

Original message:
{content}

Toxic patterns detected: {', '.join(analysis['horsemen_detected'])}

Provide ONLY the factual summary, nothing else:"""

        response = client.messages.create(
            model="claude-3-5-sonnet-20241022",
            max_tokens=200,
            messages=[{"role": "user", "content": prompt}]
        )

        summary = response.content[0].text.strip()
        return summary

    def _calculate_toxicity(self, analysis: dict) -> float:
        """Calculate 0-1 toxicity score from analysis."""
        classification = analysis['classification']
        horsemen_count = len(analysis['horsemen_detected'])

        base_scores = {
            'SAFE': 0.0,
            'WARNING': 0.3,
            'HARMFUL': 0.7,
            'ABUSIVE': 1.0
        }

        base = base_scores.get(classification, 0.0)

        # Adjust for horsemen count
        if horsemen_count > 0:
            base = max(base, 0.5 + (horsemen_count * 0.1))

        return min(base, 1.0)


# Register in app.py
from cellophanemail.routes import sms

app = create_app()
app.register(sms.SmsController)
```

---

## 7. Implementation Phases

### Phase 1: MVP (Must Have)
**Goal**: Functional SMS app with cloud filtering

- All 4 mandatory Android components
- Default SMS handler functionality
- Receive SMS via broadcast receiver
- Send SMS via SmsManager
- Cloud API integration (Four Horsemen analysis)
- Local database (Room)
- Display filtered content with clear attribution
- Store encrypted original
- Thread view UI
- Compose/reply functionality
- Basic notifications

### Phase 2: Enhanced Experience
**Goal**: Complete messaging app experience

- Feed screen (Facebook-like)
- Archive threads
- View original (with warning dialog)
- Contact integration
- Search functionality
- Settings screen
- User preferences
- Improved notifications
- MMS support

### Phase 3: Advanced Features
**Goal**: Optimize performance and add intelligence

- Local LLM integration (on-device)
- Offline mode
- Message backup/export
- Custom filtering rules
- Statistics dashboard
- Multi-device sync
- Batch operations

---

## 8. Key Design Principles

### 1. Ethical Transparency
**Never impersonate the original sender**

```kotlin
// WRONG
MessageBubble(
    sender = "John Doe",
    content = "Meeting at 2pm" // Filtered summary
)

// CORRECT
FilteredMessageCard(
    filteredBy = "CellophoneSMS",
    originalSender = "John Doe",
    filteredContent = "Meeting at 2pm",
    classification = ToxicityClass.HARMFUL,
    canViewOriginal = true
)
```

### 2. User Safety First
- Original messages encrypted at rest
- View original requires explicit confirmation
- Clear visual distinction for filtered content
- Processing failures default to showing original (with warning)

### 3. Professional Focus
- Mission: "Safety without silence"
- Target: Professionals who cannot block communication
- Value: Psychological protection without information loss

### 4. David Lieb's Principles
- **User-centric**: Built for AOD social workers' real needs
- **Clear mission**: "Home for safe professional communication"
- **Solve real pain**: Protect from psychological harm
- **Talk to users**: Validate with 5-10 social workers before building

---

## 9. Testing Strategy

### User Acceptance Testing
**Critical**: Test with 5-10 AOD social workers

Questions to ask:
1. Would you actually use this?
2. What information do you NEED from toxic messages?
3. How do you want to reply?
4. What would make you trust the filtering?
5. What's missing?

### Technical Testing
- SMS send/receive on real devices
- API integration under poor network
- Encryption/decryption performance
- Notification behavior
- Default SMS handler switching
- Thread management accuracy

### Edge Cases
- Very long messages (>160 chars)
- Rapid successive messages
- No network connectivity
- API failures
- Multiple conversations
- Contact changes

---

## 10. Security Considerations

### Data Protection
```kotlin
// Encryption at rest (example using Android Keystore)
object MessageEncryption {

    private const val KEY_ALIAS = "cellophone_message_key"

    fun encrypt(plaintext: String): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray())

        // Prepend IV to encrypted data
        return iv + encrypted
    }

    fun decrypt(ciphertext: ByteArray): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        // Extract IV and encrypted data
        val iv = ciphertext.sliceArray(0 until 12)
        val encrypted = ciphertext.sliceArray(12 until ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        return String(cipher.doFinal(encrypted))
    }
}
```

### API Security
- Bearer token authentication
- HTTPS only
- Certificate pinning (production)
- Rate limiting on backend
- Device ID for abuse prevention

### Privacy
- No cloud storage of original messages
- Encrypted local storage
- User consent for analysis
- Clear data retention policy
- GDPR compliance (if applicable)

---

## 11. Monitoring & Analytics

### Key Metrics to Track
```kotlin
// Analytics events (Firebase/Mixpanel)
sealed class AnalyticsEvent {
    object MessageReceived : AnalyticsEvent()
    data class MessageFiltered(val classification: ToxicityClass) : AnalyticsEvent()
    object MessageSent : AnalyticsEvent()
    object OriginalViewed : AnalyticsEvent()
    data class ApiError(val errorType: String) : AnalyticsEvent()
    object ThreadArchived : AnalyticsEvent()
}

// Usage tracking
data class UsageStats(
    val totalMessages: Int,
    val filteredMessages: Int,
    val filterRate: Float,
    val avgProcessingTime: Long,
    val apiSuccessRate: Float,
    val userRetentionDays: Int
)
```

### Success Criteria
- 70%+ of filtered messages viewed as helpful
- <5% false positive rate
- API response time <2 seconds
- User retention >60% after 1 week
- 4.0+ star rating on Play Store

---

## 12. Dependencies (build.gradle.kts)

```kotlin
// app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.cellophonemail.sms"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cellophonemail.sms"
        minSdk = 19
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Work Manager (background tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 13. Next Steps

### Immediate Actions
1. **User Research**: Interview 5-10 AOD social workers
   - Show mockups
   - Ask validation questions
   - Observe reactions to filtered content
   - Understand reply workflows

2. **Technical Validation**
   - Set up Android Studio project
   - Implement basic SMS receiver
   - Test becoming default handler
   - Verify API integration

3. **Backend Preparation**
   - Add `/api/v1/sms/analyze` endpoint
   - Implement fact extraction prompt
   - Test response times
   - Set up API authentication

### Week 1-2: Foundation
- Project setup
- All 4 mandatory components
- Basic SMS send/receive
- Database schema

### Week 3-4: Core Features
- API integration
- Message processing pipeline
- Thread view UI
- Notifications

### Week 5-6: Polish
- Feed screen
- Error handling
- Testing with real devices
- User feedback incorporation

---

## 14. Critical Success Factors

### Must Get Right
1. **Ethical transparency**: Never impersonate sender
2. **Fast processing**: <2s API response
3. **Reliable filtering**: <5% false positives
4. **User trust**: Clear "why filtered"
5. **Professional focus**: Keep mission clear

### Failure Risks
1. **Too many false positives**: Users lose trust
2. **Slow processing**: Bad user experience
3. **Unclear filtering**: Users confused
4. **Generic messaging app**: Loses focus
5. **No user validation**: Build wrong thing

### Mitigation
- Test with real users early
- Monitor filtering accuracy
- Clear visual feedback
- Stay focused on AOD worker needs
- Iterate based on feedback

---

## Contact & Support

**Developer**: [Your Name]
**Email**: [your-email]
**Backend API**: https://api.cellophonemail.com
**Documentation**: https://docs.cellophonemail.com

**User Feedback**: feedback@cellophonemail.com
**Support**: support@cellophonemail.com

---

**Remember David Lieb's wisdom**:
*"Focus on the things that matter. Who are you building this for? Real people, with names you know. Talk to them."*

Before writing more code, show this design to 5 AOD social workers and ask: **"Would you actually use this?"**
