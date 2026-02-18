package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.data.local.NerProviderMode
import com.cellophanemail.sms.data.local.NerProviderPreferences
import com.cellophanemail.sms.domain.model.AnnotationType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TieredNerAnnotationSourceTest {

    private lateinit var providerA: NerProvider
    private lateinit var providerB: NerProvider
    private lateinit var providerC: NerProvider
    private lateinit var preferences: NerProviderPreferences
    private lateinit var source: TieredNerAnnotationSource

    private val modeFlow = MutableStateFlow(NerProviderMode.AUTO)

    @Before
    fun setUp() {
        providerA = mockk(relaxed = true) {
            every { providerId } returns "gemini_nano"
            every { requiresNetwork } returns false
        }
        providerB = mockk(relaxed = true) {
            every { providerId } returns "qwen3_local"
            every { requiresNetwork } returns false
        }
        providerC = mockk(relaxed = true) {
            every { providerId } returns "claude_cloud"
            every { requiresNetwork } returns true
        }
        preferences = mockk(relaxed = true) {
            every { selectedProvider } returns modeFlow
        }
        source = TieredNerAnnotationSource(
            providers = listOf(providerA, providerB, providerC),
            preferences = preferences
        )
    }

    // ==================== Fallback Chain ====================

    @Test
    fun `uses first available provider in auto mode`() = runTest {
        coEvery { providerA.isAvailable() } returns true
        coEvery { providerA.extractEntities(any()) } returns listOf(
            NerEntity("John", AnnotationType.PERSON_NAME, 0, 4, 0.9f)
        )

        val result = source.annotate("John went home")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.PERSON_NAME, result[0].type)
    }

    @Test
    fun `falls back to second provider when first fails`() = runTest {
        coEvery { providerA.isAvailable() } returns true
        coEvery { providerA.extractEntities(any()) } throws RuntimeException("AICore not available")

        coEvery { providerB.isAvailable() } returns true
        coEvery { providerB.extractEntities(any()) } returns listOf(
            NerEntity("Google", AnnotationType.ORGANIZATION, 0, 6, 0.85f)
        )

        val result = source.annotate("Google is great")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.ORGANIZATION, result[0].type)
    }

    @Test
    fun `falls back to third provider when first two fail`() = runTest {
        coEvery { providerA.isAvailable() } returns false
        coEvery { providerB.isAvailable() } returns false
        coEvery { providerC.isAvailable() } returns true
        coEvery { providerC.extractEntities(any()) } returns listOf(
            NerEntity("Paris", AnnotationType.LOCATION, 0, 5, 0.9f)
        )

        val result = source.annotate("Paris is lovely")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.LOCATION, result[0].type)
    }

    @Test
    fun `returns empty when all providers fail`() = runTest {
        coEvery { providerA.isAvailable() } returns true
        coEvery { providerA.extractEntities(any()) } throws RuntimeException("fail")
        coEvery { providerB.isAvailable() } returns true
        coEvery { providerB.extractEntities(any()) } throws RuntimeException("fail")
        coEvery { providerC.isAvailable() } returns true
        coEvery { providerC.extractEntities(any()) } throws RuntimeException("fail")

        val result = source.annotate("John at Google in Paris")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty when all providers unavailable`() = runTest {
        coEvery { providerA.isAvailable() } returns false
        coEvery { providerB.isAvailable() } returns false
        coEvery { providerC.isAvailable() } returns false

        val result = source.annotate("John at Google in Paris")
        assertTrue(result.isEmpty())
    }

    // ==================== Manual Mode ====================

    @Test
    fun `uses only selected provider in manual mode`() = runTest {
        modeFlow.value = NerProviderMode.CLAUDE_CLOUD

        coEvery { providerC.isAvailable() } returns true
        coEvery { providerC.extractEntities(any()) } returns listOf(
            NerEntity("John", AnnotationType.PERSON_NAME, 0, 4, 0.9f)
        )

        val result = source.annotate("John went home")

        assertEquals(1, result.size)
    }

    @Test
    fun `returns empty when selected manual provider is unavailable`() = runTest {
        modeFlow.value = NerProviderMode.QWEN3_LOCAL
        coEvery { providerB.isAvailable() } returns false

        val result = source.annotate("John went home")
        assertTrue(result.isEmpty())
    }

    // ==================== OFF Mode ====================

    @Test
    fun `returns empty when mode is OFF`() = runTest {
        modeFlow.value = NerProviderMode.OFF

        val result = source.annotate("John at Google in Paris")
        assertTrue(result.isEmpty())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `returns empty for blank text`() = runTest {
        val result = source.annotate("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `annotations have correct source and priority`() = runTest {
        coEvery { providerA.isAvailable() } returns true
        coEvery { providerA.extractEntities(any()) } returns listOf(
            NerEntity("John", AnnotationType.PERSON_NAME, 0, 4, 0.9f)
        )

        val result = source.annotate("John went home")

        assertEquals(200, result[0].priority)
        assertEquals("gemini_nano", result[0].source)
    }
}
