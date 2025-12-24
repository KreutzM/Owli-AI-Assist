package com.example.bikeassist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractAssistantTest {

    @Test
    fun extractStringContent() {
        val body = """{"choices":[{"message":{"content":"Hallo"}}]}"""
        val result = extractAssistant(body, "openai/gpt-4o-mini", null)
        assertEquals("Hallo", result.text)
        assertTrue(result.debugPath.startsWith("message.content:string"))
    }

    @Test
    fun extractOutputTextPart() {
        val body = """{"choices":[{"message":{"content":[{"type":"output_text","text":"Antwort"}]}}]}"""
        val result = extractAssistant(body, "openai/gpt-5-nano", null)
        assertEquals("Antwort", result.text)
    }

    @Test
    fun extractContentFieldFromTextPart() {
        val body = """{"choices":[{"message":{"content":[{"type":"text","content":"Hinweis"}]}}]}"""
        val result = extractAssistant(body, "openai/gpt-4o-mini", null)
        assertEquals("Hinweis", result.text)
    }

    @Test
    fun extractReasoningWhenContentEmpty() {
        val body = """{"choices":[{"message":{"content":"","reasoning":"Begruendung"}}]}"""
        val result = extractAssistant(body, "openai/gpt-5-nano", null)
        assertEquals("Begruendung", result.text)
        assertTrue(result.isReasoningOnly)
    }
}
