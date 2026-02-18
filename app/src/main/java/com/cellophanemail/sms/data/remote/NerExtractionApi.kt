package com.cellophanemail.sms.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class NerExtractionRequest(
    @SerializedName("text") val text: String
)

data class NerExtractionResponse(
    @SerializedName("entities") val entities: List<NerEntityDto>
)

data class NerEntityDto(
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int,
    @SerializedName("confidence") val confidence: Float = 0.9f
)

interface NerExtractionApi {
    @POST("api/v1/ner/extract")
    suspend fun extractEntities(
        @Body request: NerExtractionRequest
    ): Response<NerExtractionResponse>
}
