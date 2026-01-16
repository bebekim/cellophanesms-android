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
    val id: String,
    val email: String,
    @SerializedName("subscription_status")
    val subscriptionStatus: String,
    @SerializedName("api_quota")
    val apiQuota: ApiQuota
)

data class ApiQuota(
    val used: Int,
    val limit: Int,
    @SerializedName("reset_date")
    val resetDate: Long
)

data class AuthRequest(
    val email: String,
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
