package com.cellophanemail.sms.data.remote

import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val api: dagger.Lazy<CellophoneMailApi>
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry auth endpoints to avoid infinite loops
        val path = response.request.url.encodedPath
        if (path.contains("auth/login") ||
            path.contains("auth/register") ||
            path.contains("auth/refresh")
        ) {
            return null
        }

        // Don't retry if we already retried once
        if (responseCount(response) >= 2) {
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        val newAccessToken = runBlocking {
            mutex.withLock {
                // Double-check: another thread may have already refreshed
                if (!tokenManager.isTokenExpired()) {
                    return@withLock tokenManager.getAccessToken()
                }

                try {
                    val refreshResponse = api.get().refreshToken(
                        RefreshTokenRequest(refreshToken)
                    )
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val body = refreshResponse.body()!!
                        tokenManager.saveTokens(
                            accessToken = body.accessToken,
                            refreshToken = refreshToken,
                            expiresIn = body.expiresIn
                        )
                        body.accessToken
                    } else {
                        tokenManager.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
