package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VlmSseParserTest {

    @Test
    fun streamingDeltaConcatAndFinish() {
        val events = listOf(
            """{"choices":[{"delta":{"content":"Hal"}}]}""",
            """{"choices":[{"delta":{"content":"lo"}}]}""",
            """{"choices":[{"finish_reason":"stop"}]}"""
        )
        val buffer = StringBuilder()
        var finishReason: String? = null
        for (data in events) {
            val event = VlmSseParser.parseEvent(data) ?: continue
            event.deltaText?.let { buffer.append(it) }
            finishReason = event.finishReason ?: finishReason
        }
        assertEquals("Hallo", buffer.toString())
        assertEquals("stop", finishReason)
    }

    @Test
    fun streamingIgnoresInvalidJson() {
        val event = VlmSseParser.parseEvent("not-json")
        assertNull(event)
    }
}
