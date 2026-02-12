package com.cellophanemail.sms.data.repository

import com.cellophanemail.sms.data.remote.TokenManager
import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.AuthRequest
import com.cellophanemail.sms.data.remote.model.AuthResponse
import com.cellophanemail.sms.data.remote.model.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: CellophoneMailApi,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(AuthRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                tokenManager.saveTokens(
                    accessToken = auth.accessToken,
                    refreshToken = auth.refreshToken,
                    expiresIn = auth.expiresIn
                )
                Result.success(auth)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.register(AuthRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                tokenManager.saveTokens(
                    accessToken = auth.accessToken,
                    refreshToken = auth.refreshToken,
                    expiresIn = auth.expiresIn
                )
                Result.success(auth)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<String> {
        val currentRefreshToken = tokenManager.getRefreshToken()
            ?: return Result.failure(Exception("No refresh token"))

        return try {
            val response = api.refreshToken(RefreshTokenRequest(currentRefreshToken))
            if (response.isSuccessful && response.body() != null) {
                val refresh = response.body()!!
                tokenManager.saveTokens(
                    accessToken = refresh.accessToken,
                    refreshToken = currentRefreshToken,
                    expiresIn = refresh.expiresIn
                )
                Result.success(refresh.accessToken)
            } else {
                Result.failure(Exception("Token refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.hasValidToken()

    fun logout() {
        tokenManager.clearTokens()
    }
}
