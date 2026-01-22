package com.cellophanemail.sms.domain.model

/**
 * Represents the current state of required permissions.
 */
sealed class PermissionState {
    /** All required permissions granted */
    data object Granted : PermissionState()

    /** Some permissions missing */
    data class Missing(
        val missingPermissions: List<String>,
        val canRequestAgain: Boolean = true
    ) : PermissionState()

    /** User permanently denied - must go to settings */
    data class PermanentlyDenied(
        val deniedPermissions: List<String>
    ) : PermissionState()

    companion object {
        const val READ_SMS = "android.permission.READ_SMS"
        const val RECEIVE_SMS = "android.permission.RECEIVE_SMS"
        const val SEND_SMS = "android.permission.SEND_SMS"
        const val READ_CONTACTS = "android.permission.READ_CONTACTS"
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

        val REQUIRED_PERMISSIONS = listOf(READ_SMS, RECEIVE_SMS, SEND_SMS)
        val OPTIONAL_PERMISSIONS = listOf(READ_CONTACTS, POST_NOTIFICATIONS)
    }
}

/**
 * Represents the current state of SMS scanning/analysis.
 */
sealed class ScanState {
    /** No scan has been performed yet */
    data object NotStarted : ScanState()

    /** Initial scan is in progress */
    data class Scanning(
        val phase: ScanPhase,
        val progress: Float,
        val messagesScanned: Int,
        val totalMessages: Int,
        val currentBatch: Int = 0,
        val totalBatches: Int = 0
    ) : ScanState()

    /** Scan completed successfully */
    data class Completed(
        val totalMessagesScanned: Int,
        val totalMessagesAnalyzed: Int,
        val completedAt: Long
    ) : ScanState()

    /** Scan paused (can be resumed) */
    data class Paused(
        val reason: PauseReason,
        val progress: Float,
        val messagesScanned: Int
    ) : ScanState()

    /** Scan failed */
    data class Failed(
        val error: ScanError,
        val canRetry: Boolean = true
    ) : ScanState()
}

enum class ScanPhase {
    /** Reading messages from SMS provider */
    READING_PROVIDER,
    /** Storing messages in local database */
    STORING_LOCAL,
    /** Submitting batch to backend for analysis */
    ANALYZING,
    /** Updating sender summaries and dashboard metrics */
    AGGREGATING
}

enum class PauseReason {
    /** User manually paused */
    USER_REQUESTED,
    /** No network available */
    NO_NETWORK,
    /** Backend unavailable */
    BACKEND_UNAVAILABLE,
    /** App went to background */
    APP_BACKGROUNDED
}

sealed class ScanError {
    data object PermissionDenied : ScanError()
    data object NoMessages : ScanError()
    data class NetworkError(val message: String) : ScanError()
    data class BackendError(val code: Int, val message: String) : ScanError()
    data class UnknownError(val message: String) : ScanError()
}

/**
 * Represents the current state of backend connectivity.
 */
sealed class BackendState {
    /** Backend is available and responding */
    data object Available : BackendState()

    /** Checking backend availability */
    data object Checking : BackendState()

    /** Backend is unavailable */
    data class Unavailable(
        val reason: UnavailableReason,
        val lastAttempt: Long,
        val nextRetryAt: Long,
        val retryCount: Int
    ) : BackendState()

    /** Rate limited by backend */
    data class RateLimited(
        val retryAfter: Long
    ) : BackendState()
}

enum class UnavailableReason {
    /** No network connection */
    NO_NETWORK,
    /** Network available but backend not reachable */
    BACKEND_DOWN,
    /** Backend returned 5xx error */
    SERVER_ERROR,
    /** Request timeout */
    TIMEOUT,
    /** Authentication failed */
    AUTH_FAILED
}

/**
 * Aggregated app state for UI consumption.
 */
data class AppHealthState(
    val permissionState: PermissionState,
    val scanState: ScanState,
    val backendState: BackendState,
    val isDefaultSmsApp: Boolean = false,
    val hasUnanalyzedMessages: Boolean = false,
    val pendingMessageCount: Int = 0
) {
    /** Whether the app is fully functional */
    val isFullyOperational: Boolean
        get() = permissionState is PermissionState.Granted &&
                isDefaultSmsApp &&
                backendState is BackendState.Available

    /** Whether we can perform analysis */
    val canAnalyze: Boolean
        get() = permissionState is PermissionState.Granted &&
                backendState is BackendState.Available

    /** Primary action the user should take */
    val primaryAction: PrimaryAction?
        get() = when {
            permissionState is PermissionState.Missing -> PrimaryAction.GRANT_PERMISSIONS
            permissionState is PermissionState.PermanentlyDenied -> PrimaryAction.OPEN_SETTINGS
            !isDefaultSmsApp -> PrimaryAction.SET_DEFAULT_SMS
            scanState is ScanState.NotStarted -> PrimaryAction.START_SCAN
            scanState is ScanState.Failed -> PrimaryAction.RETRY_SCAN
            backendState is BackendState.Unavailable -> PrimaryAction.CHECK_CONNECTION
            else -> null
        }
}

enum class PrimaryAction {
    GRANT_PERMISSIONS,
    OPEN_SETTINGS,
    SET_DEFAULT_SMS,
    START_SCAN,
    RETRY_SCAN,
    CHECK_CONNECTION
}

/**
 * Message category based on 2x2 matrix:
 * Toxic/Safe × Actionable/Noise
 */
enum class MessageCategory {
    /** Important and safe - SHOW */
    SAFE_LOGISTICS,
    /** Not important, safe - LOW PRIORITY */
    SAFE_NOISE,
    /** Important but harmful - MUST SEE with armor */
    TOXIC_LOGISTICS,
    /** Harmful, no info - FILTER OUT */
    TOXIC_NOISE;

    val isToxic: Boolean
        get() = this == TOXIC_LOGISTICS || this == TOXIC_NOISE

    val hasLogistics: Boolean
        get() = this == SAFE_LOGISTICS || this == TOXIC_LOGISTICS

    val shouldFilter: Boolean
        get() = this == TOXIC_NOISE

    val needsReview: Boolean
        get() = this == TOXIC_LOGISTICS

    companion object {
        fun fromAnalysis(isToxic: Boolean, hasLogistics: Boolean): MessageCategory {
            return when {
                isToxic && hasLogistics -> TOXIC_LOGISTICS
                isToxic && !hasLogistics -> TOXIC_NOISE
                !isToxic && hasLogistics -> SAFE_LOGISTICS
                else -> SAFE_NOISE
            }
        }

        fun fromString(value: String?): MessageCategory? {
            return value?.let {
                try {
                    valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Severity levels for toxic messages.
 */
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun fromScore(toxicityScore: Float): Severity {
            return when {
                toxicityScore >= 0.9f -> CRITICAL
                toxicityScore >= 0.7f -> HIGH
                toxicityScore >= 0.4f -> MEDIUM
                else -> LOW
            }
        }

        fun fromString(value: String?): Severity? {
            return value?.let {
                try {
                    valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
