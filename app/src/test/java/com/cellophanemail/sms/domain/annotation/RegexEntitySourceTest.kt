package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.AnnotationType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegexEntitySourceTest {

    private lateinit var source: RegexEntitySource

    @Before
    fun setUp() {
        source = RegexEntitySource()
    }

    // ==================== URL Detection ====================

    @Test
    fun `detects https URL`() = runTest {
        val result = source.annotate("Visit https://example.com for details")
        val urls = result.filter { it.type == AnnotationType.URL }
        assertEquals(1, urls.size)
        assertEquals("https://example.com", urls[0].metadata["matched"])
    }

    @Test
    fun `detects http URL`() = runTest {
        val result = source.annotate("Go to http://test.org/page")
        val urls = result.filter { it.type == AnnotationType.URL }
        assertEquals(1, urls.size)
        assertEquals("http://test.org/page", urls[0].metadata["matched"])
    }

    @Test
    fun `detects www URL`() = runTest {
        val result = source.annotate("Check www.example.com/path?q=1")
        val urls = result.filter { it.type == AnnotationType.URL }
        assertEquals(1, urls.size)
    }

    @Test
    fun `detects URL with path and query`() = runTest {
        val result = source.annotate("Link: https://example.com/foo/bar?a=1&b=2")
        val urls = result.filter { it.type == AnnotationType.URL }
        assertEquals(1, urls.size)
    }

    // ==================== Email Detection ====================

    @Test
    fun `detects email address`() = runTest {
        val result = source.annotate("Email me at user@example.com please")
        val emails = result.filter { it.type == AnnotationType.EMAIL }
        assertEquals(1, emails.size)
        assertEquals("user@example.com", emails[0].metadata["matched"])
    }

    @Test
    fun `detects email with dots and hyphens`() = runTest {
        val result = source.annotate("Send to first.last@sub-domain.example.co.uk")
        val emails = result.filter { it.type == AnnotationType.EMAIL }
        assertEquals(1, emails.size)
    }

    @Test
    fun `email takes precedence over URL match`() = runTest {
        val result = source.annotate("Contact user@example.com for info")
        // Email should be detected, not URL
        val emails = result.filter { it.type == AnnotationType.EMAIL }
        val urls = result.filter { it.type == AnnotationType.URL }
        assertEquals(1, emails.size)
        assertEquals(0, urls.size)
    }

    // ==================== Phone Number Detection ====================

    @Test
    fun `detects US phone number`() = runTest {
        val result = source.annotate("Call me at (555) 123-4567")
        val phones = result.filter { it.type == AnnotationType.PHONE_NUMBER }
        assertEquals(1, phones.size)
    }

    @Test
    fun `detects international phone number`() = runTest {
        val result = source.annotate("Reach me at +1 555-123-4567")
        val phones = result.filter { it.type == AnnotationType.PHONE_NUMBER }
        assertEquals(1, phones.size)
    }

    @Test
    fun `detects phone with dots`() = runTest {
        val result = source.annotate("Number: 555.123.4567")
        val phones = result.filter { it.type == AnnotationType.PHONE_NUMBER }
        assertEquals(1, phones.size)
    }

    // ==================== Date/Time Detection ====================

    @Test
    fun `detects MM-DD-YYYY date`() = runTest {
        val result = source.annotate("Meeting on 12/25/2024")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    @Test
    fun `detects month name date`() = runTest {
        val result = source.annotate("Due by January 15, 2025")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    @Test
    fun `detects abbreviated month date`() = runTest {
        val result = source.annotate("Start on Mar 3")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    @Test
    fun `detects relative dates`() = runTest {
        val result = source.annotate("Let's meet tomorrow at the park")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
        assertEquals("tomorrow", dates[0].metadata["matched"])
    }

    @Test
    fun `detects today and tonight`() = runTest {
        val todayResult = source.annotate("Available today")
        val tonightResult = source.annotate("See you tonight")
        assertEquals(1, todayResult.filter { it.type == AnnotationType.DATE_TIME }.size)
        assertEquals(1, tonightResult.filter { it.type == AnnotationType.DATE_TIME }.size)
    }

    @Test
    fun `detects next weekday`() = runTest {
        val result = source.annotate("Let's reschedule to next Monday")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    @Test
    fun `detects 12-hour time`() = runTest {
        val result = source.annotate("Appointment at 2:30 PM")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    @Test
    fun `detects 24-hour time`() = runTest {
        val result = source.annotate("Starts at 14:30")
        val dates = result.filter { it.type == AnnotationType.DATE_TIME }
        assertEquals(1, dates.size)
    }

    // ==================== Multiple Entities ====================

    @Test
    fun `detects multiple different entity types`() = runTest {
        val text = "Call 555-123-4567 or email test@example.com by tomorrow"
        val result = source.annotate(text)

        assertTrue(result.any { it.type == AnnotationType.PHONE_NUMBER })
        assertTrue(result.any { it.type == AnnotationType.EMAIL })
        assertTrue(result.any { it.type == AnnotationType.DATE_TIME })
    }

    @Test
    fun `detects multiple entities of same type`() = runTest {
        val text = "Email user1@example.com or user2@example.com"
        val result = source.annotate(text)
        val emails = result.filter { it.type == AnnotationType.EMAIL }
        assertEquals(2, emails.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `returns empty for blank input`() = runTest {
        assertEquals(emptyList<Any>(), source.annotate(""))
        assertEquals(emptyList<Any>(), source.annotate("  "))
    }

    @Test
    fun `returns empty for plain text without entities`() = runTest {
        val result = source.annotate("Hello, how are you doing?")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `all annotations have confidence 1`() = runTest {
        val result = source.annotate("Visit https://x.com at 2:30 PM")
        assertTrue(result.all { it.confidence == 1.0f })
    }

    @Test
    fun `all annotations have correct source id`() = runTest {
        val result = source.annotate("Call 555-123-4567")
        assertTrue(result.all { it.source == RegexEntitySource.SOURCE_ID })
    }

    @Test
    fun `annotation indices are within text bounds`() = runTest {
        val text = "Check https://example.com and call 555-123-4567"
        val result = source.annotate(text)

        for (annotation in result) {
            assertTrue(annotation.startIndex >= 0)
            assertTrue(annotation.endIndex <= text.length)
            assertTrue(annotation.startIndex < annotation.endIndex)
        }
    }

    // ==================== Performance ====================

    @Test
    fun `processes 500-char text in under 50ms`() = runTest {
        val text = buildString {
            append("Contact us at info@company.com or call (800) 555-0199. ")
            append("Visit https://www.company.com/support for help. ")
            append("Meeting scheduled for January 15, 2025 at 3:00 PM. ")
            while (length < 500) append("Additional filler text to reach the target length. ")
        }

        val start = System.nanoTime()
        source.annotate(text)
        val durationMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("Expected <50ms, got ${durationMs}ms", durationMs < 50)
    }
}
