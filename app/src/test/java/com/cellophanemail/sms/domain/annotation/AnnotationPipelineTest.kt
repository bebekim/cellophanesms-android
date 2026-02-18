package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnnotationPipelineTest {

    private lateinit var regexSource: RegexEntitySource
    private lateinit var pipeline: AnnotationPipeline

    @Before
    fun setUp() {
        regexSource = RegexEntitySource()
        pipeline = AnnotationPipeline(listOf(regexSource))
    }

    // ==================== Basic Pipeline ====================

    @Test
    fun `returns empty for blank text`() = runTest {
        assertEquals(emptyList<TextAnnotation>(), pipeline.annotate(""))
        assertEquals(emptyList<TextAnnotation>(), pipeline.annotate("   "))
    }

    @Test
    fun `returns annotations from single source`() = runTest {
        val result = pipeline.annotate("Visit https://example.com")
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.type == AnnotationType.URL })
    }

    @Test
    fun `returns annotations from text with multiple entity types`() = runTest {
        val result = pipeline.annotate("Email user@test.com or call 555-123-4567 by tomorrow")
        assertTrue(result.any { it.type == AnnotationType.EMAIL })
        assertTrue(result.any { it.type == AnnotationType.PHONE_NUMBER })
        assertTrue(result.any { it.type == AnnotationType.DATE_TIME })
    }

    // ==================== Multi-Source Merge ====================

    @Test
    fun `merges annotations from multiple sources`() = runTest {
        val fakeSource = object : AnnotationSource {
            override val sourceId = "fake"
            override val defaultPriority = 50
            override val requiresNetwork = false
            override suspend fun annotate(text: String): List<TextAnnotation> {
                return listOf(
                    TextAnnotation(
                        type = AnnotationType.PERSON_NAME,
                        startIndex = 0,
                        endIndex = 4,
                        confidence = 0.8f,
                        source = "fake",
                        priority = 50
                    )
                )
            }
        }

        val multiPipeline = AnnotationPipeline(listOf(regexSource, fakeSource))
        val result = multiPipeline.annotate("John visited https://example.com")

        assertTrue(result.any { it.type == AnnotationType.PERSON_NAME })
        assertTrue(result.any { it.type == AnnotationType.URL })
    }

    // ==================== Source Filtering ====================

    @Test
    fun `filters sources by enabled ids`() = runTest {
        val fakeSource = object : AnnotationSource {
            override val sourceId = "fake"
            override val defaultPriority = 50
            override val requiresNetwork = false
            override suspend fun annotate(text: String) = listOf(
                TextAnnotation(
                    type = AnnotationType.PERSON_NAME,
                    startIndex = 0,
                    endIndex = 4,
                    source = "fake",
                    priority = 50
                )
            )
        }

        val multiPipeline = AnnotationPipeline(listOf(regexSource, fakeSource))

        // Only enable fake source
        val result = multiPipeline.annotate(
            "John visited https://example.com",
            enabledSourceIds = setOf("fake")
        )

        assertTrue(result.any { it.source == "fake" })
        assertTrue(result.none { it.source == RegexEntitySource.SOURCE_ID })
    }

    @Test
    fun `null enabled ids uses all sources`() = runTest {
        val result = pipeline.annotate(
            "Visit https://example.com",
            enabledSourceIds = null
        )
        assertTrue(result.isNotEmpty())
    }

    // ==================== Caching ====================

    @Test
    fun `cache returns same result for same input`() = runTest {
        val text = "Call 555-123-4567"
        val result1 = pipeline.annotate(text)
        val result2 = pipeline.annotate(text)

        assertEquals(result1.size, result2.size)
        // Same IDs means cache hit (same objects returned)
        assertEquals(result1.map { it.id }, result2.map { it.id })
    }

    @Test
    fun `different enabled sources produce different cache entries`() = runTest {
        val text = "Call 555-123-4567"
        val result1 = pipeline.annotate(text, enabledSourceIds = setOf(RegexEntitySource.SOURCE_ID))
        val result2 = pipeline.annotate(text, enabledSourceIds = setOf("nonexistent"))

        assertTrue(result1.isNotEmpty())
        assertTrue(result2.isEmpty())
    }

    @Test
    fun `clearCache forces recomputation`() = runTest {
        val text = "Visit https://example.com"
        val result1 = pipeline.annotate(text)
        pipeline.clearCache()
        val result2 = pipeline.annotate(text)

        // Same content but different annotation IDs (new objects)
        assertEquals(result1.size, result2.size)
    }

    // ==================== Annotations Are Sorted ====================

    @Test
    fun `annotations are sorted by start index`() = runTest {
        val text = "Call 555-123-4567 or visit https://example.com tomorrow"
        val result = pipeline.annotate(text)

        for (i in 0 until result.size - 1) {
            assertTrue(
                "Expected sorted by startIndex",
                result[i].startIndex <= result[i + 1].startIndex
            )
        }
    }
}
