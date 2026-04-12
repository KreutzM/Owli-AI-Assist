package com.owlitech.owli.assist.vlm

import android.content.Context
import com.owlitech.owli.assist.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val OWLI_BACKEND_PROFILES_URL = "https://api.owli-ai.com/api/v1/profiles"
private const val VLM_PROFILES_CACHE_PREFS = "vlm_profiles_cache"
private const val VLM_PROFILES_CACHE_KEY = "backend_profiles_json"

internal class OwliBackendProfilesService(
    private val url: String = OWLI_BACKEND_PROFILES_URL
) {
    suspend fun fetchProfilesJson(): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        val httpCode = connection.responseCode
        val body = (if (httpCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (httpCode !in 200..299) {
            throw IOException("Owli backend profiles request failed with HTTP $httpCode.")
        }
        body
    }
}

internal class VlmProfilesCacheStore(context: Context) {
    private val preferences = context.getSharedPreferences(VLM_PROFILES_CACHE_PREFS, Context.MODE_PRIVATE)

    fun load(): String? {
        return preferences.getString(VLM_PROFILES_CACHE_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun save(rawJson: String) {
        preferences.edit().putString(VLM_PROFILES_CACHE_KEY, rawJson).apply()
    }

    fun clear() {
        preferences.edit().remove(VLM_PROFILES_CACHE_KEY).apply()
    }
}

class VlmProfilesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheStore = VlmProfilesCacheStore(appContext)
    private val service = OwliBackendProfilesService()

    fun loadInitialConfig(): VlmProfilesConfig {
        val localRegistry = VlmProfileLoader.loadLocalRegistryAsset(appContext)
        val cachedRaw = cacheStore.load()
        val cachedConfig = cachedRaw
            ?.let(VlmProfileLoader::parsePublicRegistry)
            ?.let { publicRegistry ->
                VlmProfileLoader.mergePublicRegistry(publicRegistry, localRegistry).copy(
                    source = VlmProfilesSource.CACHED_BACKEND
                )
            }
        if (cachedRaw != null && cachedConfig == null) {
            cacheStore.clear()
        }
        if (cachedConfig != null) {
            return cachedConfig
        }
        return localRegistry ?: VlmProfileLoader.loadLegacyAsset(appContext)
    }

    suspend fun refreshRemoteConfig(): VlmProfilesConfig? = withContext(Dispatchers.IO) {
        val localRegistry = VlmProfileLoader.loadLocalRegistryAsset(appContext)
        return@withContext runCatching {
            val rawJson = service.fetchProfilesJson()
            val publicRegistry = VlmProfileLoader.parsePublicRegistry(rawJson)
                ?: throw IOException("Owli backend profiles returned an invalid response.")
            cacheStore.save(rawJson)
            VlmProfileLoader.mergePublicRegistry(publicRegistry, localRegistry)
        }.onFailure { error ->
            AppLogger.w("VLM", "Remote profiles refresh failed; keeping cached/local profiles. ${error::class.java.simpleName}")
        }.getOrNull()
    }
}
