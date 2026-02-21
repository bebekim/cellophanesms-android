package com.cellophanemail.sms.ui.components.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
fun ToneBadge(
    tone: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val toneColor = toneColor(tone, isDark)

    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = tone.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = toneColor
            )
        },
        modifier = modifier.padding(top = 4.dp),
        border = BorderStroke(1.dp, toneColor.copy(alpha = 0.3f)),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = toneColor.copy(alpha = 0.12f)
        )
    )
}

fun toneColor(tone: String, isDark: Boolean): Color {
    return when (tone.lowercase()) {
        "warm" -> if (isDark) ToneWarmDark else ToneWarm
        "formal" -> if (isDark) ToneFormalDark else ToneFormal
        "casual" -> if (isDark) ToneCasualDark else ToneCasual
        "contemplative" -> if (isDark) ToneContemplativeDark else ToneContemplative
        "urgent" -> if (isDark) ToneUrgentDark else ToneUrgent
        else -> if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575) // neutral grey fallback
    }
}
