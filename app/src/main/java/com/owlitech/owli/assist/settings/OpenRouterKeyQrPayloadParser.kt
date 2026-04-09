package com.owlitech.owli.assist.settings

object OpenRouterKeyQrPayloadParser {
    private const val PREFIX = "openrouter:key="
    private val keyPattern = Regex("^sk-or-[A-Za-z0-9_-]{16,}$")

    fun extractKey(payload: String): String? {
        val trimmed = payload.trim()
        val candidate = if (trimmed.startsWith(PREFIX, ignoreCase = true)) {
            trimmed.substring(PREFIX.length).trim()
        } else {
            trimmed
        }
        return candidate.takeIf { keyPattern.matches(it) }
    }
}
