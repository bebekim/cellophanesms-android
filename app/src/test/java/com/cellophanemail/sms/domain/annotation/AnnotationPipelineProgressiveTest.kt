package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationPipelineProgressiveTest {

    // ==================== Progressive Flow ====================

    @Test
    fun `progressive emits regex results first then merged`() = runTest {
        val regexSource = RegexEntitySource()
        val nerSource = FakeNerAnnotationSource(
            entities = listOf(
                TextAnnotation(
                    type = AnnotationType.PERSON_NAME,
                    startIndex = 5,
                    endIndex = 9,
                    confidence = 0.9f,
                    source = "tiered_ner",
                    priority = 200
                )
            )
        )

        val pipeline = AnnotationPipeline(listOf(regexSource, nerSource))
        val text = "Meet John tomorrow"

        val emissions = pipeline.annotateProgressive(text).toList()

        // First emission: regex only (DATE_TIME for "tomorrow")
        assertTrue(emissions.size >= 1)
        assertTrue(emissions[0].all { it.source == RegexEntitySource.SOURCE_ID })

        // Second emission: regex + NER merged
        if (emissions.size >= 2) {
            assertTrue(emissions[1].any { it.type == AnnotationType.PERSON_NAME })
            assertTrue(emissions[1].any { it.type == AnnotationType.DATE_TIME })
        }
    }

    @Test
    fun `progressive emits single result when no NER sources`() = runTest {
        val regexSource = RegexEntitySource()
        val pipeline = AnnotationPipeline(listOf(regexSource))
        val text = "Call 555-123-4567"

        val emissions = pipeline.annotateProgressive(text).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].any { it.type == AnnotationType.PHONE_NUMBER })
    }

    @Test
    fun `progressive emits empty for blank text`() = runTest {
        val pipeline = AnnotationPipeline(listOf(RegexEntitySource()))
        val emissions = pipeline.annotateProgressive("").toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())
    }

    @Test
    fun `progressive emits single when NER returns empty`() = runTest {
        val regexSource = RegexEntitySource()
        val nerSource = FakeNerAnnotationSource(entities = emptyList())

        val pipeline = AnnotationPipeline(listOf(regexSource, nerSource))
        val text = "Call 555-123-4567"

        val emissions = pipeline.annotateProgressive(text).toList()

        // Only one emission because NER returned nothing
        assertEquals(1, emissions.size)
    }

    @Test
    fun `progressive handles NER source failure gracefully`() = runTest {
        val regexSource = RegexEntitySource()
        val failingSource = FailingAnnotationSource()

        val pipeline = AnnotationPipeline(listOf(regexSource, failingSource))
        val text = "Call 555-123-4567"

        val emissions = pipeline.annotateProgressive(text).toList()

        // Should still get regex results even if NER fails
        assertTrue(emissions.isNotEmpty())
        assertTrue(emissions[0].any { it.type == AnnotationType.PHONE_NUMBER })
    }

    // ==================== Backward Compatibility ====================

    @Test
    fun `existing annotate method still works with multiple sources`() = runTest {
        val regexSource = RegexEntitySource()
        val nerSource = FakeNerAnnotationSource(
            entities = listOf(
                TextAnnotation(
                    type = AnnotationType.PERSON_NAME,
                    startIndex = 5,
                    endIndex = 9,
                    confidence = 0.9f,
                    source = "tiered_ner",
                    priority = 200
                )
            )
        )

        val pipeline = AnnotationPipeline(listOf(regexSource, nerSource))
        val result = pipeline.annotate("Meet John tomorrow")

        assertTrue(result.any { it.type == AnnotationType.PERSON_NAME })
        assertTrue(result.any { it.type == AnnotationType.DATE_TIME })
    }

    // ==================== Merge Integration ====================

    @Test
    fun `progressive merges overlapping regex and NER correctly`() = runTest {
        val regexSource = RegexEntitySource()
        // NER claims "John" at same position — both should coexist since non-overlapping
        val nerSource = FakeNerAnnotationSource(
            entities = listOf(
                TextAnnotation(
                    type = AnnotationType.PERSON_NAME,
                    startIndex = 0,
                    endIndex = 4,
                    confidence = 0.9f,
                    source = "tiered_ner",
                    priority = 200
                )
            )
        )

        val pipeline = AnnotationPipeline(listOf(regexSource, nerSource))
        val text = "John visited https://example.com"

        val emissions = pipeline.annotateProgressive(text).toList()

        // Final emission should have both PERSON_NAME and URL
        val final = emissions.last()
        assertTrue(final.any { it.type == AnnotationType.PERSON_NAME })
        assertTrue(final.any { it.type == AnnotationType.URL })
    }
}

// ==================== Test Helpers ====================

private class FakeNerAnnotationSource(
    private val entities: List<TextAnnotation>
) : AnnotationSource {
    override val sourceId = "tiered_ner"
    override val defaultPriority = 200
    override val requiresNetwork = false

    override suspend fun annotate(text: String): List<TextAnnotation> = entities
}

private class FailingAnnotationSource : AnnotationSource {
    override val sourceId = "failing_ner"
    override val defaultPriority = 200
    override val requiresNetwork = false

    override suspend fun annotate(text: String): List<TextAnnotation> {
        throw RuntimeException("NER provider crashed")
    }
}
