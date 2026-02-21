package com.cellophanemail.sms.data.remote.api

import com.cellophanemail.sms.data.remote.NerExtractionApi
import com.cellophanemail.sms.data.remote.NerExtractionRequest
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

class NerExtractionApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: NerExtractionApi

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
            .create(NerExtractionApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ==================== Request Tests ====================

    @Test
    fun `sends POST to correct endpoint path`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":5,"extractor_used":"mock","channel":"sms"}""")
                .addHeader("Content-Type", "application/json")
        )

        api.extractEntities(NerExtractionRequest(content = "Hello"))

        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/messages/extract-entities", recorded.path)
    }

    @Test
    fun `sends content field in request body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":5,"extractor_used":"mock","channel":"sms"}""")
        )

        api.extractEntities(NerExtractionRequest(content = "Meet Sarah at the park"))

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue("Request should contain 'content' field", body.contains("\"content\":\"Meet Sarah at the park\""))
    }

    @Test
    fun `sends channel field in request body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":5,"extractor_used":"mock","channel":"sms"}""")
        )

        api.extractEntities(NerExtractionRequest(content = "Hello", channel = "sms"))

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue("Request should contain 'channel' field", body.contains("\"channel\":\"sms\""))
    }

    // ==================== Response Tests ====================

    @Test
    fun `parses successful response with entities`() = runTest {
        val responseJson = """
            {
                "entities": [
                    {"text": "Sarah", "type": "person_name", "start": 5, "end": 10, "confidence": 0.95},
                    {"text": "the park", "type": "location", "start": 14, "end": 22, "confidence": 0.88}
                ],
                "tone": "casual",
                "processing_time_ms": 42,
                "extractor_used": "anthropic",
                "channel": "sms"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
                .addHeader("Content-Type", "application/json")
        )

        val response = api.extractEntities(NerExtractionRequest(content = "Meet Sarah at the park"))

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals(2, body?.entities?.size)

        val sarah = body?.entities?.get(0)
        assertEquals("Sarah", sarah?.text)
        assertEquals("person_name", sarah?.type)
        assertEquals(5, sarah?.start)
        assertEquals(10, sarah?.end)
        assertEquals(0.95f, sarah?.confidence)

        val park = body?.entities?.get(1)
        assertEquals("the park", park?.text)
        assertEquals("location", park?.type)
    }

    @Test
    fun `parses tone from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":"warm","processing_time_ms":10,"extractor_used":"mock","channel":"sms"}""")
        )

        val response = api.extractEntities(NerExtractionRequest(content = "Hello"))
        assertEquals("warm", response.body()?.tone)
    }

    @Test
    fun `handles null tone in response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":10,"extractor_used":"mock","channel":"sms"}""")
        )

        val response = api.extractEntities(NerExtractionRequest(content = "Hello"))
        assertNull(response.body()?.tone)
    }

    @Test
    fun `parses processing metadata from response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":150,"extractor_used":"anthropic","channel":"email"}""")
        )

        val response = api.extractEntities(NerExtractionRequest(content = "Hello"))
        val body = response.body()
        assertEquals(150, body?.processingTimeMs)
        assertEquals("anthropic", body?.extractorUsed)
        assertEquals("email", body?.channel)
    }

    @Test
    fun `parses empty entities list`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"entities":[],"tone":null,"processing_time_ms":5,"extractor_used":"mock","channel":"sms"}""")
        )

        val response = api.extractEntities(NerExtractionRequest(content = "no entities here"))
        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.entities?.isEmpty() == true)
    }

    // ==================== Error Tests ====================

    @Test
    fun `handles 401 unauthorized`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = api.extractEntities(NerExtractionRequest(content = "test"))
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun `handles 500 server error`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val response = api.extractEntities(NerExtractionRequest(content = "test"))
        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }
}
