package com.cellophanemail.sms.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnnotationTest {

    private fun annotation(start: Int, end: Int) = TextAnnotation(
        type = AnnotationType.URL,
        startIndex = start,
        endIndex = end
    )

    @Test
    fun `length returns correct span length`() {
        val ann = annotation(5, 15)
        assertEquals(10, ann.length)
    }

    @Test
    fun `length is zero for empty span`() {
        val ann = annotation(5, 5)
        assertEquals(0, ann.length)
    }

    @Test
    fun `overlaps detects overlapping spans`() {
        val a = annotation(0, 10)
        val b = annotation(5, 15)
        assertTrue(a.overlaps(b))
        assertTrue(b.overlaps(a))
    }

    @Test
    fun `overlaps detects containment`() {
        val outer = annotation(0, 20)
        val inner = annotation(5, 10)
        assertTrue(outer.overlaps(inner))
        assertTrue(inner.overlaps(outer))
    }

    @Test
    fun `overlaps returns false for adjacent spans`() {
        val a = annotation(0, 5)
        val b = annotation(5, 10)
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))
    }

    @Test
    fun `overlaps returns false for disjoint spans`() {
        val a = annotation(0, 5)
        val b = annotation(10, 15)
        assertFalse(a.overlaps(b))
        assertFalse(b.overlaps(a))
    }

    @Test
    fun `default values are sensible`() {
        val ann = TextAnnotation(
            type = AnnotationType.EMAIL,
            startIndex = 0,
            endIndex = 10
        )
        assertEquals("", ann.label)
        assertEquals(1.0f, ann.confidence)
        assertEquals("", ann.source)
        assertEquals(0, ann.priority)
        assertTrue(ann.metadata.isEmpty())
        assertTrue(ann.id.isNotBlank())
    }

    @Test
    fun `all annotation types are accessible`() {
        val types = AnnotationType.entries
        assertTrue(types.contains(AnnotationType.DATE_TIME))
        assertTrue(types.contains(AnnotationType.URL))
        assertTrue(types.contains(AnnotationType.EMAIL))
        assertTrue(types.contains(AnnotationType.PHONE_NUMBER))
        assertTrue(types.contains(AnnotationType.PERSON_NAME))
        assertTrue(types.contains(AnnotationType.LOCATION))
        assertTrue(types.contains(AnnotationType.ORGANIZATION))
        assertTrue(types.contains(AnnotationType.TOXICITY_SPAN))
        assertTrue(types.contains(AnnotationType.HORSEMAN_SPAN))
    }

    @Test
    fun `AnnotatedMessageText plain factory creates empty annotations`() {
        val plain = AnnotatedMessageText.plain("Hello")
        assertEquals("Hello", plain.text)
        assertTrue(plain.annotations.isEmpty())
    }
}
