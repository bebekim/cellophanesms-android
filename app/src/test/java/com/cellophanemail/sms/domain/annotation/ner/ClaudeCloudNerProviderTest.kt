package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.data.remote.NerEntityDto
import com.cellophanemail.sms.data.remote.NerExtractionApi
import com.cellophanemail.sms.data.remote.NerExtractionRequest
import com.cellophanemail.sms.data.remote.NerExtractionResponse
import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ClaudeCloudNerProviderTest {

    private lateinit var api: NerExtractionApi
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var provider: ClaudeCloudNerProvider

    private val connectedFlow = MutableStateFlow(true)

    @Before
    fun setUp() {
        api = mockk()
        networkMonitor = mockk {
            every { isConnected } returns connectedFlow
        }
        provider = ClaudeCloudNerProvider(api, networkMonitor)
    }

    // ==================== Provider Identity ====================

    @Test
    fun `providerId is claude_cloud`() {
        assertEquals("claude_cloud", provider.providerId)
    }

    @Test
    fun `requiresNetwork is true`() {
        assertTrue(provider.requiresNetwork)
    }

    // ==================== Availability ====================

    @Test
    fun `isAvailable returns true when network connected`() = runTest {
        connectedFlow.value = true
        assertTrue(provider.isAvailable())
    }

    @Test
    fun `isAvailable returns false when network disconnected`() = runTest {
        connectedFlow.value = false
        assertFalse(provider.isAvailable())
    }

    // ==================== Entity Extraction ====================

    @Test
    fun `extractEntities sends content field to API`() = runTest {
        val response = Response.success(
            NerExtractionResponse(entities = emptyList())
        )
        coEvery { api.extractEntities(any()) } returns response

        provider.extractEntities("Hello Sarah")

        coVerify {
            api.extractEntities(
                match { it.content == "Hello Sarah" }
            )
        }
    }

    @Test
    fun `extractEntities maps person_name entities`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Sarah", type = "person_name", start = 6, end = 11, confidence = 0.95f)
                ),
                tone = "warm"
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Hello Sarah")

        assertEquals(1, result.entities.size)
        assertEquals(AnnotationType.PERSON_NAME, result.entities[0].type)
        assertEquals("Sarah", result.entities[0].text)
        assertEquals(6, result.entities[0].startIndex)
        assertEquals(11, result.entities[0].endIndex)
        assertEquals(0.95f, result.entities[0].confidence)
    }

    @Test
    fun `extractEntities maps location entities`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "the park", type = "location", start = 14, end = 22, confidence = 0.88f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Meet Sarah at the park")

        assertEquals(1, result.entities.size)
        assertEquals(AnnotationType.LOCATION, result.entities[0].type)
    }

    @Test
    fun `extractEntities maps organization entities`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Google", type = "organization", start = 10, end = 16, confidence = 0.92f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("I work at Google")

        assertEquals(1, result.entities.size)
        assertEquals(AnnotationType.ORGANIZATION, result.entities[0].type)
    }

    @Test
    fun `extractEntities filters out non-NER types returned by server`() = runTest {
        // Server returns all 7 entity types, but Android only accepts 3 (PERSON_NAME, LOCATION, ORGANIZATION)
        // The other 4 (date_time, url, email, phone_number) are handled by Android's regex Phase 1
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Sarah", type = "person_name", start = 0, end = 5, confidence = 0.9f),
                    NerEntityDto(text = "https://example.com", type = "url", start = 15, end = 34, confidence = 0.99f),
                    NerEntityDto(text = "test@test.com", type = "email", start = 40, end = 53, confidence = 0.99f),
                    NerEntityDto(text = "tomorrow", type = "date_time", start = 60, end = 68, confidence = 0.85f),
                    NerEntityDto(text = "555-1234", type = "phone_number", start = 70, end = 78, confidence = 0.95f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Sarah said check https://example.com or test@test.com and call tomorrow at 555-1234")

        // Only person_name should survive parseAnnotationType filter
        assertEquals(1, result.entities.size)
        assertEquals(AnnotationType.PERSON_NAME, result.entities[0].type)
    }

    @Test
    fun `extractEntities handles multiple valid entities`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Sarah", type = "person_name", start = 5, end = 10, confidence = 0.95f),
                    NerEntityDto(text = "Central Park", type = "location", start = 14, end = 26, confidence = 0.9f),
                    NerEntityDto(text = "Apple", type = "organization", start = 32, end = 37, confidence = 0.88f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Meet Sarah at Central Park from Apple")

        assertEquals(3, result.entities.size)
        assertEquals(AnnotationType.PERSON_NAME, result.entities[0].type)
        assertEquals(AnnotationType.LOCATION, result.entities[1].type)
        assertEquals(AnnotationType.ORGANIZATION, result.entities[2].type)
    }

    // ==================== Index Validation ====================

    @Test
    fun `extractEntities coerces out-of-bounds indices`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Sarah", type = "person_name", start = 0, end = 100, confidence = 0.9f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Sarah")

        assertEquals(1, result.entities.size)
        assertEquals(0, result.entities[0].startIndex)
        assertEquals(5, result.entities[0].endIndex) // coerced to text.length
    }

    @Test
    fun `extractEntities drops entity with zero-length span after coercion`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "X", type = "person_name", start = 100, end = 101, confidence = 0.9f)
                )
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        // Both start and end will be coerced to text.length (3), making validEnd <= validStart
        val result = provider.extractEntities("abc")

        assertTrue(result.entities.isEmpty())
    }

    // ==================== Tone ====================

    @Test
    fun `extractEntities propagates tone from API response`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = listOf(
                    NerEntityDto(text = "Sarah", type = "person_name", start = 6, end = 11, confidence = 0.9f)
                ),
                tone = "formal"
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("Hello Sarah")

        assertEquals("formal", result.tone)
    }

    @Test
    fun `extractEntities returns null tone when API has no tone`() = runTest {
        val response = Response.success(
            NerExtractionResponse(
                entities = emptyList(),
                tone = null
            )
        )
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("test")

        assertNull(result.tone)
    }

    @Test
    fun `extractEntities returns null tone on null body`() = runTest {
        val response = Response.success<NerExtractionResponse>(null)
        coEvery { api.extractEntities(any()) } returns response

        val result = provider.extractEntities("test")

        assertTrue(result.entities.isEmpty())
        assertNull(result.tone)
    }

    // ==================== Error Handling ====================

    @Test
    fun `extractEntities throws on HTTP error`() = runTest {
        val response = Response.error<NerExtractionResponse>(
            500,
            okhttp3.ResponseBody.create(null, """{"error":"internal"}""")
        )
        coEvery { api.extractEntities(any()) } returns response

        try {
            provider.extractEntities("test")
            assertTrue("Should have thrown", false)
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("500") == true)
        }
    }
}
