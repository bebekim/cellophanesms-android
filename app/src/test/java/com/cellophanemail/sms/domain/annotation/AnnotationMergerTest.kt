package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.AnnotationType
import com.cellophanemail.sms.domain.model.TextAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationMergerTest {

    private fun annotation(
        type: AnnotationType = AnnotationType.URL,
        start: Int,
        end: Int,
        priority: Int = 100,
        confidence: Float = 1.0f,
        source: String = "test"
    ) = TextAnnotation(
        type = type,
        startIndex = start,
        endIndex = end,
        priority = priority,
        confidence = confidence,
        source = source
    )

    // ==================== Basic Merge ====================

    @Test
    fun `returns empty list for empty input`() {
        assertEquals(emptyList<TextAnnotation>(), AnnotationMerger.merge(emptyList()))
    }

    @Test
    fun `returns single annotation unchanged`() {
        val ann = annotation(start = 0, end = 10)
        val result = AnnotationMerger.merge(listOf(ann))
        assertEquals(1, result.size)
        assertEquals(ann, result[0])
    }

    @Test
    fun `non-overlapping annotations preserved`() {
        val a1 = annotation(start = 0, end = 5)
        val a2 = annotation(start = 10, end = 15)
        val result = AnnotationMerger.merge(listOf(a1, a2))
        assertEquals(2, result.size)
    }

    @Test
    fun `result is sorted by start index`() {
        val a1 = annotation(start = 20, end = 30)
        val a2 = annotation(start = 5, end = 10)
        val a3 = annotation(start = 0, end = 3)
        val result = AnnotationMerger.merge(listOf(a1, a2, a3))

        assertEquals(3, result.size)
        assertEquals(0, result[0].startIndex)
        assertEquals(5, result[1].startIndex)
        assertEquals(20, result[2].startIndex)
    }

    // ==================== Priority Resolution ====================

    @Test
    fun `higher priority wins full overlap`() {
        val high = annotation(start = 0, end = 10, priority = 200)
        val low = annotation(start = 0, end = 10, priority = 100)
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(1, result.size)
        assertEquals(200, result[0].priority)
    }

    @Test
    fun `higher priority wins when it contains lower`() {
        val high = annotation(start = 0, end = 20, priority = 200)
        val low = annotation(start = 5, end = 15, priority = 100)
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(1, result.size)
        assertEquals(200, result[0].priority)
    }

    // ==================== Confidence Tiebreak ====================

    @Test
    fun `higher confidence wins when priority is equal`() {
        val highConf = annotation(start = 0, end = 10, priority = 100, confidence = 0.9f)
        val lowConf = annotation(start = 0, end = 10, priority = 100, confidence = 0.5f)
        val result = AnnotationMerger.merge(listOf(lowConf, highConf))

        assertEquals(1, result.size)
        assertEquals(0.9f, result[0].confidence)
    }

    // ==================== Longer Span Tiebreak ====================

    @Test
    fun `longer span wins when priority and confidence are equal`() {
        val longer = annotation(start = 0, end = 15, priority = 100, confidence = 1.0f)
        val shorter = annotation(start = 0, end = 10, priority = 100, confidence = 1.0f)
        val result = AnnotationMerger.merge(listOf(shorter, longer))

        assertEquals(1, result.size)
        assertEquals(15, result[0].endIndex)
    }

    // ==================== Partial Overlap Truncation ====================

    @Test
    fun `partial overlap truncates lower priority to leading portion`() {
        val high = annotation(start = 5, end = 15, priority = 200)
        val low = annotation(start = 0, end = 10, priority = 100)
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(2, result.size)
        // Low priority should be truncated to [0, 5)
        val truncated = result.first { it.priority == 100 }
        assertEquals(0, truncated.startIndex)
        assertEquals(5, truncated.endIndex)
    }

    @Test
    fun `partial overlap truncates lower priority to trailing portion`() {
        val high = annotation(start = 0, end = 10, priority = 200)
        val low = annotation(start = 5, end = 20, priority = 100)
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(2, result.size)
        // Low priority should be truncated to [10, 20)
        val truncated = result.first { it.priority == 100 }
        assertEquals(10, truncated.startIndex)
        assertEquals(20, truncated.endIndex)
    }

    // ==================== Minimum Length ====================

    @Test
    fun `discards annotation when truncated to less than 2 chars`() {
        val high = annotation(start = 0, end = 10, priority = 200)
        val low = annotation(start = 9, end = 11, priority = 100) // Would be truncated to [10,11) = 1 char
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(1, result.size)
        assertEquals(200, result[0].priority)
    }

    @Test
    fun `keeps annotation when truncated to exactly 2 chars`() {
        val high = annotation(start = 0, end = 10, priority = 200)
        val low = annotation(start = 8, end = 12, priority = 100) // Truncated to [10,12) = 2 chars
        val result = AnnotationMerger.merge(listOf(low, high))

        assertEquals(2, result.size)
    }

    // ==================== Deduplication ====================

    @Test
    fun `exact duplicates collapsed to one`() {
        val a1 = annotation(start = 0, end = 10, priority = 100)
        val a2 = annotation(start = 0, end = 10, priority = 100)
        val result = AnnotationMerger.merge(listOf(a1, a2))

        assertEquals(1, result.size)
    }

    // ==================== Complex Scenarios ====================

    @Test
    fun `three overlapping annotations resolved correctly`() {
        val high = annotation(start = 5, end = 15, priority = 300)
        val mid = annotation(start = 0, end = 10, priority = 200)
        val low = annotation(start = 10, end = 20, priority = 100)
        val result = AnnotationMerger.merge(listOf(low, mid, high))

        // high wins [5,15), mid gets [0,5), low gets [15,20)
        assertTrue(result.any { it.priority == 300 && it.startIndex == 5 && it.endIndex == 15 })
        assertTrue(result.any { it.priority == 200 && it.startIndex == 0 && it.endIndex == 5 })
        assertTrue(result.any { it.priority == 100 && it.startIndex == 15 && it.endIndex == 20 })
    }

    @Test
    fun `adjacent non-overlapping annotations preserved`() {
        val a1 = annotation(start = 0, end = 5)
        val a2 = annotation(start = 5, end = 10)
        val result = AnnotationMerger.merge(listOf(a1, a2))

        assertEquals(2, result.size)
    }
}
