package com.owlitech.owli.assist.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenRouterKeyQrPayloadParserTest {
    @Test
    fun acceptsRawOpenRouterKey() {
        val key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456"

        assertEquals(key, OpenRouterKeyQrPayloadParser.extractKey("  $key  "))
    }

    @Test
    fun acceptsPrefixedOpenRouterKey() {
        val key = "sk-or-v1-abcdefghijklmnopqrstuvwxyz123456"

        assertEquals(key, OpenRouterKeyQrPayloadParser.extractKey("openrouter:key=$key"))
    }

    @Test
    fun rejectsUnrelatedPayload() {
        assertNull(OpenRouterKeyQrPayloadParser.extractKey("https://example.com/not-a-key"))
        assertNull(OpenRouterKeyQrPayloadParser.extractKey("openrouter:key=short"))
        assertNull(OpenRouterKeyQrPayloadParser.extractKey(""))
    }
}
