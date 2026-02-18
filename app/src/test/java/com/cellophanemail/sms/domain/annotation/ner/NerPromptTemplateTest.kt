package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.domain.model.AnnotationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NerPromptTemplateTest {

    // ==================== Valid JSON Parsing ====================

    @Test
    fun `parses valid entity response`() {
        val json = """{"entities": [{"text": "John", "type": "PERSON_NAME", "start": 5, "end": 9}]}"""
        val text = "Meet John at the park"
        val result = NerPromptTemplate.parseResponse(json, text)

        assertEquals(1, result.size)
        assertEquals("John", result[0].text)
        assertEquals(AnnotationType.PERSON_NAME, result[0].type)
        assertEquals(5, result[0].startIndex)
        assertEquals(9, result[0].endIndex)
    }

    @Test
    fun `parses multiple entities`() {
        val json = """
            {"entities": [
                {"text": "John", "type": "PERSON_NAME", "start": 5, "end": 9},
                {"text": "Google", "type": "ORGANIZATION", "start": 13, "end": 19},
                {"text": "Mountain View", "type": "LOCATION", "start": 24, "end": 37}
            ]}
        """.trimIndent()
        val text = "Meet John at Google in Mountain View tomorrow"
        val result = NerPromptTemplate.parseResponse(json, text)

        assertEquals(3, result.size)
        assertEquals(AnnotationType.PERSON_NAME, result[0].type)
        assertEquals(AnnotationType.ORGANIZATION, result[1].type)
        assertEquals(AnnotationType.LOCATION, result[2].type)
    }

    @Test
    fun `parses empty entities array`() {
        val json = """{"entities": []}"""
        val result = NerPromptTemplate.parseResponse(json, "no entities here")

        assertTrue(result.isEmpty())
    }

    // ==================== Type Aliases ====================

    @Test
    fun `accepts PERSON alias`() {
        val json = """{"entities": [{"text": "John", "type": "PERSON", "start": 0, "end": 4}]}"""
        val result = NerPromptTemplate.parseResponse(json, "John went home")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.PERSON_NAME, result[0].type)
    }

    @Test
    fun `accepts ORG alias`() {
        val json = """{"entities": [{"text": "Google", "type": "ORG", "start": 0, "end": 6}]}"""
        val result = NerPromptTemplate.parseResponse(json, "Google is great")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.ORGANIZATION, result[0].type)
    }

    @Test
    fun `accepts LOC alias`() {
        val json = """{"entities": [{"text": "Paris", "type": "LOC", "start": 0, "end": 5}]}"""
        val result = NerPromptTemplate.parseResponse(json, "Paris is lovely")

        assertEquals(1, result.size)
        assertEquals(AnnotationType.LOCATION, result[0].type)
    }

    // ==================== Malformed Input ====================

    @Test
    fun `returns empty for invalid JSON`() {
        val result = NerPromptTemplate.parseResponse("not json at all", "some text")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty for missing entities key`() {
        val json = """{"results": []}"""
        val result = NerPromptTemplate.parseResponse(json, "some text")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles markdown code fence wrapping`() {
        val json = """
            ```json
            {"entities": [{"text": "John", "type": "PERSON_NAME", "start": 0, "end": 4}]}
            ```
        """.trimIndent()
        val result = NerPromptTemplate.parseResponse(json, "John went home")

        assertEquals(1, result.size)
    }

    @Test
    fun `skips unknown entity types`() {
        val json = """{"entities": [
            {"text": "test", "type": "UNKNOWN_TYPE", "start": 0, "end": 4},
            {"text": "John", "type": "PERSON_NAME", "start": 5, "end": 9}
        ]}"""
        val result = NerPromptTemplate.parseResponse(json, "test John went home")

        assertEquals(1, result.size)
        assertEquals("John", result[0].text)
    }

    @Test
    fun `skips entities with empty text`() {
        val json = """{"entities": [{"text": "", "type": "PERSON_NAME", "start": 0, "end": 0}]}"""
        val result = NerPromptTemplate.parseResponse(json, "some text")

        assertTrue(result.isEmpty())
    }

    // ==================== Index Validation ====================

    @Test
    fun `falls back to substring search when indices are invalid`() {
        val json = """{"entities": [{"text": "John", "type": "PERSON_NAME", "start": -1, "end": -1}]}"""
        val text = "Meet John at the park"
        val result = NerPromptTemplate.parseResponse(json, text)

        assertEquals(1, result.size)
        assertEquals(5, result[0].startIndex)
        assertEquals(9, result[0].endIndex)
    }

    @Test
    fun `skips entities not found in text`() {
        val json = """{"entities": [{"text": "NotInText", "type": "PERSON_NAME", "start": -1, "end": -1}]}"""
        val result = NerPromptTemplate.parseResponse(json, "Meet John at the park")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `skips entities with out-of-bounds indices`() {
        val json = """{"entities": [{"text": "John", "type": "PERSON_NAME", "start": 100, "end": 104}]}"""
        val result = NerPromptTemplate.parseResponse(json, "John")

        // start 100 is out of bounds for "John" (length 4), falls back to substring search
        assertEquals(1, result.size)
        assertEquals(0, result[0].startIndex)
    }

    // ==================== Prompt Building ====================

    @Test
    fun `buildPrompt includes text`() {
        val prompt = NerPromptTemplate.buildPrompt("Hello world")
        assertTrue(prompt.contains("Hello world"))
    }

    @Test
    fun `buildPromptWithNoThink includes nothink prefix`() {
        val prompt = NerPromptTemplate.buildPromptWithNoThink("Hello world")
        assertTrue(prompt.startsWith("/nothink"))
        assertTrue(prompt.contains("Hello world"))
    }
}
