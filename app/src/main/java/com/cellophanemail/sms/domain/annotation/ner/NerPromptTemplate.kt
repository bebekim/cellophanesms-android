package com.cellophanemail.sms.domain.annotation.ner

import com.cellophanemail.sms.domain.model.AnnotationType
import org.json.JSONArray
import org.json.JSONObject

object NerPromptTemplate {

    private const val SYSTEM_PROMPT = """Extract named entities from the text. Return ONLY a JSON object with an "entities" array. Each entity has: "text" (exact substring), "type" (PERSON_NAME, LOCATION, or ORGANIZATION), "start" (character index), "end" (character index, exclusive). If no entities found, return {"entities": []}."""

    fun buildPrompt(text: String): String =
        "$SYSTEM_PROMPT\n\nText: $text"

    fun buildPromptWithNoThink(text: String): String =
        "/nothink\n${buildPrompt(text)}"

    fun parseResponse(json: String, originalText: String): List<NerEntity> {
        val cleaned = json.trim().let { raw ->
            // Strip markdown code fences if present
            val fenceStart = raw.indexOf("```")
            if (fenceStart >= 0) {
                val jsonStart = raw.indexOf("{", fenceStart)
                val jsonEnd = raw.lastIndexOf("}")
                if (jsonStart >= 0 && jsonEnd > jsonStart) raw.substring(jsonStart, jsonEnd + 1) else raw
            } else {
                // Find the first { and last }
                val start = raw.indexOf("{")
                val end = raw.lastIndexOf("}")
                if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
            }
        }

        return try {
            val root = JSONObject(cleaned)
            val entities = root.optJSONArray("entities") ?: return emptyList()
            parseEntitiesArray(entities, originalText)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEntitiesArray(
        entities: JSONArray,
        originalText: String
    ): List<NerEntity> = buildList {
        for (i in 0 until entities.length()) {
            val obj = entities.optJSONObject(i) ?: continue
            val text = obj.optString("text", "") .takeIf { it.isNotEmpty() } ?: continue
            val typeName = obj.optString("type", "")
            val type = parseAnnotationType(typeName) ?: continue
            val start = obj.optInt("start", -1)
            val end = obj.optInt("end", -1)

            // Validate indices against original text
            val validStart = if (start in 0..originalText.length) start else findSubstringIndex(originalText, text)
            val validEnd = if (end in validStart..originalText.length) end else validStart + text.length

            if (validStart < 0 || validEnd <= validStart || validEnd > originalText.length) continue

            add(
                NerEntity(
                    text = originalText.substring(validStart, validEnd),
                    type = type,
                    startIndex = validStart,
                    endIndex = validEnd,
                    confidence = 0.85f
                )
            )
        }
    }

    fun parseAnnotationType(name: String): AnnotationType? = when (name.uppercase()) {
        "PERSON_NAME", "PERSON" -> AnnotationType.PERSON_NAME
        "LOCATION", "LOC" -> AnnotationType.LOCATION
        "ORGANIZATION", "ORG" -> AnnotationType.ORGANIZATION
        else -> null
    }

    private fun findSubstringIndex(text: String, substring: String): Int =
        text.indexOf(substring, ignoreCase = true)
}
