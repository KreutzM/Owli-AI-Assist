package com.owlitech.owli.assist.settings

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object OpenRouterEncryptedQrPayloadDecryptor {
    const val DEFAULT_PIN = "1597"

    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_KEY_ALGORITHM = "AES"
    private const val AES_KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    fun normalizePin(pinInput: String): String {
        val trimmed = pinInput.trim()
        if (trimmed.isBlank()) return DEFAULT_PIN
        require(trimmed.matches(Regex("^\\d{4}$"))) { "PIN must be exactly 4 digits." }
        return trimmed
    }

    fun decrypt(payload: OpenRouterEncryptedQrPayload, pinInput: String): String? {
        val normalizedPin = normalizePin(pinInput)
        if (payload.version != "v1" || payload.algorithm != "pbkdf2-sha256") return null

        return runCatching {
            val keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
            val keySpec = PBEKeySpec(
                normalizedPin.toCharArray(),
                payload.salt,
                payload.iterations,
                AES_KEY_BITS
            )
            val secretBytes = keyFactory.generateSecret(keySpec).encoded
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(secretBytes, AES_KEY_ALGORITHM),
                GCMParameterSpec(GCM_TAG_BITS, payload.iv)
            )
            val plaintext = cipher.doFinal(payload.ciphertext).toString(StandardCharsets.UTF_8).trim()
            plaintext.takeIf(OpenRouterKeyQrPayloadParser::isValidOpenRouterKey)
        }.getOrNull()
    }
}
