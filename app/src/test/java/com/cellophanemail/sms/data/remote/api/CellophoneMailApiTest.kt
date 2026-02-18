package com.cellophanemail.sms.data.remote.api

import com.cellophanemail.sms.data.remote.model.SmsAnalysisRequest
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CellophoneMailApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: CellophoneMailApi
    private val gson = Gson()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CellophoneMailApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ==================== SMS Analysis Tests ====================

    @Test
    fun `analyzeSms returns successful response with SAFE classification`() = runTest {
        val responseJson = """
            {
                "classification": "SAFE",
                "toxicity_score": 0.1,
                "horsemen": [],
                "reasoning": "This message is friendly and supportive.",
                "filtered_summary": "Hello, how are you?",
                "specific_examples": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "Hello, how are you?",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("SAFE", body?.classification)
        assertEquals(0.1f, body?.toxicityScore)
        assertTrue(body?.horsemen?.isEmpty() == true)
    }

    @Test
    fun `analyzeSms returns WARNING classification with horsemen`() = runTest {
        val responseJson = """
            {
                "classification": "WARNING",
                "toxicity_score": 0.5,
                "horsemen": ["criticism", "defensiveness"],
                "reasoning": "The message contains critical language.",
                "filtered_summary": "The sender expressed some concerns.",
                "specific_examples": ["You always do this wrong"]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "You always do this wrong!",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("WARNING", body?.classification)
        assertEquals(0.5f, body?.toxicityScore)
        assertEquals(2, body?.horsemen?.size)
        assertTrue(body?.horsemen?.contains("criticism") == true)
        assertTrue(body?.horsemen?.contains("defensiveness") == true)
    }

    @Test
    fun `analyzeSms returns HARMFUL classification with contempt`() = runTest {
        val responseJson = """
            {
                "classification": "HARMFUL",
                "toxicity_score": 0.8,
                "horsemen": ["contempt"],
                "reasoning": "The message expresses superiority and disdain.",
                "filtered_summary": "The sender expressed strong negative emotions.",
                "specific_examples": ["You're pathetic"]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "You're pathetic and worthless",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertEquals("HARMFUL", body?.classification)
        assertEquals(0.8f, body?.toxicityScore)
        assertTrue(body?.horsemen?.contains("contempt") == true)
    }

    @Test
    fun `analyzeSms handles 401 unauthorized error`() = runTest {
        val errorJson = """
            {
                "error": "unauthorized",
                "detail": "Invalid or expired token"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "Test message",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `analyzeSms handles 429 rate limit error`() = runTest {
        val errorJson = """
            {
                "error": "rate_limit_exceeded",
                "detail": "API quota exceeded. Please wait before trying again."
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody(errorJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "Test message",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertFalse(response.isSuccessful)
        assertEquals(429, response.code())
    }

    @Test
    fun `analyzeSms handles 500 server error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "internal_error", "detail": "Server error"}""")
                .addHeader("Content-Type", "application/json")
        )

        val request = SmsAnalysisRequest(
            content = "Test message",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = "test-device"
        )

        val response = api.analyzeSms(request)

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `analyzeSms sends correct request body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "classification": "SAFE",
                        "toxicity_score": 0.0,
                        "horsemen": [],
                        "reasoning": "",
                        "filtered_summary": "",
                        "specific_examples": []
                    }
                """.trimIndent())
                .addHeader("Content-Type", "application/json")
        )

        val timestamp = 1234567890L
        val request = SmsAnalysisRequest(
            content = "Hello world",
            sender = "+19995551234",
            timestamp = timestamp,
            deviceId = "device-123"
        )

        api.analyzeSms(request)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/api/v1/messages/analyze", recordedRequest.path)

        val bodyJson = recordedRequest.body.readUtf8()
        assertTrue(bodyJson.contains("\"content\":\"Hello world\""))
        assertTrue(bodyJson.contains("\"sender\":\"+19995551234\""))
        assertTrue(bodyJson.contains("\"timestamp\":$timestamp"))
        assertTrue(bodyJson.contains("\"device_id\":\"device-123\""))
    }

    @Test
    fun `analyzeSms handles empty specific_examples`() = runTest {
        val responseJson = """
            {
                "classification": "SAFE",
                "toxicity_score": 0.0,
                "horsemen": [],
                "reasoning": "This is safe",
                "filtered_summary": "Hello",
                "specific_examples": []
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )

        val request = SmsAnalysisRequest(
            content = "Hello",
            sender = "+1234567890",
            timestamp = System.currentTimeMillis(),
            deviceId = null
        )

        val response = api.analyzeSms(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertTrue(body?.specificExamples?.isEmpty() == true)
    }

    // ==================== User Profile Tests ====================

    @Test
    fun `getUserProfile returns user data`() = runTest {
        val responseJson = """
            {
                "id": "user-123",
                "email": "test@example.com",
                "subscription_status": "active",
                "api_quota": {
                    "used": 50,
                    "limit": 1000,
                    "reset_date": 1735689600000
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = api.getUserProfile()

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("user-123", body?.id)
        assertEquals("test@example.com", body?.email)
        assertEquals("active", body?.subscriptionStatus)
        assertEquals(50, body?.apiQuota?.used)
        assertEquals(1000, body?.apiQuota?.limit)
    }

    @Test
    fun `getUserProfile handles unauthorized error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "unauthorized", "detail": "Token expired"}""")
        )

        val response = api.getUserProfile()

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    // ==================== Auth Tests ====================

    @Test
    fun `login returns tokens on success`() = runTest {
        val responseJson = """
            {
                "access_token": "access-token-123",
                "refresh_token": "refresh-token-456",
                "expires_in": 3600,
                "user": {
                    "id": "user-123",
                    "email": "test@example.com",
                    "subscription_status": "active",
                    "api_quota": {
                        "used": 0,
                        "limit": 1000,
                        "reset_date": 1735689600000
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val request = com.cellophanemail.sms.data.remote.model.LoginRequest(
            identifier = "test@example.com",
            password = "password123"
        )

        val response = api.login(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("access-token-123", body?.accessToken)
        assertEquals("refresh-token-456", body?.refreshToken)
        assertEquals(3600L, body?.expiresIn)
        assertEquals("user-123", body?.user?.id)
    }

    @Test
    fun `login handles invalid credentials`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "invalid_credentials", "detail": "Email or password incorrect"}""")
        )

        val request = com.cellophanemail.sms.data.remote.model.LoginRequest(
            identifier = "wrong@example.com",
            password = "wrongpassword"
        )

        val response = api.login(request)

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `refreshToken returns new tokens`() = runTest {
        val responseJson = """
            {
                "access_token": "new-access-token",
                "refresh_token": "new-refresh-token",
                "expires_in": 3600,
                "user": {
                    "id": "user-123",
                    "email": "test@example.com",
                    "subscription_status": "active",
                    "api_quota": {
                        "used": 10,
                        "limit": 1000,
                        "reset_date": 1735689600000
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )

        val request = com.cellophanemail.sms.data.remote.model.RefreshTokenRequest(
            refreshToken = "old-refresh-token"
        )

        val response = api.refreshToken(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("new-access-token", body?.accessToken)
        assertEquals("new-refresh-token", body?.refreshToken)
    }

    @Test
    fun `refreshToken handles expired token`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "token_expired", "detail": "Refresh token has expired"}""")
        )

        val request = com.cellophanemail.sms.data.remote.model.RefreshTokenRequest(
            refreshToken = "expired-token"
        )

        val response = api.refreshToken(request)

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `register creates new user account`() = runTest {
        val responseJson = """
            {
                "access_token": "new-user-token",
                "refresh_token": "new-refresh-token",
                "expires_in": 3600,
                "user": {
                    "id": "new-user-456",
                    "email": "newuser@example.com",
                    "subscription_status": "trial",
                    "api_quota": {
                        "used": 0,
                        "limit": 100,
                        "reset_date": 1735689600000
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(responseJson)
        )

        val request = com.cellophanemail.sms.data.remote.model.RegisterRequest(
            email = "newuser@example.com",
            password = "securepassword123"
        )

        val response = api.register(request)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("new-user-456", body?.user?.id)
        assertEquals("trial", body?.user?.subscriptionStatus)
    }

    @Test
    fun `register handles duplicate email`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("""{"error": "email_exists", "detail": "Email already registered"}""")
        )

        val request = com.cellophanemail.sms.data.remote.model.RegisterRequest(
            email = "existing@example.com",
            password = "password123"
        )

        val response = api.register(request)

        assertFalse(response.isSuccessful)
        assertEquals(409, response.code())
    }
}
