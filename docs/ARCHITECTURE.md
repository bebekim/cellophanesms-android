# CellophoneSMS Android - Domain-Driven Design Architecture

## Overview

This architecture mirrors the cellophanemail backend's DDD patterns, adapted for Android with Kotlin, Jetpack Compose, and modern Android development practices.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │   Composables   │  │   ViewModels    │  │    UI State Models      │  │
│  │   (Screens)     │  │   (MVVM)        │  │    (UiState sealed)     │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │    Use Cases    │  │   Orchestrators │  │    Event Handlers       │  │
│  │  (Interactors)  │  │   (Workflows)   │  │  (SMS/MMS Receivers)    │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DOMAIN LAYER                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │    Entities     │  │  Value Objects  │  │   Domain Services       │  │
│  │  (Aggregates)   │  │  (Immutable)    │  │   (Business Rules)      │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
│  ┌─────────────────┐  ┌─────────────────┐                               │
│  │   Repository    │  │    Domain       │                               │
│  │   Interfaces    │  │    Events       │                               │
│  └─────────────────┘  └─────────────────┘                               │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │   Repository    │  │    API Client   │  │   Platform Services     │  │
│  │   Implementations│  │   (Retrofit)    │  │   (SMS, Notifications)  │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │   Local DB      │  │   Encryption    │  │   Preferences           │  │
│  │   (Room)        │  │   (Keystore)    │  │   (DataStore)           │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Directory Structure

