package com.example.bikeassist.vlm

data class VlmConfig(
    val model: String,
    val maxTokens: Int,
    val temperature: Double,
    val systemPrompt: String,
    val overviewPrompt: String
) {
    companion object {
        const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        const val DEFAULT_MAX_TOKENS = 320
        const val DEFAULT_TEMPERATURE = 0.2
        const val DEFAULT_SYSTEM_PROMPT =
            "Du bist ein Assistenzsystem fuer sehbehinderte Radfahrer. " +
            "Antworte auf Deutsch in kurzen, strukturierten Saetzen. " +
            "Fokussiere Szene, Hindernisse und eine vorsichtige Handlungsempfehlung. " +
            "Keine Garantien wie 'Weg frei'."
        const val DEFAULT_OVERVIEW_PROMPT =
            "Beschreibe die Szene knapp und klar. " +
            "Nenne Hindernisse und eine sichere, vorsichtige Empfehlung."

        fun defaults(): VlmConfig {
            return VlmConfig(
                model = DEFAULT_MODEL,
                maxTokens = DEFAULT_MAX_TOKENS,
                temperature = DEFAULT_TEMPERATURE,
                systemPrompt = DEFAULT_SYSTEM_PROMPT,
                overviewPrompt = DEFAULT_OVERVIEW_PROMPT
            )
        }
    }
}
