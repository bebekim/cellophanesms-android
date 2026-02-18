package com.cellophanemail.sms.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NerModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NerProviderPreferences
) {
    private val modelDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val modelPath: File
        get() = File(modelDir, MODEL_FILENAME)

    fun modelExists(): Boolean = modelPath.let { it.exists() && it.length() > 0 }

    fun modelSizeBytes(): Long = if (modelExists()) modelPath.length() else 0L

    fun deleteModel() {
        modelPath.delete()
        preferences.setModelDownloaded(false)
    }

    companion object {
        const val MODEL_FILENAME = "qwen3-0.6b-q4_k_m.gguf"
        const val MODEL_SIZE_BYTES = 503_316_480L // ~480MB
        // Checksum verification is skipped when empty. Set this before production release
        // to prevent loading corrupted or tampered GGUF files.
        const val MODEL_SHA256 = ""
    }
}
