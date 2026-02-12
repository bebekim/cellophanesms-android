package com.cellophanemail.sms.data.remote.api

import com.cellophanemail.sms.data.remote.model.AuthRequest
import com.cellophanemail.sms.data.remote.model.AuthResponse
import com.cellophanemail.sms.data.remote.model.BatchAnalysisRequest
import com.cellophanemail.sms.data.remote.model.BatchAnalysisResponse
import com.cellophanemail.sms.data.remote.model.BatchJobResponse
import com.cellophanemail.sms.data.remote.model.JobResultsResponse
import com.cellophanemail.sms.data.remote.model.JobStatusResponse
import com.cellophanemail.sms.data.remote.model.RefreshTokenRequest
import com.cellophanemail.sms.data.remote.model.SmsAnalysisRequest
import com.cellophanemail.sms.data.remote.model.SmsAnalysisResponse
import com.cellophanemail.sms.data.remote.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CellophoneMailApi {

    @POST("api/v1/messages/analyze")
    suspend fun analyzeSms(
        @Body request: SmsAnalysisRequest
    ): Response<SmsAnalysisResponse>

    @GET("api/v1/auth/profile")
    suspend fun getUserProfile(): Response<UserProfile>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    // Batch Analysis Endpoints

    @POST("api/v1/sms/analyze:batch")
    suspend fun analyzeBatch(
        @Body request: BatchAnalysisRequest
    ): Response<BatchAnalysisResponse>

    @POST("api/v1/sms/analyze:job")
    suspend fun submitAnalysisJob(
        @Body request: BatchAnalysisRequest
    ): Response<BatchJobResponse>

    @GET("api/v1/analysis/jobs/{jobId}")
    suspend fun getJobStatus(
        @Path("jobId") jobId: String
    ): Response<JobStatusResponse>

    @GET("api/v1/analysis/jobs/{jobId}/results")
    suspend fun getJobResults(
        @Path("jobId") jobId: String,
        @Query("cursor") cursor: String? = null
    ): Response<JobResultsResponse>
}