```
app/src/main/java/com/cellophonemail/sms/
├── CellophoneSmsApplication.kt          # Hilt Application
│
├── domain/                              # DOMAIN LAYER (Pure Kotlin)
│   ├── model/                           # Entities & Value Objects
│   │   ├── message/
│   │   │   ├── Message.kt               # Aggregate Root
│   │   │   ├── MessageContent.kt        # Value Object (encrypted)
│   │   │   └── ProcessingState.kt       # Value Object (enum)
│   │   ├── thread/
│   │   │   ├── Thread.kt                # Aggregate Root
│   │   │   └── ThreadSummary.kt         # Value Object
│   │   ├── analysis/
│   │   │   ├── AnalysisResult.kt        # Value Object
│   │   │   ├── ToxicityClass.kt         # Value Object (enum)
│   │   │   ├── Horseman.kt              # Value Object (enum)
│   │   │   └── HorsemanDetection.kt     # Value Object
│   │   ├── contact/
│   │   │   └── Contact.kt               # Entity
│   │   └── user/
│   │       ├── User.kt                  # Aggregate Root
│   │       └── Subscription.kt          # Value Object
│   │
│   ├── repository/                      # Repository Interfaces
│   │   ├── MessageRepository.kt
│   │   ├── ThreadRepository.kt
│   │   ├── ContactRepository.kt
│   │   └── UserRepository.kt
│   │
│   ├── service/                         # Domain Services
│   │   ├── MessageFilterService.kt      # Filtering rules
│   │   ├── ToxicityClassifier.kt        # Classification logic
│   │   └── EncryptionService.kt         # Encryption contract
│   │
│   └── event/                           # Domain Events
│       ├── DomainEvent.kt               # Base event
│       ├── MessageReceived.kt
│       ├── MessageAnalyzed.kt
│       └── MessageFiltered.kt
│
├── application/                         # APPLICATION LAYER
│   ├── usecase/                         # Use Cases / Interactors
│   │   ├── message/
│   │   │   ├── ReceiveMessageUseCase.kt
│   │   │   ├── SendMessageUseCase.kt
│   │   │   ├── GetMessagesUseCase.kt
│   │   │   └── ViewOriginalUseCase.kt
│   │   ├── thread/
│   │   │   ├── GetThreadsUseCase.kt
│   │   │   ├── ArchiveThreadUseCase.kt
│   │   │   └── MarkThreadReadUseCase.kt
│   │   ├── analysis/
│   │   │   ├── AnalyzeMessageUseCase.kt
│   │   │   └── GetAnalysisResultUseCase.kt
│   │   └── auth/
│   │       ├── LoginUseCase.kt
│   │       └── RegisterUseCase.kt
│   │
│   ├── orchestrator/                    # Workflow Orchestrators
│   │   ├── MessageProcessingOrchestrator.kt
│   │   └── OnboardingOrchestrator.kt
│   │
│   └── dto/                             # Data Transfer Objects
│       ├── MessageDto.kt
│       └── AnalysisRequestDto.kt
│
├── infrastructure/                      # INFRASTRUCTURE LAYER
│   ├── persistence/                     # Local Database
│   │   ├── database/
│   │   │   └── CellophoneDatabase.kt    # Room Database
│   │   ├── entity/                      # Room Entities
│   │   │   ├── MessageEntity.kt
│   │   │   ├── ThreadEntity.kt
│   │   │   └── ContactEntity.kt
│   │   ├── dao/                         # Data Access Objects
│   │   │   ├── MessageDao.kt
│   │   │   ├── ThreadDao.kt
│   │   │   └── ContactDao.kt
│   │   ├── mapper/                      # Entity <-> Domain Mappers
│   │   │   ├── MessageMapper.kt
│   │   │   └── ThreadMapper.kt
│   │   └── repository/                  # Repository Implementations
│   │       ├── MessageRepositoryImpl.kt
│   │       ├── ThreadRepositoryImpl.kt
│   │       └── ContactRepositoryImpl.kt
│   │
│   ├── network/                         # Remote API
│   │   ├── api/
│   │   │   └── CellophoneMailApi.kt     # Retrofit Interface
│   │   ├── model/                       # API Models
│   │   │   ├── SmsAnalysisRequest.kt
│   │   │   ├── SmsAnalysisResponse.kt
│   │   │   └── AuthResponse.kt
│   │   ├── interceptor/
│   │   │   ├── AuthInterceptor.kt
│   │   │   └── ErrorInterceptor.kt
│   │   └── client/
│   │       └── ApiClient.kt             # Retrofit Setup
│   │
│   ├── platform/                        # Android Platform Services
│   │   ├── sms/
│   │   │   ├── SmsReceiver.kt           # BroadcastReceiver
│   │   │   ├── MmsReceiver.kt           # BroadcastReceiver
│   │   │   ├── SmsSender.kt             # SmsManager wrapper
│   │   │   └── SmsProviderAdapter.kt    # Content Provider access
│   │   ├── notification/
│   │   │   ├── NotificationService.kt
│   │   │   └── NotificationChannels.kt
│   │   └── contact/
│   │       └── ContactsProviderAdapter.kt
│   │
│   ├── security/                        # Security Services
│   │   ├── EncryptionServiceImpl.kt     # Android Keystore
│   │   ├── TokenManager.kt              # Secure token storage
│   │   └── BiometricService.kt          # Biometric auth
│   │
│   └── preferences/                     # DataStore
│       ├── UserPreferences.kt
│       └── AppSettings.kt
│
├── presentation/                        # PRESENTATION LAYER
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Typography.kt
│   │   ├── components/                  # Reusable Composables
│   │   │   ├── MessageBubble.kt
│   │   │   ├── FilteredMessageCard.kt
│   │   │   ├── ThreadCard.kt
│   │   │   ├── ContactAvatar.kt
│   │   │   └── ToxicityBadge.kt
│   │   ├── screen/                      # Screen Composables
│   │   │   ├── main/
│   │   │   │   ├── MainScreen.kt
│   │   │   │   └── MainViewModel.kt
│   │   │   ├── thread/
│   │   │   │   ├── ThreadScreen.kt
│   │   │   │   └── ThreadViewModel.kt
│   │   │   ├── compose/
│   │   │   │   ├── ComposeScreen.kt
│   │   │   │   └── ComposeViewModel.kt
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   └── onboarding/
│   │   │       ├── OnboardingScreen.kt
│   │   │       └── OnboardingViewModel.kt
│   │   └── navigation/
│   │       └── NavGraph.kt
│   │
│   ├── state/                           # UI State Models
│   │   ├── UiState.kt                   # Sealed class
│   │   ├── MainUiState.kt
│   │   └── ThreadUiState.kt
│   │
│   └── mapper/                          # Domain -> UI Mappers
│       ├── MessageUiMapper.kt
│       └── ThreadUiMapper.kt
│
├── di/                                  # Dependency Injection
│   ├── AppModule.kt                     # Application-wide deps
│   ├── DatabaseModule.kt                # Room database
│   ├── NetworkModule.kt                 # Retrofit, OkHttp
│   ├── RepositoryModule.kt              # Repository bindings
│   └── UseCaseModule.kt                 # Use case bindings
│
└── service/                             # Android Services
    ├── MessageProcessingService.kt      # Background processing
    └── QuickReplyService.kt             # Quick reply handler
```

