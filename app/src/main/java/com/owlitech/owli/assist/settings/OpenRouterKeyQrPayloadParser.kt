package com.owlitech.owli.assist.settings

import java.util.Base64

sealed interface OpenRouterKeyQrPayload {
    data class PlainKey(val key: String) : OpenRouterKeyQrPayload
    data class EncryptedKey(val payload: OpenRouterEncryptedQrPayload) : OpenRouterKeyQrPayload
}

class OpenRouterEncryptedQrPayload(
    val version: String,
    val algorithm: String,
    val iterations: Int,
    val salt: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray
)

object OpenRouterKeyQrPayloadParser {
    private const val PLAIN_PREFIX = "openrouter:key="
    private const val ENCRYPTED_HEAD = "openrouter:keyenc:"
    private const val EXPECTED_VERSION = "v1"
    private const val EXPECTED_ALGORITHM = "pbkdf2-sha256"
    private val keyPattern = Regex("^sk-or-[A-Za-z0-9_-]{16,}$")

    fun parse(payload: String): OpenRouterKeyQrPayload? {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) return null

        parsePlainKey(trimmed)?.let { return OpenRouterKeyQrPayload.PlainKey(it) }
        parseEncrypted(trimmed)?.let { return OpenRouterKeyQrPayload.EncryptedKey(it) }
        return null
    }

    fun extractKey(payload: String): String? {
        return (parse(payload) as? OpenRouterKeyQrPayload.PlainKey)?.key
    }

    internal fun isValidOpenRouterKey(candidate: String): Boolean {
        return keyPattern.matches(candidate)
    }

    private fun parsePlainKey(payload: String): String? {
        val candidate = if (payload.startsWith(PLAIN_PREFIX, ignoreCase = true)) {
            payload.substring(PLAIN_PREFIX.length).trim()
        } else {
            payload
        }
        return candidate.takeIf(::isValidOpenRouterKey)
    }

    private fun parseEncrypted(payload: String): OpenRouterEncryptedQrPayload? {
        if (!payload.startsWith(ENCRYPTED_HEAD, ignoreCase = true)) return null

        val parts = payload.split(":")
        if (parts.size != 8) return null
        if (!parts[0].equals("openrouter", ignoreCase = true)) return null
        if (!parts[1].equals("keyenc", ignoreCase = true)) return null
        if (!parts[2].equals(EXPECTED_VERSION, ignoreCase = true)) return null
        if (!parts[3].equals(EXPECTED_ALGORITHM, ignoreCase = true)) return null

        val iterations = parts[4].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val salt = decodeBase64Url(parts[5]) ?: return null
        val iv = decodeBase64Url(parts[6]) ?: return null
        val ciphertext = decodeBase64Url(parts[7]) ?: return null
        if (salt.isEmpty() || iv.isEmpty() || ciphertext.isEmpty()) return null

        return OpenRouterEncryptedQrPayload(
            version = EXPECTED_VERSION,
            algorithm = EXPECTED_ALGORITHM,
            iterations = iterations,
            salt = salt,
            iv = iv,
            ciphertext = ciphertext
        )
    }

    private fun decodeBase64Url(value: String): ByteArray? {
        return runCatching {
            Base64.getUrlDecoder().decode(value)
        }.getOrNull()
    }
}
