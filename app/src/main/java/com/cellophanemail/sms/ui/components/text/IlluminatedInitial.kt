package com.cellophanemail.sms.ui.components.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cellophanemail.sms.domain.model.IlluminatedStyle

@Composable
fun IlluminatedInitial(
    letter: Char,
    style: IlluminatedStyle,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val letterColor = style.letterColor(isDark)
    val ornamentColor = style.ornamentColor(isDark)
    val fontFamily = remember(style.fontResId) { FontFamily(Font(style.fontResId)) }

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ornament background
        Image(
            painter = painterResource(style.ornamentResId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ornamentColor),
            modifier = Modifier.size(48.dp)
        )

        // Decorative letter
        Text(
            text = letter.uppercase(),
            fontFamily = fontFamily,
            fontSize = 28.sp,
            color = letterColor
        )
    }
}