---

## Bounded Contexts (Feature Modules)

Following cellophanemail's pattern, the domain is organized into bounded contexts:

### 1. Message Protection Context (Core Domain)

```kotlin
// domain/model/analysis/AnalysisResult.kt
data class AnalysisResult(
    val safe: Boolean,
    val threatLevel: ThreatLevel,
    val toxicityScore: Float,
    val horsemenDetected: List<HorsemanDetection>,
    val reasoning: String,
    val filteredSummary: String,
    val processingTimeMs: Long,
    val cached: Boolean = false
)

// domain/model/analysis/HorsemanDetection.kt
data class HorsemanDetection(
    val horseman: Horseman,
    val confidence: Float,
    val indicators: List<String>,
    val severity: Severity
)

// domain/model/analysis/Horseman.kt
enum class Horseman {
    CRITICISM,      // Attack on character/personality
    CONTEMPT,       // Disgust, disrespect, mockery
    DEFENSIVENESS,  // Counter-attacking, victimhood
    STONEWALLING    // Withdrawal, silent treatment
}
```

### 2. Messaging Context

```kotlin
// domain/model/message/Message.kt
data class Message(
    val id: MessageId,              // Value Object
    val threadId: ThreadId,
    val address: PhoneNumber,       // Value Object
    val timestamp: Instant,
    val direction: MessageDirection,
    val content: MessageContent,    // Value Object (handles encryption)
    val analysis: AnalysisResult?,
    val state: ProcessingState,
    val metadata: MessageMetadata
) {
    val isFiltered: Boolean
        get() = analysis?.safe == false

    val displayContent: String
        get() = if (isFiltered) analysis?.filteredSummary ?: "" else content.decrypted
}

// domain/model/message/MessageContent.kt
data class MessageContent private constructor(
    private val encrypted: ByteArray,
    private val encryptionService: EncryptionService
) {
    val decrypted: String
        get() = encryptionService.decrypt(encrypted)

    companion object {
        fun fromPlainText(text: String, encryptionService: EncryptionService): MessageContent {
            return MessageContent(encryptionService.encrypt(text), encryptionService)
        }

        fun fromEncrypted(encrypted: ByteArray, encryptionService: EncryptionService): MessageContent {
            return MessageContent(encrypted, encryptionService)
        }
    }
}
```

### 3. User & Authentication Context

```kotlin
// domain/model/user/User.kt
data class User(
    val id: UserId,
    val email: Email,
    val subscription: Subscription,
    val preferences: UserPreferences,
    val quotas: ApiQuotas
)

// domain/model/user/Subscription.kt
data class Subscription(
    val status: SubscriptionStatus,
    val plan: SubscriptionPlan,
    val expiresAt: Instant?
)

enum class SubscriptionStatus {
    ACTIVE, TRIALING, PAST_DUE, CANCELED, FREE
}
```

---

## Repository Pattern

Following cellophanemail's approach - interfaces in domain, implementations in infrastructure:

```kotlin
// domain/repository/MessageRepository.kt
interface MessageRepository {
    suspend fun getById(id: MessageId): Message?
    suspend fun getByThreadId(threadId: ThreadId): Flow<List<Message>>
    suspend fun insert(message: Message): MessageId
    suspend fun update(message: Message)
    suspend fun delete(id: MessageId)
    suspend fun markAsRead(id: MessageId)
    suspend fun getUnprocessed(): List<Message>
}

// infrastructure/persistence/repository/MessageRepositoryImpl.kt
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val messageMapper: MessageMapper,
    private val encryptionService: EncryptionService
) : MessageRepository {

    override suspend fun getById(id: MessageId): Message? {
        return messageDao.getById(id.value)?.let {
            messageMapper.toDomain(it, encryptionService)
        }
    }

    override suspend fun getByThreadId(threadId: ThreadId): Flow<List<Message>> {
        return messageDao.getByThreadId(threadId.value)
            .map { entities ->
                entities.map { messageMapper.toDomain(it, encryptionService) }
            }
    }

    override suspend fun insert(message: Message): MessageId {
        val entity = messageMapper.toEntity(message)
        messageDao.insert(entity)
        return message.id
    }

    // ... other implementations
}
```

