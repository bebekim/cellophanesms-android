package com.cellophanemail.sms.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cellophanemail.sms.domain.model.IlluminatedStylePack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextRenderingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _illuminatedEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ILLUMINATED_ENABLED, false)
    )
    val illuminatedEnabled: StateFlow<Boolean> = _illuminatedEnabled.asStateFlow()

    private val _selectedStylePack = MutableStateFlow(
        IlluminatedStylePack.fromId(
            prefs.getString(KEY_STYLE_PACK, IlluminatedStylePack.CLASSIC_ILLUMINATED.style.id) ?: ""
        )
    )
    val selectedStylePack: StateFlow<IlluminatedStylePack> = _selectedStylePack.asStateFlow()

    private val _entityHighlightsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ENTITY_HIGHLIGHTS, true)
    )
    val entityHighlightsEnabled: StateFlow<Boolean> = _entityHighlightsEnabled.asStateFlow()

    fun setIlluminatedEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ILLUMINATED_ENABLED, enabled).apply()
        _illuminatedEnabled.value = enabled
    }

    fun setSelectedStylePack(pack: IlluminatedStylePack) {
        prefs.edit().putString(KEY_STYLE_PACK, pack.style.id).apply()
        _selectedStylePack.value = pack
    }

    fun setEntityHighlightsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENTITY_HIGHLIGHTS, enabled).apply()
        _entityHighlightsEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "text_rendering_prefs"
        private const val KEY_ILLUMINATED_ENABLED = "illuminated_enabled"
        private const val KEY_STYLE_PACK = "style_pack"
        private const val KEY_ENTITY_HIGHLIGHTS = "entity_highlights"
    }
}
