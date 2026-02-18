package com.cellophanemail.sms.domain.annotation.ner

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoNerProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : NerProvider {

    override val providerId: String = PROVIDER_ID
    override val requiresNetwork: Boolean = false

    private val probeMutex = Mutex()
    private var model: GenerativeModel? = null
    @Volatile private var availabilityChecked = false
    @Volatile private var isDeviceSupported = false

    override suspend fun isAvailable(): Boolean {
        if (availabilityChecked) return isDeviceSupported
        return probeMutex.withLock {
            // Double-check after acquiring lock
            if (availabilityChecked) return@withLock isDeviceSupported
            try {
                val testModel = GenerativeModel(
                    modelName = "gemini-nano",
                    apiKey = "",
                    generationConfig = generationConfig {
                        temperature = 0f
                        maxOutputTokens = 256
                    }
                )
                testModel.generateContent("test")
                model = testModel
                isDeviceSupported = true
                availabilityChecked = true
                true
            } catch (_: Exception) {
                isDeviceSupported = false
                availabilityChecked = true
                false
            }
        }
    }

    override suspend fun extractEntities(text: String): List<NerEntity> {
        val activeModel = model ?: throw IllegalStateException("Gemini Nano not available")
        val prompt = NerPromptTemplate.buildPrompt(text)

        val response = activeModel.generateContent(prompt)
        val responseText = response.text ?: return emptyList()

        return NerPromptTemplate.parseResponse(responseText, text)
    }

    companion object {
        const val PROVIDER_ID = "gemini_nano"
    }
}
