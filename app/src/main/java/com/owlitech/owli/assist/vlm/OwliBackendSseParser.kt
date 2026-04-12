package com.owlitech.owli.assist.vlm

import org.json.JSONObject
import java.time.Instant

internal sealed interface OwliBackendSseEvent {
    data class Metadata(
        val mode: String?,
        val modelAlias: String?,
        val profileId: String?,
        val locale: String?
    ) : OwliBackendSseEvent

    data class Delta(
        val textDelta: String,
        val requestId: String?
    ) : OwliBackendSseEvent

    data class Done(
        val answerText: String,
        val mode: String?,
        val modelAlias: String?,
        val requestId: String?,
        val sceneToken: String?,
        val sceneTokenExpiresAt: Instant?
    ) : OwliBackendSseEvent

    data class Error(
        val message: String
    ) : OwliBackendSseEvent
}

internal object OwliBackendSseParser {
    fun parseEvent(eventType: String?, data: String): OwliBackendSseEvent? {
        val normalizedType = eventType?.trim().orEmpty()
        if (normalizedType.isEmpty()) return null
        return when (normalizedType) {
            "metadata" -> parseMetadata(data)
            "delta" -> parseDelta(data)
            "done" -> parseDone(data)
            "error" -> parseError(data)
            else -> null
        }
    }

    private fun parseMetadata(data: String): OwliBackendSseEvent.Metadata? {
        val root = data.toJsonObjectOrNull() ?: return null
        return OwliBackendSseEvent.Metadata(
            mode = root.optString("mode").trim().ifEmpty { null },
            modelAlias = root.optString("modelAlias").trim().ifEmpty { null },
            profileId = root.optString("profileId").trim().ifEmpty { null },
            locale = root.optString("locale").trim().ifEmpty { null }
        )
    }

    private fun parseDelta(data: String): OwliBackendSseEvent.Delta? {
        val root = data.toJsonObjectOrNull() ?: return null
        val textDelta = root.optString("textDelta").trim().ifEmpty { return null }
        return OwliBackendSseEvent.Delta(
            textDelta = textDelta,
            requestId = root.optString("requestId").trim().ifEmpty { null }
        )
    }

    private fun parseDone(data: String): OwliBackendSseEvent.Done? {
        val root = data.toJsonObjectOrNull() ?: return null
        val answerText = root.optString("answerText").trim().ifEmpty { return null }
        return OwliBackendSseEvent.Done(
            answerText = answerText,
            mode = root.optString("mode").trim().ifEmpty { null },
            modelAlias = root.optString("modelAlias").trim().ifEmpty { null },
            requestId = root.optString("requestId").trim().ifEmpty { null },
            sceneToken = root.optString("sceneToken").trim().ifEmpty { null },
            sceneTokenExpiresAt = root.optString("sceneTokenExpiresAt").toInstantOrNull()
        )
    }

    private fun parseError(data: String): OwliBackendSseEvent.Error? {
        val root = data.toJsonObjectOrNull() ?: return null
        val message = root.optString("message").trim()
            .ifEmpty { root.optString("error").trim() }
            .ifEmpty { return null }
        return OwliBackendSseEvent.Error(message = message)
    }

    private fun String.toJsonObjectOrNull(): JSONObject? {
        return runCatching { JSONObject(this) }.getOrNull()
    }

    private fun String?.toInstantOrNull(): Instant? {
        val value = this?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }
}
