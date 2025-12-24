# VLM (OpenRouter) - Profile-driven Setup

Dieses Dokument beschreibt die VLM-Integration ueber OpenRouter mit profilbasierter Konfiguration.
Ziel: Modelle ohne Codeaenderung per JSON austauschen, stabile Antworten (message.content) erzwingen
und Reasoning nur fuer Debug/Telemetry nutzen.

## 1) Zentrale Konfiguration

Quelle der Wahrheit ist `app/src/main/assets/vlm-profiles.json`.
`vlm-config.json` ist legacy und wird nicht mehr aktiv verwendet.

Die Datei enthaelt globale Defaults und eine Liste von Profilen:

```json
{
  "defaults": {
    "provider": "openrouter",
    "system_prompt": "...",
    "overview_prompt": "...",
    "image": { "max_side_px": 1024, "jpeg_quality": 80 },
    "token_policy": { "max_tokens": 320 }
  },
  "default_profile_id": "gpt4o_default",
  "profiles": [
    {
      "id": "gpt4o_default",
      "label": "GPT-4o Mini",
      "model_id": "openai/gpt-4o-mini",
      "parameter_overrides": { "temperature": 0.2 },
      "token_policy": { "max_tokens": 320 }
    },
    {
      "id": "nano-low",
      "label": "Nano Low",
      "model_id": "openai/gpt-5-nano",
      "family": "gpt5",
      "token_policy": {
        "max_tokens": 900,
        "reasoning_effort": "low",
        "retry1_max_tokens": 1200,
        "retry2_max_tokens": 1400
      }
    }
  ]
}
```

## 2) Profilfelder (Kurzuebersicht)

- `model_id`: OpenRouter Model ID (z.B. `openai/gpt-5-nano`)
- `provider`: aktuell nur `openrouter`
- `family`: steuert Policy (z.B. `gpt5`, `gpt4o`)
- `system_prompt`, `overview_prompt`: prompt defaults (falls im Profil nicht gesetzt)
- `image`: Snapshot-Qualitaet (`max_side_px`, `jpeg_quality`)
- `token_policy`: `max_tokens`, optional `reasoning_effort`, retry budgets
- `parameter_overrides`: z.B. `temperature`
- `capabilities`: `supports_vision`, `supports_reasoning`, `supports_json`

## 3) Policy-Familien

- `gpt5`: Reasoning erlaubt, Default `reasoning_effort=low`, hoehere Token-Budgets.
- `gpt4o`: Reasoning nicht gesendet, Temperatur erlaubt, moderate Token-Budgets.
- `other`: konservative Defaults (wie gpt4o, ohne Reasoning).

## 4) Retry-Strategie (Reasoning-only)

Wenn `message.content` leer ist und Reasoning vorhanden ist, gilt die Antwort als
Reasoning-only und wird nicht an UI/TTS weitergegeben. Dann:

1) Retry #1: `reasoning.effort=low` + mehr Output-Tokens.
2) Retry #2: Reasoning weglassen + mehr Output-Tokens + System-Hinweis:
   "Gib NUR die finale Antwort im content aus. Keine Zwischenschritte."

Maximal 2 Retries, jeder Schritt wird im Log markiert.

## 5) Response-Parsing

- `finalAnswer` kommt **nur** aus `message.content` (oder kompatiblen Feldern wie
  `message.output_text`).
- `message.reasoning` und `reasoning_details` sind **nur Debug/Telemetry**.
- UI/TTS verwenden ausschliesslich `finalAnswer`.
- `usage` (prompt/completion/reasoning Tokens) wird geloggt.

## 6) Neue Modelle hinzufuegen

1. Profil in `vlm-profiles.json` ergaenzen (neue `id`, `model_id`).
2. Optional `family`, `token_policy`, `parameter_overrides` definieren.
3. App starten und Profil im Settings-Screen auswaehlen.

Keine Codeaenderung erforderlich, solange das Profilschema korrekt ist.