---

## Use Case Pattern

Application layer orchestrates domain logic:

```kotlin
// application/usecase/message/ReceiveMessageUseCase.kt
class ReceiveMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val encryptionService: EncryptionService,
    private val eventBus: EventBus
) {
    suspend operator fun invoke(
        sender: String,
        body: String,
        timestamp: Long
    ): Result<Message> = runCatching {

        // 1. Create domain model with encrypted content
        val content = MessageContent.fromPlainText(body, encryptionService)
        val phoneNumber = PhoneNumber.parse(sender)
        val threadId = ThreadId.fromAddress(phoneNumber)

        val message = Message(
            id = MessageId.generate(),
            threadId = threadId,
            address = phoneNumber,
            timestamp = Instant.ofEpochMilli(timestamp),
            direction = MessageDirection.INCOMING,
            content = content,
            analysis = null,
            state = ProcessingState.PENDING,
            metadata = MessageMetadata.default()
        )

        // 2. Persist
        messageRepository.insert(message)

        // 3. Update or create thread
        threadRepository.upsertFromMessage(message)

        // 4. Emit domain event
        eventBus.emit(MessageReceived(message.id, message.threadId))

        message
    }
}

// application/usecase/analysis/AnalyzeMessageUseCase.kt
class AnalyzeMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val analysisApi: CellophoneMailApi,
    private val eventBus: EventBus
) {
    suspend operator fun invoke(messageId: MessageId): Result<AnalysisResult> = runCatching {

        val message = messageRepository.getById(messageId)
            ?: throw MessageNotFoundException(messageId)

        // 1. Update state to processing
        messageRepository.update(message.copy(state = ProcessingState.PROCESSING))

        // 2. Call API
        val response = analysisApi.analyzeSms(
            SmsAnalysisRequest(
                content = message.content.decrypted,
                sender = message.address.formatted,
                timestamp = message.timestamp.toEpochMilli()
            )
        )

        // 3. Map to domain model
        val result = AnalysisResult(
            safe = response.classification == "SAFE",
            threatLevel = ThreatLevel.valueOf(response.classification),
            toxicityScore = response.toxicityScore,
            horsemenDetected = response.horsemen.map { HorsemanDetection.fromApi(it) },
            reasoning = response.reasoning,
            filteredSummary = response.filteredSummary,
            processingTimeMs = System.currentTimeMillis() - message.timestamp.toEpochMilli(),
            cached = false
        )

        // 4. Update message with analysis
        val analyzedMessage = message.copy(
            analysis = result,
            state = if (result.safe) ProcessingState.SAFE else ProcessingState.FILTERED
        )
        messageRepository.update(analyzedMessage)

        // 5. Emit domain event
        eventBus.emit(MessageAnalyzed(messageId, result))

        result
    }
}
```

---

## Message Processing Pipeline

Mirrors cellophanemail's processor architecture:

```
SMS Received (BroadcastReceiver)
         │
         ▼
┌─────────────────────────┐
│  ReceiveMessageUseCase  │
│  - Encrypt content      │
│  - Create Message       │
│  - Store in Room        │
│  - Emit MessageReceived │
└─────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  MessageProcessingOrchestrator  │
│  - Listen for MessageReceived   │
│  - Coordinate analysis flow     │
└─────────────────────────────────┘
         │
         ▼
┌─────────────────────────┐
│  AnalyzeMessageUseCase  │
│  - Call Cloud API       │
│  - Four Horsemen check  │
│  - Toxicity scoring     │
└─────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  NotificationService            │
│  - Show filtered/safe content   │
│  - Color-coded by threat level  │
└─────────────────────────────────┘
```

```kotlin
// application/orchestrator/MessageProcessingOrchestrator.kt
class MessageProcessingOrchestrator @Inject constructor(
    private val analyzeMessageUseCase: AnalyzeMessageUseCase,
    private val notificationService: NotificationService,
    private val eventBus: EventBus,
    private val errorHandler: ErrorHandler,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    init {
        // Listen for incoming messages
        scope.launch {
            eventBus.events<MessageReceived>().collect { event ->
                processMessage(event.messageId)
            }
        }
    }

    private suspend fun processMessage(messageId: MessageId) {
        analyzeMessageUseCase(messageId)
            .onSuccess { result ->
                notificationService.showMessageNotification(messageId, result)
                eventBus.emit(MessageFiltered(messageId, result))
            }
            .onFailure { error ->
                errorHandler.handle(error)
                // Show original with warning on failure
                notificationService.showMessageNotification(
                    messageId,
                    AnalysisResult.error(),
                    showWarning = true
                )
            }
    }
}
```

