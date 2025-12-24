package com.example.bikeassist.vlm

data class VlmProfile(
    val id: String,
    val label: String,
    val description: String?,
    val model: String,
    val maxTokens: Int,
    val systemPrompt: String,
    val overviewPrompt: String,
    val temperature: Double?,
    val thinkingEffort: String? = null
)
