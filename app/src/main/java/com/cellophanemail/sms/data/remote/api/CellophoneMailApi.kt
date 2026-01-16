package com.cellophanemail.sms.data.remote.api

import com.cellophanemail.sms.data.remote.model.AuthRequest
import com.cellophanemail.sms.data.remote.model.AuthResponse
import com.cellophanemail.sms.data.remote.model.RefreshTokenRequest
import com.cellophanemail.sms.data.remote.model.SmsAnalysisRequest
import com.cellophanemail.sms.data.remote.model.SmsAnalysisResponse
import com.cellophanemail.sms.data.remote.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CellophoneMailApi {

    @POST("api/v1/messages/analyze")
    suspend fun analyzeSms(
        @Body request: SmsAnalysisRequest
    ): Response<SmsAnalysisResponse>

    @GET("api/v1/user/profile")
    suspend fun getUserProfile(): Response<UserProfile>

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>
}
