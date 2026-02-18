package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.data.remote.NerEntityDto
import com.cellophanemail.sms.data.remote.NerExtractionApi
import com.cellophanemail.sms.data.remote.NerExtractionRequest
import com.cellophanemail.sms.util.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeCloudNerProvider @Inject constructor(
    private val api: NerExtractionApi,
    private val networkMonitor: NetworkMonitor
) : NerProvider {

    override val providerId: String = PROVIDER_ID
    override val requiresNetwork: Boolean = true

    override suspend fun isAvailable(): Boolean = networkMonitor.isConnected.value

    override suspend fun extractEntities(text: String): List<NerEntity> {
        val response = api.extractEntities(NerExtractionRequest(text))

        if (!response.isSuccessful) {
            throw RuntimeException("NER API returned ${response.code()}: ${response.message()}")
        }

        val body = response.body() ?: return emptyList()
        return body.entities.mapNotNull { it.toNerEntity(text) }
    }

    private fun NerEntityDto.toNerEntity(originalText: String): NerEntity? {
        val type = NerPromptTemplate.parseAnnotationType(this.type) ?: return null

        val validStart = start.coerceIn(0, originalText.length)
        val validEnd = end.coerceIn(validStart, originalText.length)
        if (validEnd <= validStart) return null

        return NerEntity(
            text = originalText.substring(validStart, validEnd),
            type = type,
            startIndex = validStart,
            endIndex = validEnd,
            confidence = confidence
        )
    }

    companion object {
        const val PROVIDER_ID = "claude_cloud"
    }
}
