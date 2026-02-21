package com.cellophanemail.sms.data.remote.api

import com.cellophanemail.sms.data.remote.AnnotatedSpanDto
import com.cellophanemail.sms.data.remote.DocumentDto
import com.cellophanemail.sms.data.remote.RenderResponseDto
import com.cellophanemail.sms.data.remote.TextBlockDto
import com.cellophanemail.sms.data.remote.ToneBadgeDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderApiTest {

    private val gson = Gson()

    // ==================== DocumentDto ====================

    @Test
    fun `DocumentDto deserializes schema_version`() {
        val json = """{"schema_version": 1, "blocks": [], "tone": null}"""
        val doc = gson.fromJson(json, DocumentDto::class.java)
        assertEquals(1, doc.schemaVersion)
    }

    @Test
    fun `DocumentDto deserializes blocks with text`() {
        val json = """{
            "schema_version": 1,
            "blocks": [{"block_type": "text", "text": "Hello Sarah", "spans": []}],
            "tone": null
        }"""
        val doc = gson.fromJson(json, DocumentDto::class.java)
        assertEquals(1, doc.blocks.size)
        assertEquals("Hello Sarah", doc.blocks[0].text)
        assertEquals("text", doc.blocks[0].blockType)
    }

    @Test
    fun `DocumentDto deserializes tone badge`() {
        val json = """{
            "schema_version": 1,
            "blocks": [],
            "tone": {"tone": "warm", "confidence": 0.85}
        }"""
        val doc = gson.fromJson(json, DocumentDto::class.java)
        assertNotNull(doc.tone)
        assertEquals("warm", doc.tone?.tone)
        assertEquals(0.85f, doc.tone?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun `DocumentDto handles null tone`() {
        val json = """{"schema_version": 1, "blocks": [], "tone": null}"""
        val doc = gson.fromJson(json, DocumentDto::class.java)
        assertNull(doc.tone)
    }

    // ==================== AnnotatedSpanDto ====================

    @Test
    fun `AnnotatedSpanDto deserializes entity decoration`() {
        val json = """{
            "start": 6, "end": 11, "style": "person_name",
            "decoration": {"entity_type": "person_name", "confidence": 0.95, "tappable": true}
        }"""
        val span = gson.fromJson(json, AnnotatedSpanDto::class.java)
        assertEquals(6, span.start)
        assertEquals(11, span.end)
        assertEquals("person_name", span.style)

        val entity = span.asEntityDecoration()
        assertNotNull(entity)
        assertEquals("person_name", entity?.entityType)
        assertEquals(0.95f, entity?.confidence ?: 0f, 0.001f)
        assertTrue(entity?.tappable == true)
    }

    @Test
    fun `AnnotatedSpanDto deserializes link decoration`() {
        val json = """{
            "start": 0, "end": 20, "style": "url",
            "decoration": {"url": "https://example.com", "tappable": true}
        }"""
        val span = gson.fromJson(json, AnnotatedSpanDto::class.java)
        val link = span.asLinkDecoration()
        assertNotNull(link)
        assertEquals("https://example.com", link?.url)
    }

    // ==================== RenderResponseDto ====================

    @Test
    fun `RenderResponseDto deserializes full response`() {
        val json = """{
            "document": {
                "schema_version": 1,
                "blocks": [{
                    "block_type": "text",
                    "text": "Meet Sarah at the park",
                    "spans": [
                        {"start": 5, "end": 10, "style": "person_name",
                         "decoration": {"entity_type": "person_name", "confidence": 0.95, "tappable": true}},
                        {"start": 14, "end": 22, "style": "location",
                         "decoration": {"entity_type": "location", "confidence": 0.88, "tappable": true}}
                    ]
                }],
                "tone": {"tone": "casual", "confidence": 0.8}
            },
            "processing_time_ms": 42,
            "extractor_used": "mock",
            "channel": "sms"
        }"""
        val resp = gson.fromJson(json, RenderResponseDto::class.java)
        assertEquals(42, resp.processingTimeMs)
        assertEquals("mock", resp.extractorUsed)
        assertEquals("sms", resp.channel)
        assertEquals(1, resp.document.schemaVersion)
        assertEquals(2, resp.document.blocks[0].spans.size)
        assertEquals("casual", resp.document.tone?.tone)
    }

    @Test
    fun `RenderResponseDto roundtrip serialization`() {
        val original = RenderResponseDto(
            document = DocumentDto(
                schemaVersion = 1,
                blocks = listOf(
                    TextBlockDto(
                        text = "Hello",
                        spans = listOf(
                            AnnotatedSpanDto(
                                start = 0, end = 5, style = "person_name",
                                decoration = mapOf(
                                    "entity_type" to "person_name",
                                    "confidence" to 0.9,
                                    "tappable" to true
                                )
                            )
                        )
                    )
                ),
                tone = ToneBadgeDto(tone = "formal", confidence = 0.8f)
            ),
            processingTimeMs = 10,
            extractorUsed = "mock",
            channel = "sms"
        )
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, RenderResponseDto::class.java)
        assertEquals(original.document.schemaVersion, deserialized.document.schemaVersion)
        assertEquals(original.document.blocks.size, deserialized.document.blocks.size)
        assertEquals(original.processingTimeMs, deserialized.processingTimeMs)
    }
}