---

## Factory Pattern (Provider Abstraction)

Following cellophanemail's email sender factory:

```kotlin
// infrastructure/platform/sms/SmsSenderFactory.kt
interface SmsSender {
    suspend fun send(address: PhoneNumber, content: String): SendResult
    suspend fun sendMultipart(address: PhoneNumber, parts: List<String>): SendResult
}

class SmsSenderFactory @Inject constructor(
    private val context: Context
) {
    private val senders = mapOf(
        "default" to { DefaultSmsSender(context) },
        "sim1" to { SimSlotSmsSender(context, slotIndex = 0) },
        "sim2" to { SimSlotSmsSender(context, slotIndex = 1) }
    )

    fun create(type: String = "default"): SmsSender {
        return senders[type]?.invoke()
            ?: throw IllegalArgumentException("Unknown SMS sender type: $type")
    }

    fun register(type: String, factory: () -> SmsSender) {
        senders[type] = factory
    }
}

// Infrastructure implementation
class DefaultSmsSender(
    private val context: Context
) : SmsSender {

    private val smsManager: SmsManager
        get() = context.getSystemService(SmsManager::class.java)

    override suspend fun send(address: PhoneNumber, content: String): SendResult {
        return try {
            if (content.length > 160) {
                sendMultipart(address, smsManager.divideMessage(content))
            } else {
                smsManager.sendTextMessage(
                    address.formatted,
                    null,
                    content,
                    null,
                    null
                )
                SendResult.Success
            }
        } catch (e: Exception) {
            SendResult.Failure(e.message ?: "Unknown error")
        }
    }
}
```

---

## Dependency Injection Modules

```kotlin
// di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEventBus(): EventBus = EventBusImpl()

    @Provides
    @Singleton
    fun provideEncryptionService(
        @ApplicationContext context: Context
    ): EncryptionService = KeystoreEncryptionService(context)
}

// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CellophoneDatabase {
        return Room.databaseBuilder(
            context,
            CellophoneDatabase::class.java,
            "cellophone_sms.db"
        ).build()
    }

    @Provides
    fun provideMessageDao(database: CellophoneDatabase): MessageDao {
        return database.messageDao()
    }
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    abstract fun bindThreadRepository(
        impl: ThreadRepositoryImpl
    ): ThreadRepository
}

// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideCellophoneMailApi(
        okHttpClient: OkHttpClient
    ): CellophoneMailApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CellophoneMailApi::class.java)
    }
}
```

---

## Domain Events

Following cellophanemail's event-driven approach:

```kotlin
// domain/event/DomainEvent.kt
sealed interface DomainEvent {
    val occurredAt: Instant
}

// domain/event/MessageReceived.kt
data class MessageReceived(
    val messageId: MessageId,
    val threadId: ThreadId,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

// domain/event/MessageAnalyzed.kt
data class MessageAnalyzed(
    val messageId: MessageId,
    val result: AnalysisResult,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

// domain/event/MessageFiltered.kt
data class MessageFiltered(
    val messageId: MessageId,
    val result: AnalysisResult,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent

// infrastructure/event/EventBus.kt
interface EventBus {
    suspend fun emit(event: DomainEvent)
    fun <T : DomainEvent> events(): Flow<T>
}

class EventBusImpl : EventBus {
    private val _events = MutableSharedFlow<DomainEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun emit(event: DomainEvent) {
        _events.emit(event)
    }

    override fun <T : DomainEvent> events(): Flow<T> {
        return _events.filterIsInstance()
    }
}
```

---

## ViewModel Pattern

ViewModels consume use cases and emit UI state:

