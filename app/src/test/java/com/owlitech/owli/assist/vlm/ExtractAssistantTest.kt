package com.owlitech.owli.assist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VlmResponseParserTest {

    @Test
    fun extractStringContent() {
        val body = """{"choices":[{"message":{"content":"Hallo"}}]}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-4o-mini", null)
        assertEquals("Hallo", result.finalAnswer)
        assertTrue(result.debugPath.startsWith("message.content:string"))
    }

    @Test
    fun extractOutputTextPart() {
        val body = """{"choices":[{"message":{"content":[{"type":"output_text","text":"Antwort"}]}}]}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-5-nano", null)
        assertEquals("Antwort", result.finalAnswer)
    }

    @Test
    fun extractContentFieldFromTextPart() {
        val body = """{"choices":[{"message":{"content":[{"type":"text","content":"Hinweis"}]}}]}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-4o-mini", null)
        assertEquals("Hinweis", result.finalAnswer)
    }

    @Test
    fun extractReasoningWhenContentEmpty() {
        val body = """{"choices":[{"message":{"content":"","reasoning":"Begruendung"}}]}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-5-nano", null)
        assertEquals("", result.finalAnswer)
        assertEquals("Begruendung", result.debugReasoningSummary)
        assertTrue(result.isReasoningOnly)
    }

    @Test
    fun extractReasoningSummaryFromDetails() {
        val body = """{"choices":[{"message":{"content":"","reasoning_details":[{"type":"reasoning.summary","text":"Kurz"}]}}]}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-5-nano", null)
        assertEquals("", result.finalAnswer)
        assertEquals("Kurz", result.debugReasoningSummary)
        assertTrue(result.isReasoningOnly)
    }

    @Test
    fun extractUsageTokens() {
        val body = """{"choices":[{"message":{"content":"Hallo"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":20,"completion_tokens_details":{"reasoning_tokens":5}}}"""
        val result = VlmResponseParser.parse(body, "openai/gpt-4o-mini", null)
        assertEquals("Hallo", result.finalAnswer)
        assertEquals(10, result.usage?.promptTokens)
        assertEquals(20, result.usage?.completionTokens)
        assertEquals(5, result.usage?.reasoningTokens)
    }
}
