package com.cellophanemail.sms.data.remote.model

import com.google.gson.annotations.SerializedName

data class SmsAnalysisRequest(
    val content: String,
    val sender: String,
    val timestamp: Long,
    @SerializedName("device_id")
    val deviceId: String?
)

data class SmsAnalysisResponse(
    val classification: String,
    @SerializedName("toxicity_score")
    val toxicityScore: Float,
    val horsemen: List<String>,
    val reasoning: String,
    @SerializedName("filtered_summary")
    val filteredSummary: String,
    @SerializedName("specific_examples")
    val specificExamples: List<String>
)

data class UserProfile(
    @SerializedName("user_id", alternate = ["id"])
    val id: String,
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("phone_verified")
    val phoneVerified: Boolean? = null,
    @SerializedName("verification_method")
    val verificationMethod: String? = null,
    val username: String? = null,
    val role: String? = null,
    @SerializedName("is_verified")
    val isVerified: Boolean? = null,
    @SerializedName("subscription_status")
    val subscriptionStatus: String? = null,
    @SerializedName("api_quota")
    val apiQuota: ApiQuota? = null
)

data class ApiQuota(
    val used: Int,
    val limit: Int,
    @SerializedName("reset_date")
    val resetDate: Long
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class RegisterRequest(
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    val user: UserProfile
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class ApiError(
    val error: String,
    val detail: String?
)

// Batch Analysis Models

data class BatchAnalysisRequest(
    val client: ClientInfo,
    val analysis: AnalysisConfig,
    @SerializedName("sender_id_strategy")
    val senderIdStrategy: String = "normalized",
    val messages: List<BatchMessageRequest>
)

data class ClientInfo(
    val platform: String = "android",
    @SerializedName("app_version")
    val appVersion: String,
    val timezone: String
)

data class AnalysisConfig(
    @SerializedName("engine_version")
    val engineVersion: String = "v1",
    val features: AnalysisFeatures = AnalysisFeatures()
)

data class AnalysisFeatures(
    @SerializedName("four_horsemen")
    val fourHorsemen: Boolean = true,
    @SerializedName("logistics_detection")
    val logisticsDetection: Boolean = true
)

data class BatchMessageRequest(
    @SerializedName("client_message_id")
    val clientMessageId: String,
    @SerializedName("sender_id")
    val senderId: String,
    val direction: String,
    @SerializedName("timestamp_ms")
    val timestampMs: Long,
    val body: String
)

data class BatchAnalysisResponse(
    @SerializedName("engine_version")
    val engineVersion: String,
    val received: Int,
    val processed: Int,
    val results: List<MessageAnalysisResult>,
    @SerializedName("sender_summaries")
    val senderSummaries: List<SenderSummaryResponse>?,
    @SerializedName("dashboard_rollup")
    val dashboardRollup: DashboardRollup?
)

data class MessageAnalysisResult(
    @SerializedName("client_message_id")
    val clientMessageId: String,
    val classification: MessageClassification,
    val signals: AnalysisSignals,
    val explain: AnalysisExplanation?
)

data class MessageClassification(
    @SerializedName("is_filtered")
    val isFiltered: Boolean,
    val category: String?,
    val severity: String?
)

data class AnalysisSignals(
    @SerializedName("four_horsemen")
    val fourHorsemen: List<HorsemanSignal>?,
    @SerializedName("has_logistics")
    val hasLogistics: Boolean?
)

data class HorsemanSignal(
    val type: String,
    val confidence: Float
)

data class AnalysisExplanation(
    @SerializedName("short_reason")
    val shortReason: String?
)

data class SenderSummaryResponse(
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("filtered_count")
    val filteredCount: Int,
    @SerializedName("noise_count")
    val noiseCount: Int,
    @SerializedName("toxic_logistics_count")
    val toxicLogisticsCount: Int,
    @SerializedName("last_filtered_timestamp_ms")
    val lastFilteredTimestampMs: Long?,
    @SerializedName("horsemen_counts")
    val horsemenCounts: HorsemenCounts?
)

data class HorsemenCounts(
    @SerializedName("CRITICISM")
    val criticism: Int = 0,
    @SerializedName("CONTEMPT")
    val contempt: Int = 0,
    @SerializedName("DEFENSIVENESS")
    val defensiveness: Int = 0,
    @SerializedName("STONEWALLING")
    val stonewalling: Int = 0
)

data class DashboardRollup(
    @SerializedName("toxic_senders")
    val toxicSenders: Int,
    @SerializedName("filtered_messages")
    val filteredMessages: Int,
    @SerializedName("noise_messages")
    val noiseMessages: Int,
    @SerializedName("toxic_logistics_messages")
    val toxicLogisticsMessages: Int
)

// Async Job Models

data class BatchJobResponse(
    @SerializedName("job_id")
    val jobId: String,
    val status: String
)

data class JobStatusResponse(
    @SerializedName("job_id")
    val jobId: String,
    val status: String,
    val progress: JobProgress?,
    @SerializedName("partial_result")
    val partialResult: PartialJobResult?
)

data class JobProgress(
    val received: Int,
    val processed: Int
)

data class PartialJobResult(
    @SerializedName("dashboard_rollup")
    val dashboardRollup: DashboardRollup?
)

data class JobResultsResponse(
    val results: List<MessageAnalysisResult>,
    @SerializedName("sender_summaries")
    val senderSummaries: List<SenderSummaryResponse>?,
    @SerializedName("next_cursor")
    val nextCursor: String?
)
