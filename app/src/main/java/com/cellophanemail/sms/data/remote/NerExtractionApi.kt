package com.cellophanemail.sms.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class NerExtractionRequest(
    @SerializedName("content") val content: String,
    @SerializedName("channel") val channel: String = "sms"
)

data class NerExtractionResponse(
    @SerializedName("entities") val entities: List<NerEntityDto>,
    @SerializedName("tone") val tone: String? = null,
    @SerializedName("processing_time_ms") val processingTimeMs: Int = 0,
    @SerializedName("extractor_used") val extractorUsed: String = "",
    @SerializedName("channel") val channel: String = ""
)

data class NerEntityDto(
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("confidence") val confidence: Float = 0.9f
)

interface NerExtractionApi {
    @POST("api/v1/messages/extract-entities")
    suspend fun extractEntities(
        @Body request: NerExtractionRequest
    ): Response<NerExtractionResponse>
}
