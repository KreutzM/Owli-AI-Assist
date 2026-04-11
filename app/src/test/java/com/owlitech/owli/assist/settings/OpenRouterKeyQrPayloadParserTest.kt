package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class OpenRouterKeyQrPayloadParserTest {
    @Test
    fun acceptsRawOpenRouterKey() {
        val key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456"

        assertEquals(key, OpenRouterKeyQrPayloadParser.extractKey("  $key  "))
        val parsed = OpenRouterKeyQrPayloadParser.parse("  $key  ")
        assertEquals(key, (parsed as OpenRouterKeyQrPayload.PlainKey).key)
    }

    @Test
    fun acceptsPrefixedOpenRouterKey() {
        val key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456"

        assertEquals(key, OpenRouterKeyQrPayloadParser.extractKey("openrouter:key=$key"))
    }

    @Test
    fun parsesEncryptedPayload() {
        val payload = "openrouter:keyenc:v1:pbkdf2-sha256:200000:${
            byteArrayToBase64Url(byteArrayOf(1, 2, 3, 4))
        }:${byteArrayToBase64Url(byteArrayOf(5, 6, 7, 8))}:${
            byteArrayToBase64Url(byteArrayOf(9, 10, 11, 12))
        }"

        val parsed = OpenRouterKeyQrPayloadParser.parse(payload) as? OpenRouterKeyQrPayload.EncryptedKey

        assertNotNull(parsed)
        assertEquals("v1", parsed?.payload?.version)
        assertEquals("pbkdf2-sha256", parsed?.payload?.algorithm)
        assertEquals(200000, parsed?.payload?.iterations)
        assertTrue(parsed?.payload?.salt?.contentEquals(byteArrayOf(1, 2, 3, 4)) == true)
    }

    @Test
    fun rejectsUnrelatedPayload() {
        assertNull(OpenRouterKeyQrPayloadParser.extractKey("https://example.com/not-a-key"))
        assertNull(OpenRouterKeyQrPayloadParser.extractKey("openrouter:key=short"))
        assertNull(OpenRouterKeyQrPayloadParser.parse("openrouter:keyenc:v2:pbkdf2-sha256:200000:a:b:c"))
        assertNull(OpenRouterKeyQrPayloadParser.parse("openrouter:keyenc:v1:pbkdf2-sha256:0:a:b:c"))
        assertNull(OpenRouterKeyQrPayloadParser.extractKey(""))
    }

    private fun byteArrayToBase64Url(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
