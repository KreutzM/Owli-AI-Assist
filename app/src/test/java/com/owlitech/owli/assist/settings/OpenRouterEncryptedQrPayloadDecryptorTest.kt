package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class OpenRouterEncryptedQrPayloadDecryptorTest {
    @Test
    fun blankPinFallsBackToDefaultPin() {
        assertEquals("1597", OpenRouterEncryptedQrPayloadDecryptor.normalizePin(""))
        assertEquals("1597", OpenRouterEncryptedQrPayloadDecryptor.normalizePin("   "))
    }

    @Test
    fun rejectsNonNumericOrWrongLengthPins() {
        try {
            OpenRouterEncryptedQrPayloadDecryptor.normalizePin("12a4")
            fail("Expected invalid PIN to throw.")
        } catch (_: IllegalArgumentException) {
        }

        try {
            OpenRouterEncryptedQrPayloadDecryptor.normalizePin("123")
            fail("Expected short PIN to throw.")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun decryptsEncryptedPayloadWithDefaultPin() {
        val key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456"
        val payload = createEncryptedPayload(key, "1597")

        assertEquals(key, OpenRouterEncryptedQrPayloadDecryptor.decrypt(payload, ""))
    }

    @Test
    fun failsDecryptionForWrongPin() {
        val payload = createEncryptedPayload(
            key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456",
            pin = "1597"
        )

        assertNull(OpenRouterEncryptedQrPayloadDecryptor.decrypt(payload, "1234"))
    }

    private fun createEncryptedPayload(key: String, pin: String): OpenRouterEncryptedQrPayload {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secret = keyFactory.generateSecret(PBEKeySpec(pin.toCharArray(), salt, 200000, 256)).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(secret, "AES"),
            GCMParameterSpec(128, iv)
        )
        val ciphertext = cipher.doFinal(key.toByteArray(StandardCharsets.UTF_8))
        return OpenRouterEncryptedQrPayload(
            version = "v1",
            algorithm = "pbkdf2-sha256",
            iterations = 200000,
            salt = salt,
            iv = iv,
            ciphertext = ciphertext
        )
    }
}
