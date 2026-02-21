package com.cellophanemail.sms.ui.components.text

import androidx.compose.ui.graphics.Color
import com.cellophanemail.sms.ui.theme.ToneCasual
import com.cellophanemail.sms.ui.theme.ToneCasualDark
import com.cellophanemail.sms.ui.theme.ToneContemplative
import com.cellophanemail.sms.ui.theme.ToneContemplativeDark
import com.cellophanemail.sms.ui.theme.ToneFormal
import com.cellophanemail.sms.ui.theme.ToneFormalDark
import com.cellophanemail.sms.ui.theme.ToneUrgent
import com.cellophanemail.sms.ui.theme.ToneUrgentDark
import com.cellophanemail.sms.ui.theme.ToneWarm
import com.cellophanemail.sms.ui.theme.ToneWarmDark
import org.junit.Assert.assertEquals
import org.junit.Test

class ToneBadgeTest {

    // ==================== Light Theme ====================

    @Test
    fun `warm tone maps to ToneWarm in light mode`() {
        assertEquals(ToneWarm, toneColor("warm", isDark = false))
    }

    @Test
    fun `formal tone maps to ToneFormal in light mode`() {
        assertEquals(ToneFormal, toneColor("formal", isDark = false))
    }

    @Test
    fun `casual tone maps to ToneCasual in light mode`() {
        assertEquals(ToneCasual, toneColor("casual", isDark = false))
    }

    @Test
    fun `contemplative tone maps to ToneContemplative in light mode`() {
        assertEquals(ToneContemplative, toneColor("contemplative", isDark = false))
    }

    @Test
    fun `urgent tone maps to ToneUrgent in light mode`() {
        assertEquals(ToneUrgent, toneColor("urgent", isDark = false))
    }

    // ==================== Dark Theme ====================

    @Test
    fun `warm tone maps to ToneWarmDark in dark mode`() {
        assertEquals(ToneWarmDark, toneColor("warm", isDark = true))
    }

    @Test
    fun `formal tone maps to ToneFormalDark in dark mode`() {
        assertEquals(ToneFormalDark, toneColor("formal", isDark = true))
    }

    @Test
    fun `casual tone maps to ToneCasualDark in dark mode`() {
        assertEquals(ToneCasualDark, toneColor("casual", isDark = true))
    }

    @Test
    fun `contemplative tone maps to ToneContemplativeDark in dark mode`() {
        assertEquals(ToneContemplativeDark, toneColor("contemplative", isDark = true))
    }

    @Test
    fun `urgent tone maps to ToneUrgentDark in dark mode`() {
        assertEquals(ToneUrgentDark, toneColor("urgent", isDark = true))
    }

    // ==================== Unknown / Fallback ====================

    @Test
    fun `unknown tone returns grey fallback in light mode`() {
        assertEquals(Color(0xFF757575), toneColor("unknown_tone", isDark = false))
    }

    @Test
    fun `unknown tone returns grey fallback in dark mode`() {
        assertEquals(Color(0xFF9E9E9E), toneColor("unknown_tone", isDark = true))
    }

    // ==================== Case Insensitivity ====================

    @Test
    fun `tone matching is case insensitive`() {
        assertEquals(ToneWarm, toneColor("WARM", isDark = false))
        assertEquals(ToneFormal, toneColor("Formal", isDark = false))
        assertEquals(ToneCasual, toneColor("CASUAL", isDark = false))
    }
}
