package com.cellophanemail.sms.domain.annotation.ner

import android.util.Log
import com.cellophanemail.sms.data.local.NerProviderMode
import com.cellophanemail.sms.data.local.NerProviderPreferences
import com.cellophanemail.sms.domain.annotation.AnnotationSource
import com.cellophanemail.sms.domain.model.TextAnnotation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TieredNerAnnotationSource @Inject constructor(
    private val providers: List<@JvmSuppressWildcards NerProvider>,
    private val preferences: NerProviderPreferences
) : AnnotationSource {

    override val sourceId: String = SOURCE_ID
    override val defaultPriority: Int = 200
    override val requiresNetwork: Boolean = false

    override suspend fun annotate(text: String): List<TextAnnotation> {
        if (text.isBlank()) return emptyList()

        val mode = preferences.selectedProvider.value
        if (mode == NerProviderMode.OFF) return emptyList()

        val providersToTry = if (mode == NerProviderMode.AUTO) {
            providers.filter { runCatching { it.isAvailable() }.getOrDefault(false) }
        } else {
            providers.filter { it.providerId == mode.id }
                .filter { runCatching { it.isAvailable() }.getOrDefault(false) }
        }

        for (provider in providersToTry) {
            runCatching { provider.extractEntities(text) }
                .onSuccess { entities ->
                    return entities.map { it.toTextAnnotation(provider.providerId, defaultPriority) }
                }
                .onFailure { e ->
                    Log.w(TAG, "${provider.providerId} failed: ${e.message}")
                }
        }

        return emptyList()
    }

    companion object {
        const val SOURCE_ID = "tiered_ner"
        private const val TAG = "TieredNerSource"
    }
}
