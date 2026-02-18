package com.cellophanemail.sms.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class NerProviderMode(val id: String) {
    AUTO("auto"),
    GEMINI_NANO("gemini_nano"),
    QWEN3_LOCAL("qwen3_local"),
    CLAUDE_CLOUD("claude_cloud"),
    OFF("off");

    companion object {
        fun fromId(id: String): NerProviderMode =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

@Singleton
class NerProviderPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedProvider = MutableStateFlow(
        NerProviderMode.fromId(prefs.getString(KEY_PROVIDER_MODE, NerProviderMode.AUTO.id) ?: "")
    )
    val selectedProvider: StateFlow<NerProviderMode> = _selectedProvider.asStateFlow()

    private val _modelDownloaded = MutableStateFlow(
        prefs.getBoolean(KEY_MODEL_DOWNLOADED, false)
    )
    val modelDownloaded: StateFlow<Boolean> = _modelDownloaded.asStateFlow()

    private val _wifiOnlyDownload = MutableStateFlow(
        prefs.getBoolean(KEY_WIFI_ONLY, true)
    )
    val wifiOnlyDownload: StateFlow<Boolean> = _wifiOnlyDownload.asStateFlow()

    fun setSelectedProvider(mode: NerProviderMode) {
        prefs.edit().putString(KEY_PROVIDER_MODE, mode.id).apply()
        _selectedProvider.value = mode
    }

    fun setModelDownloaded(downloaded: Boolean) {
        prefs.edit().putBoolean(KEY_MODEL_DOWNLOADED, downloaded).apply()
        _modelDownloaded.value = downloaded
    }

    fun setWifiOnlyDownload(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
        _wifiOnlyDownload.value = wifiOnly
    }

    companion object {
        private const val PREFS_NAME = "ner_provider_prefs"
        private const val KEY_PROVIDER_MODE = "ner_provider_mode"
        private const val KEY_MODEL_DOWNLOADED = "local_model_downloaded"
        private const val KEY_WIFI_ONLY = "model_download_wifi_only"
    }
}
