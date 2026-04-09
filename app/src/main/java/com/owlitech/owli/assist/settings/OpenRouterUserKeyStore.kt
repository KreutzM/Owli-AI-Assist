package com.owlitech.owli.assist.settings

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface OpenRouterUserKeyStore {
    fun saveKey(apiKey: String)
    fun loadKey(): String?
    fun hasKey(): Boolean
    fun clearKey()
}

class AndroidOpenRouterUserKeyStore(
    context: Context,
    private val keyAlias: String = KEY_ALIAS
) : OpenRouterUserKeyStore {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun saveKey(apiKey: String) {
        val normalized = apiKey.trim()
        require(normalized.isNotEmpty()) { "OpenRouter API key must not be blank." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(), SecureRandom())
        val ciphertext = cipher.doFinal(normalized.toByteArray(StandardCharsets.UTF_8))
        val blob = EncryptedOpenRouterUserKeyBlob(
            version = BLOB_VERSION,
            iv = cipher.iv,
            ciphertext = ciphertext
        )
        prefs.edit()
            .putString(PREF_ENCRYPTED_KEY_BLOB, blob.encode())
            .apply()
    }

    @Synchronized
    override fun loadKey(): String? {
        val blob = prefs.getString(PREF_ENCRYPTED_KEY_BLOB, null)
            ?.let { EncryptedOpenRouterUserKeyBlob.decodeOrNull(it) }
            ?: return null
        if (blob.version != BLOB_VERSION) return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, blob.iv))
            val plaintext = cipher.doFinal(blob.ciphertext)
            plaintext.toString(StandardCharsets.UTF_8).takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    @Synchronized
    override fun hasKey(): Boolean {
        return prefs.contains(PREF_ENCRYPTED_KEY_BLOB)
    }

    @Synchronized
    override fun clearKey() {
        prefs.edit()
            .remove(PREF_ENCRYPTED_KEY_BLOB)
            .apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "owli_openrouter_user_key"
        const val PREFS_NAME = "openrouter_user_key_secure"
        const val PREF_ENCRYPTED_KEY_BLOB = "encrypted_key_blob"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val BLOB_VERSION = 1
    }
}
