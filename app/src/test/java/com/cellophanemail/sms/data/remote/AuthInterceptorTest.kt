package com.cellophanemail.sms.data.remote

import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var interceptor: AuthInterceptor
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        tokenManager = mockk(relaxed = true)
        interceptor = AuthInterceptor(tokenManager)

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `interceptor adds Authorization header when token is present`() {
        every { tokenManager.getAccessToken() } returns "test-jwt-token"

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer test-jwt-token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor does not add Authorization header when token is null`() {
        every { tokenManager.getAccessToken() } returns null

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor adds Content-Type header`() {
        every { tokenManager.getAccessToken() } returns null

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/json", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `interceptor adds Accept header`() {
        every { tokenManager.getAccessToken() } returns null

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `interceptor adds User-Agent header`() {
        every { tokenManager.getAccessToken() } returns null

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        val userAgent = recordedRequest.getHeader("User-Agent")
        assertTrue(userAgent?.startsWith("CellophaneSMS-Android/") == true)
    }

    @Test
    fun `interceptor preserves original request method`() {
        every { tokenManager.getAccessToken() } returns "token"

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .post("{}".toRequestBody())
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
    }

    @Test
    fun `interceptor preserves original request path`() {
        every { tokenManager.getAccessToken() } returns "token"

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/v1/messages/analyze"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/messages/analyze", recordedRequest.path)
    }

    @Test
    fun `interceptor handles empty token string`() {
        every { tokenManager.getAccessToken() } returns ""

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        // Empty string is still a non-null token, so header is added
        // "Bearer $token" with empty token produces "Bearer" (no trailing space)
        assertEquals("Bearer", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptor uses fresh token for each request`() {
        every { tokenManager.getAccessToken() } returnsMany listOf("token1", "token2")

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        // First request
        okHttpClient.newCall(
            Request.Builder().url(mockWebServer.url("/test1")).build()
        ).execute()

        // Second request
        okHttpClient.newCall(
            Request.Builder().url(mockWebServer.url("/test2")).build()
        ).execute()

        val request1 = mockWebServer.takeRequest()
        val request2 = mockWebServer.takeRequest()

        assertEquals("Bearer token1", request1.getHeader("Authorization"))
        assertEquals("Bearer token2", request2.getHeader("Authorization"))
    }

    @Test
    fun `interceptor preserves original request body`() {
        every { tokenManager.getAccessToken() } returns "token"

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val bodyContent = """{"message": "hello"}"""
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .post(bodyContent.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(bodyContent, recordedRequest.body.readUtf8())
    }

    @Test
    fun `interceptor returns server response correctly`() {
        every { tokenManager.getAccessToken() } returns "token"

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"status": "created"}""")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .build()

        val response = okHttpClient.newCall(request).execute()

        assertEquals(201, response.code)
        assertEquals("""{"status": "created"}""", response.body?.string())
    }
}
