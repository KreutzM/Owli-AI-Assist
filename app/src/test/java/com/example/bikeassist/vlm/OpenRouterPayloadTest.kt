package com.example.bikeassist.vlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterPayloadTest {

    @Test
    fun reasoningExcludeAddsExclude() {
        val provider = OpenRouterProvider(apiKey = "test")
        val request = VlmProviderRequest(
            modelId = "openai/gpt-5-nano",
            messages = listOf(VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text("Hi")))),
            options = VlmRequestOptions(
                maxTokens = 200,
                temperature = null,
                reasoningEffort = "minimal",
                reasoningExclude = true,
                includeReasoning = true,
                stream = false
            ),
            family = VlmModelFamily.GPT5
        )
        val payload = provider.buildPayloadForTest(request).payload
        val reasoning = payload.optJSONObject("reasoning")
        assertTrue(reasoning?.optBoolean("exclude") == true)
        assertEquals("minimal", reasoning?.optString("effort"))
    }

    @Test
    fun reasoningEffortWithoutExclude() {
        val provider = OpenRouterProvider(apiKey = "test")
        val request = VlmProviderRequest(
            modelId = "openai/gpt-5-nano",
            messages = listOf(VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text("Hi")))),
            options = VlmRequestOptions(
                maxTokens = 200,
                temperature = null,
                reasoningEffort = "low",
                reasoningExclude = false,
                includeReasoning = true,
                stream = false
            ),
            family = VlmModelFamily.GPT5
        )
        val payload = provider.buildPayloadForTest(request).payload
        val reasoning = payload.optJSONObject("reasoning")
        assertTrue(reasoning?.has("exclude") == false)
        assertEquals("low", reasoning?.optString("effort"))
    }

    @Test
    fun nonReasoningModelOmitsReasoningAndNullTemperature() {
        val provider = OpenRouterProvider(apiKey = "test")
        val request = VlmProviderRequest(
            modelId = "openai/gpt-4o-mini",
            messages = listOf(VlmChatMessage(role = "user", content = listOf(VlmContentPart.Text("Hi")))),
            options = VlmRequestOptions(
                maxTokens = 120,
                temperature = null,
                reasoningEffort = "low",
                reasoningExclude = true,
                includeReasoning = false,
                stream = false
            ),
            family = VlmModelFamily.GPT4O
        )
        val payload = provider.buildPayloadForTest(request).payload
        assertFalse(payload.has("reasoning"))
        assertFalse(payload.has("temperature"))
    }
}
