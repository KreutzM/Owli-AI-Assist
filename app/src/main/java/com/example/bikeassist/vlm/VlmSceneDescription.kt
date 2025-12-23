package com.example.bikeassist.vlm

import org.json.JSONArray
import org.json.JSONObject

data class VlmSceneDescription(
    val ttsOneLiner: String,
    val obstacles: List<String>,
    val landmarks: List<String>,
    val readableText: String,
    val actionSuggestion: String,
    val overallConfidence: String? = null
) {
    companion object {
        fun parse(raw: String): Result<VlmSceneDescription> {
            return runCatching {
                val jsonText = extractJsonObject(raw)
                    ?: throw IllegalArgumentException("Kein JSON-Objekt gefunden.")
                val obj = JSONObject(jsonText)
                val tts = obj.optString("tts_one_liner", "").trim()
                val readable = obj.optString("readable_text", "").trim()
                val action = obj.optString("action_suggestion", "").trim()
                val obstacles = obj.optJSONArray("obstacles").toStringList()
                val landmarks = obj.optJSONArray("landmarks").toStringList()
                val confidence = obj.optString("overall_confidence", "").trim().ifEmpty { null }
                if (tts.isBlank() && readable.isBlank() && action.isBlank()) {
                    throw IllegalArgumentException("JSON enthaelt keine verwertbaren Felder.")
                }
                VlmSceneDescription(
                    ttsOneLiner = tts,
                    obstacles = obstacles,
                    landmarks = landmarks,
                    readableText = readable,
                    actionSuggestion = action,
                    overallConfidence = confidence
                )
            }
        }

        private fun extractJsonObject(raw: String): String? {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return raw.substring(start, end + 1)
        }

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            val result = ArrayList<String>()
            for (i in 0 until length()) {
                val item = opt(i)
                when (item) {
                    is String -> result.add(item)
                    is JSONObject -> {
                        val name = item.optString("name", "").ifEmpty { item.toString() }
                        result.add(name)
                    }
                    else -> result.add(item.toString())
                }
            }
            return result
        }
    }
}
