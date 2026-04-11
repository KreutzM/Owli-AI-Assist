package com.owlitech.owli.assist.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterCurrentKeyInfoService(
    private val endpoint: String = "https://openrouter.ai/api/v1/key"
) {
    suspend fun fetchCurrentKeyInfo(apiKey: String): OpenRouterCurrentKeyInfo = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            throw OpenRouterCurrentKeyInfoException.NoActiveKey
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $trimmedKey")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("HTTP-Referer", "https://localhost")
            setRequestProperty("X-Title", "Owli-AI Assist")
        }

        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()

        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw OpenRouterCurrentKeyInfoException.Unauthorized
        }
        if (code !in 200..299) {
            throw OpenRouterCurrentKeyInfoException.Network
        }

        return@withContext OpenRouterCurrentKeyInfoParser.parse(body)
            ?: throw OpenRouterCurrentKeyInfoException.InvalidResponse
    }
}

sealed class OpenRouterCurrentKeyInfoException(message: String) : IOException(message) {
    data object NoActiveKey : OpenRouterCurrentKeyInfoException("No active OpenRouter key.")
    data object Unauthorized : OpenRouterCurrentKeyInfoException("OpenRouter key info unauthorized.")
    data object Network : OpenRouterCurrentKeyInfoException("OpenRouter key info request failed.")
    data object InvalidResponse : OpenRouterCurrentKeyInfoException("OpenRouter key info response invalid.")
}
