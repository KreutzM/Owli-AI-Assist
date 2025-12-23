package com.example.bikeassist.vlm

data class VlmProfile(
    val id: String,
    val label: String,
    val model: String,
    val temperature: Double,
    val maxTokens: Int
)

val DEFAULT_VLM_PROFILES: List<VlmProfile> = listOf(
    VlmProfile(
        id = "safe",
        label = "Sicher",
        model = "openai/gpt-4o-mini",
        temperature = 0.2,
        maxTokens = 360
    ),
    VlmProfile(
        id = "fast",
        label = "Schnell",
        model = "openai/gpt-4o-mini",
        temperature = 0.6,
        maxTokens = 200
    )
)

fun findVlmProfile(profileId: String): VlmProfile {
    return DEFAULT_VLM_PROFILES.firstOrNull { it.id == profileId } ?: DEFAULT_VLM_PROFILES.first()
}
