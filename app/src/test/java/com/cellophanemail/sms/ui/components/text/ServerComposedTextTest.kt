package com.cellophanemail.sms.ui.components.text

import com.cellophanemail.sms.domain.model.AnnotationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerComposedTextTest {

    // ==================== Style → AnnotationType Mapping ====================

    @Test
    fun `person_name style maps to PERSON_NAME`() {
        assertEquals(AnnotationType.PERSON_NAME, parseDecorationStyle("person_name"))
    }

    @Test
    fun `location style maps to LOCATION`() {
        assertEquals(AnnotationType.LOCATION, parseDecorationStyle("location"))
    }

    @Test
    fun `organization style maps to ORGANIZATION`() {
        assertEquals(AnnotationType.ORGANIZATION, parseDecorationStyle("organization"))
    }

    @Test
    fun `date_time style maps to DATE_TIME`() {
        assertEquals(AnnotationType.DATE_TIME, parseDecorationStyle("date_time"))
    }

    @Test
    fun `url style maps to URL`() {
        assertEquals(AnnotationType.URL, parseDecorationStyle("url"))
    }

    @Test
    fun `email style maps to EMAIL`() {
        assertEquals(AnnotationType.EMAIL, parseDecorationStyle("email"))
    }

    @Test
    fun `phone_number style maps to PHONE_NUMBER`() {
        assertEquals(AnnotationType.PHONE_NUMBER, parseDecorationStyle("phone_number"))
    }

    @Test
    fun `unknown style returns null`() {
        assertNull(parseDecorationStyle("emphasis"))
        assertNull(parseDecorationStyle("unknown"))
        assertNull(parseDecorationStyle(""))
    }
}
