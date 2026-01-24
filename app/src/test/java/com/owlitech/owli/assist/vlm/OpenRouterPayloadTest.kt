package com.owlitech.owli.assist.vlm

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

    @Test
    fun payloadIncludesTextAndImagePartsInOrder() {
        val provider = OpenRouterProvider(apiKey = "test")
        val request = VlmProviderRequest(
            modelId = "openai/gpt-4o-mini",
            messages = listOf(
                VlmChatMessage(
                    role = "user",
                    content = listOf(
                        VlmContentPart.Text("Describe"),
                        VlmContentPart.ImageUrl("data:image/jpeg;base64,AAAA"),
                        VlmContentPart.ImageUrl("data:image/jpeg;base64,BBBB")
                    )
                )
            ),
            options = VlmRequestOptions(
                maxTokens = 120,
                temperature = null,
                reasoningEffort = null,
                reasoningExclude = false,
                includeReasoning = false,
                stream = false
            ),
            family = VlmModelFamily.GPT4O
        )

        val payload = provider.buildPayloadForTest(request).payload
        val messages = payload.getJSONArray("messages")
        val content = messages.getJSONObject(0).getJSONArray("content")
        assertEquals(3, content.length())
        assertEquals("text", content.getJSONObject(0).getString("type"))
        assertEquals("Describe", content.getJSONObject(0).getString("text"))
        assertEquals("image_url", content.getJSONObject(1).getString("type"))
        assertEquals(
            "data:image/jpeg;base64,AAAA",
            content.getJSONObject(1).getJSONObject("image_url").getString("url")
        )
        assertEquals("image_url", content.getJSONObject(2).getString("type"))
        assertEquals(
            "data:image/jpeg;base64,BBBB",
            content.getJSONObject(2).getJSONObject("image_url").getString("url")
        )
    }

    @Test
    fun payloadAllowsImagesWithoutText() {
        val provider = OpenRouterProvider(apiKey = "test")
        val request = VlmProviderRequest(
            modelId = "openai/gpt-4o-mini",
            messages = listOf(
                VlmChatMessage(
                    role = "user",
                    content = listOf(
                        VlmContentPart.ImageUrl("data:image/jpeg;base64,AAAA"),
                        VlmContentPart.ImageUrl("data:image/jpeg;base64,BBBB")
                    )
                )
            ),
            options = VlmRequestOptions(
                maxTokens = 120,
                temperature = null,
                reasoningEffort = null,
                reasoningExclude = false,
                includeReasoning = false,
                stream = false
            ),
            family = VlmModelFamily.GPT4O
        )

        val payload = provider.buildPayloadForTest(request).payload
        val messages = payload.getJSONArray("messages")
        val content = messages.getJSONObject(0).getJSONArray("content")
        assertEquals(2, content.length())
        assertEquals("image_url", content.getJSONObject(0).getString("type"))
        assertEquals("image_url", content.getJSONObject(1).getString("type"))
    }
}
