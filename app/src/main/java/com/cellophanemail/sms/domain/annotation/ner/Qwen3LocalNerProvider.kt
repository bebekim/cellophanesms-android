package com.cellophanemail.sms.domain.annotation.ner

import android.util.Log
import com.cellophanemail.sms.data.local.NerModelManager
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device NER via Qwen3-0.6B running through llama.cpp JNI.
 *
 * The model is loaded lazily on first extraction and kept in memory
 * until [release]/[close] is called. Uses non-thinking mode (/nothink) for speed.
 *
 * All access to [nativeContext] is synchronized via [lock] to prevent
 * use-after-free when [release] is called concurrently with [runInference].
 */
@Singleton
class Qwen3LocalNerProvider @Inject constructor(
    private val modelManager: NerModelManager
) : NerProvider, Closeable {

    override val providerId: String = PROVIDER_ID
    override val requiresNetwork: Boolean = false

    private val lock = Any()
    private var nativeContext: Long? = null

    override suspend fun isAvailable(): Boolean = modelManager.modelExists()

    override suspend fun extractEntities(text: String): List<NerEntity> {
        val responseText = synchronized(lock) {
            ensureModelLoaded()
            val prompt = NerPromptTemplate.buildPromptWithNoThink(text)
            runInference(prompt)
        }

        return NerPromptTemplate.parseResponse(responseText, text)
    }

    // Must be called while holding [lock]
    private fun ensureModelLoaded() {
        if (nativeContext != null) return
        val path = modelManager.modelPath.absolutePath
        nativeContext = llamaLoadModel(path)
    }

    // Must be called while holding [lock]
    private fun runInference(prompt: String): String {
        val ctx = nativeContext ?: throw IllegalStateException("Model not loaded")
        return llamaInfer(ctx, prompt, MAX_TOKENS, TEMPERATURE)
    }

    fun release() {
        synchronized(lock) {
            nativeContext?.let { ctx ->
                try {
                    llamaFreeModel(ctx)
                } catch (e: Exception) {
                    Log.w(TAG, "Error freeing model: ${e.message}")
                }
            }
            nativeContext = null
        }
    }

    override fun close() = release()

    companion object {
        const val PROVIDER_ID = "qwen3_local"
        private const val TAG = "Qwen3LocalNer"
        private const val MAX_TOKENS = 200
        private const val TEMPERATURE = 0f

        init {
            try {
                System.loadLibrary("llama")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "llama native library not available: ${e.message}")
            }
        }

        @JvmStatic
        private external fun llamaLoadModel(modelPath: String): Long

        @JvmStatic
        private external fun llamaInfer(
            context: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float
        ): String

        @JvmStatic
        private external fun llamaFreeModel(context: Long)
    }
}
