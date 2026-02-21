package com.cellophanemail.sms.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// ============================================================================
// DTOs mirroring server render_ast.py Document AST
// ============================================================================

data class EntityDecorationDto(
    @SerializedName("entity_type") val entityType: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("tappable") val tappable: Boolean = true
)

data class LinkDecorationDto(
    @SerializedName("url") val url: String,
    @SerializedName("tappable") val tappable: Boolean = true
)

data class AnnotatedSpanDto(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("style") val style: String,
    @SerializedName("decoration") val decoration: Map<String, Any>
) {
    /** Parse decoration as EntityDecoration if it contains entity_type. */
    fun asEntityDecoration(): EntityDecorationDto? {
        val entityType = decoration["entity_type"] as? String ?: return null
        val confidence = (decoration["confidence"] as? Number)?.toFloat() ?: 0.9f
        val tappable = decoration["tappable"] as? Boolean ?: true
        return EntityDecorationDto(entityType, confidence, tappable)
    }

    /** Parse decoration as LinkDecoration if it contains url. */
    fun asLinkDecoration(): LinkDecorationDto? {
        val url = decoration["url"] as? String ?: return null
        val tappable = decoration["tappable"] as? Boolean ?: true
        return LinkDecorationDto(url, tappable)
    }
}

data class TextBlockDto(
    @SerializedName("block_type") val blockType: String = "text",
    @SerializedName("text") val text: String,
    @SerializedName("spans") val spans: List<AnnotatedSpanDto> = emptyList()
)

data class ToneBadgeDto(
    @SerializedName("tone") val tone: String? = null,
    @SerializedName("confidence") val confidence: Float = 0.0f
)

data class DocumentDto(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("blocks") val blocks: List<TextBlockDto> = emptyList(),
    @SerializedName("tone") val tone: ToneBadgeDto? = null
)

data class RenderResponseDto(
    @SerializedName("document") val document: DocumentDto,
    @SerializedName("processing_time_ms") val processingTimeMs: Int,
    @SerializedName("extractor_used") val extractorUsed: String,
    @SerializedName("channel") val channel: String
)

// ============================================================================
// Retrofit API
// ============================================================================

interface RenderApi {
    @POST("api/v1/messages/render")
    suspend fun render(
        @Body request: NerExtractionRequest
    ): Response<RenderResponseDto>
}
