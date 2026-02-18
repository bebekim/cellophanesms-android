package com.cellophanemail.sms.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color
import com.cellophanemail.sms.R

data class IlluminatedStyle(
    val id: String,
    val displayName: String,
    @FontRes val fontResId: Int,
    @DrawableRes val ornamentResId: Int,
    val letterColorLight: Color,
    val ornamentColorLight: Color,
    val letterColorDark: Color,
    val ornamentColorDark: Color,
    val spanLines: Int = 3
) {
    fun letterColor(isDark: Boolean): Color =
        if (isDark) letterColorDark else letterColorLight

    fun ornamentColor(isDark: Boolean): Color =
        if (isDark) ornamentColorDark else ornamentColorLight
}

enum class IlluminatedStylePack(val style: IlluminatedStyle) {
    CLASSIC_ILLUMINATED(
        IlluminatedStyle(
            id = "classic",
            displayName = "Classic Illuminated",
            fontResId = R.font.cinzel_decorative_bold,
            ornamentResId = R.drawable.ic_ornament_classic,
            letterColorLight = Color(0xFF1A237E),   // Deep indigo
            ornamentColorLight = Color(0xFFB8860B), // Dark goldenrod
            letterColorDark = Color(0xFF9FA8DA),    // Light indigo
            ornamentColorDark = Color(0xFFFFD54F)   // Light gold
        )
    ),
    GOTHIC_INK(
        IlluminatedStyle(
            id = "gothic",
            displayName = "Gothic Ink",
            fontResId = R.font.unifraktur_maguntia_regular,
            ornamentResId = R.drawable.ic_ornament_gothic,
            letterColorLight = Color(0xFF212121),   // Near black
            ornamentColorLight = Color(0xFF424242), // Dark gray
            letterColorDark = Color(0xFFE0E0E0),    // Light gray
            ornamentColorDark = Color(0xFF9E9E9E)   // Medium gray
        )
    ),
    CELTIC_KNOT(
        IlluminatedStyle(
            id = "celtic",
            displayName = "Celtic Knot",
            fontResId = R.font.medieval_sharp_regular,
            ornamentResId = R.drawable.ic_ornament_celtic,
            letterColorLight = Color(0xFF1B5E20),   // Deep green
            ornamentColorLight = Color(0xFF33691E), // Dark lime
            letterColorDark = Color(0xFF81C784),    // Light green
            ornamentColorDark = Color(0xFFA5D6A7)   // Pastel green
        )
    ),
    GOLD_LEAF(
        IlluminatedStyle(
            id = "gold_leaf",
            displayName = "Gold Leaf",
            fontResId = R.font.playfair_display_sc_bold,
            ornamentResId = R.drawable.ic_ornament_gold_leaf,
            letterColorLight = Color(0xFFB8860B),   // Dark goldenrod
            ornamentColorLight = Color(0xFFDAA520), // Goldenrod
            letterColorDark = Color(0xFFFFD54F),    // Light gold
            ornamentColorDark = Color(0xFFFFE082)   // Pale gold
        )
    ),
    MINIMAL_FANCY(
        IlluminatedStyle(
            id = "minimal",
            displayName = "Minimal Fancy",
            fontResId = R.font.cormorant_garamond_bold,
            ornamentResId = R.drawable.ic_ornament_minimal,
            letterColorLight = Color(0xFF37474F),   // Blue gray 800
            ornamentColorLight = Color(0xFF78909C), // Blue gray 400
            letterColorDark = Color(0xFFB0BEC5),    // Blue gray 200
            ornamentColorDark = Color(0xFF546E7A)   // Blue gray 600
        )
    );

    companion object {
        fun fromId(id: String): IlluminatedStylePack =
            entries.find { it.style.id == id } ?: CLASSIC_ILLUMINATED
    }
}
