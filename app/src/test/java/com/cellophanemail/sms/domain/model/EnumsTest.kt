package com.cellophanemail.sms.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for domain enums: ProcessingState, ToxicityClass, Horseman
 */
class EnumsTest {

    // ==================== ProcessingState Tests ====================

    @Test
    fun `ProcessingState fromString parses valid values`() {
        assertEquals(ProcessingState.PENDING, ProcessingState.fromString("PENDING"))
        assertEquals(ProcessingState.PROCESSING, ProcessingState.fromString("PROCESSING"))
        assertEquals(ProcessingState.FILTERED, ProcessingState.fromString("FILTERED"))
        assertEquals(ProcessingState.SAFE, ProcessingState.fromString("SAFE"))
        assertEquals(ProcessingState.ERROR, ProcessingState.fromString("ERROR"))
    }

    @Test
    fun `ProcessingState fromString is case insensitive`() {
        assertEquals(ProcessingState.PENDING, ProcessingState.fromString("pending"))
        assertEquals(ProcessingState.SAFE, ProcessingState.fromString("Safe"))
        assertEquals(ProcessingState.ERROR, ProcessingState.fromString("eRrOr"))
    }

    @Test
    fun `ProcessingState fromString returns PENDING for unknown values`() {
        assertEquals(ProcessingState.PENDING, ProcessingState.fromString("UNKNOWN"))
        assertEquals(ProcessingState.PENDING, ProcessingState.fromString(""))
        assertEquals(ProcessingState.PENDING, ProcessingState.fromString("invalid"))
    }

    @Test
    fun `ProcessingState has all expected values`() {
        val states = ProcessingState.entries

        assertEquals(5, states.size)
        assertTrue(states.contains(ProcessingState.PENDING))
        assertTrue(states.contains(ProcessingState.PROCESSING))
        assertTrue(states.contains(ProcessingState.FILTERED))
        assertTrue(states.contains(ProcessingState.SAFE))
        assertTrue(states.contains(ProcessingState.ERROR))
    }

    // ==================== ToxicityClass Tests ====================

    @Test
    fun `ToxicityClass fromString parses valid values`() {
        assertEquals(ToxicityClass.SAFE, ToxicityClass.fromString("SAFE"))
        assertEquals(ToxicityClass.WARNING, ToxicityClass.fromString("WARNING"))
        assertEquals(ToxicityClass.HARMFUL, ToxicityClass.fromString("HARMFUL"))
        assertEquals(ToxicityClass.ABUSIVE, ToxicityClass.fromString("ABUSIVE"))
    }

    @Test
    fun `ToxicityClass fromString is case insensitive`() {
        assertEquals(ToxicityClass.SAFE, ToxicityClass.fromString("safe"))
        assertEquals(ToxicityClass.HARMFUL, ToxicityClass.fromString("Harmful"))
        assertEquals(ToxicityClass.ABUSIVE, ToxicityClass.fromString("aBuSiVe"))
    }

    @Test
    fun `ToxicityClass fromString returns null for unknown values`() {
        assertNull(ToxicityClass.fromString("UNKNOWN"))
        assertNull(ToxicityClass.fromString(""))
        assertNull(ToxicityClass.fromString("invalid"))
    }

    @Test
    fun `ToxicityClass fromString returns null for null input`() {
        assertNull(ToxicityClass.fromString(null))
    }

    @Test
    fun `ToxicityClass has all expected values`() {
        val classes = ToxicityClass.entries

        assertEquals(4, classes.size)
        assertTrue(classes.contains(ToxicityClass.SAFE))
        assertTrue(classes.contains(ToxicityClass.WARNING))
        assertTrue(classes.contains(ToxicityClass.HARMFUL))
        assertTrue(classes.contains(ToxicityClass.ABUSIVE))
    }

    @Test
    fun `ToxicityClass ordinal represents severity order`() {
        // SAFE < WARNING < HARMFUL < ABUSIVE
        assertTrue(ToxicityClass.SAFE.ordinal < ToxicityClass.WARNING.ordinal)
        assertTrue(ToxicityClass.WARNING.ordinal < ToxicityClass.HARMFUL.ordinal)
        assertTrue(ToxicityClass.HARMFUL.ordinal < ToxicityClass.ABUSIVE.ordinal)
    }

    // ==================== Horseman Tests ====================

    @Test
    fun `Horseman fromString parses valid values`() {
        assertEquals(Horseman.CRITICISM, Horseman.fromString("CRITICISM"))
        assertEquals(Horseman.CONTEMPT, Horseman.fromString("CONTEMPT"))
        assertEquals(Horseman.DEFENSIVENESS, Horseman.fromString("DEFENSIVENESS"))
        assertEquals(Horseman.STONEWALLING, Horseman.fromString("STONEWALLING"))
    }

    @Test
    fun `Horseman fromString is case insensitive`() {
        assertEquals(Horseman.CRITICISM, Horseman.fromString("criticism"))
        assertEquals(Horseman.CONTEMPT, Horseman.fromString("Contempt"))
        assertEquals(Horseman.DEFENSIVENESS, Horseman.fromString("dEfEnSiVeNeSs"))
    }

    @Test
    fun `Horseman fromString returns null for unknown values`() {
        assertNull(Horseman.fromString("UNKNOWN"))
        assertNull(Horseman.fromString(""))
        assertNull(Horseman.fromString("invalid"))
    }

    @Test
    fun `Horseman fromList parses valid list`() {
        val input = listOf("CRITICISM", "CONTEMPT", "UNKNOWN", "STONEWALLING")
        val result = Horseman.fromList(input)

        assertEquals(3, result.size)
        assertTrue(result.contains(Horseman.CRITICISM))
        assertTrue(result.contains(Horseman.CONTEMPT))
        assertTrue(result.contains(Horseman.STONEWALLING))
    }

    @Test
    fun `Horseman fromList handles empty list`() {
        val result = Horseman.fromList(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Horseman fromList filters out invalid values`() {
        val input = listOf("INVALID1", "INVALID2", "NOT_A_HORSEMAN")
        val result = Horseman.fromList(input)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Horseman has all Four Horsemen values`() {
        val horsemen = Horseman.entries

        // The Four Horsemen of relationship conflict
        assertEquals(4, horsemen.size)
        assertTrue(horsemen.contains(Horseman.CRITICISM))
        assertTrue(horsemen.contains(Horseman.CONTEMPT))
        assertTrue(horsemen.contains(Horseman.DEFENSIVENESS))
        assertTrue(horsemen.contains(Horseman.STONEWALLING))
    }
}