```kotlin
// presentation/ui/screen/thread/ThreadViewModel.kt
@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markThreadReadUseCase: MarkThreadReadUseCase,
    private val viewOriginalUseCase: ViewOriginalUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId = ThreadId(savedStateHandle.get<String>("threadId")!!)

    private val _uiState = MutableStateFlow<ThreadUiState>(ThreadUiState.Loading)
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText: StateFlow<String> = _composingText.asStateFlow()

    init {
        loadMessages()
        markAsRead()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(threadId)
                .map { messages -> ThreadUiState.Success(messages.map { it.toUiModel() }) }
                .catch { e -> emit(ThreadUiState.Error(e.message ?: "Unknown error")) }
                .collect { _uiState.value = it }
        }
    }

    fun updateComposingText(text: String) {
        _composingText.value = text
    }

    fun sendMessage() {
        val text = _composingText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            sendMessageUseCase(threadId, text)
                .onSuccess { _composingText.value = "" }
                .onFailure { /* Show error */ }
        }
    }

    fun viewOriginal(messageId: MessageId) {
        viewModelScope.launch {
            viewOriginalUseCase(messageId)
                .onSuccess { original ->
                    // Emit event to show dialog with warning
                }
        }
    }
}

// presentation/state/ThreadUiState.kt
sealed interface ThreadUiState {
    data object Loading : ThreadUiState
    data class Success(val messages: List<MessageUiModel>) : ThreadUiState
    data class Error(val message: String) : ThreadUiState
}

// presentation/ui/screen/thread/MessageUiModel.kt
data class MessageUiModel(
    val id: String,
    val content: String,
    val timestamp: String,
    val isIncoming: Boolean,
    val isFiltered: Boolean,
    val toxicityClass: ToxicityClass?,
    val horsemenIcons: List<Int>,  // Resource IDs
    val canViewOriginal: Boolean
)
```

---

## Key Architecture Principles

### 1. **Dependency Rule**
- Domain layer has NO dependencies on other layers
- Application layer depends only on Domain
- Infrastructure and Presentation depend on Domain and Application

### 2. **Value Objects for Safety**
```kotlin
// Compile-time safety with value objects
data class MessageId(val value: String) {
    companion object {
        fun generate() = MessageId(UUID.randomUUID().toString())
    }
}

data class PhoneNumber private constructor(val value: String) {
    val formatted: String get() = /* format logic */

    companion object {
        fun parse(raw: String): PhoneNumber {
            // Validation and normalization
            return PhoneNumber(normalized)
        }
    }
}
```

### 3. **Encryption at Rest**
- Original message content ALWAYS encrypted using MessageContent value object
- Decryption only happens when explicitly accessed
- Keys stored in Android Keystore

### 4. **Ethical Transparency**
- Filtered messages NEVER impersonate sender
- Clear visual distinction for filtered content
- Original always accessible with explicit confirmation

### 5. **Event-Driven Processing**
- Domain events decouple components
- Enables async processing pipeline
- Supports future features (analytics, sync)

---

## Testing Strategy

```kotlin
// Test doubles for domain layer
class FakeMessageRepository : MessageRepository {
    private val messages = mutableMapOf<MessageId, Message>()

    override suspend fun getById(id: MessageId) = messages[id]
    override suspend fun insert(message: Message): MessageId {
        messages[message.id] = message
        return message.id
    }
    // ...
}

// Use case tests
class AnalyzeMessageUseCaseTest {

    private val fakeRepository = FakeMessageRepository()
    private val fakeApi = FakeCellophoneMailApi()
    private val fakeEventBus = FakeEventBus()

    private val useCase = AnalyzeMessageUseCase(
        messageRepository = fakeRepository,
        analysisApi = fakeApi,
        eventBus = fakeEventBus
    )

    @Test
    fun `analyze toxic message returns filtered result`() = runTest {
        // Given
        val message = createTestMessage(content = "toxic content")
        fakeRepository.insert(message)
        fakeApi.setResponse(toxicAnalysisResponse())

        // When
        val result = useCase(message.id)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().safe)
        assertEquals(ProcessingState.FILTERED, fakeRepository.getById(message.id)?.state)
    }
}
```

---

## Summary

This DDD architecture:

1. **Mirrors cellophanemail patterns** - Same bounded contexts, value objects, and service patterns
2. **Clean separation** - Domain logic isolated from Android framework
3. **Testable** - Use cases and domain logic easily unit tested
4. **Extensible** - Factory patterns for future providers
5. **Secure** - Encryption built into domain model via value objects
6. **Event-driven** - Loose coupling between processing stages
