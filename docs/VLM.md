# VLM (OpenRouter) - Profile-driven Setup

Dieses Dokument beschreibt die VLM-Integration ueber OpenRouter mit profilbasierter Konfiguration.
Ziel: Modelle ohne Codeaenderung per JSON austauschen, stabile Antworten (message.content) erzwingen
und Reasoning nur fuer Debug/Telemetry nutzen.

## 1) Zentrale Konfiguration

Quelle der Wahrheit ist `app/src/main/assets/vlm-profiles.json`.
`vlm-config.json` ist entfernt; die Konfiguration laeuft ausschliesslich ueber `vlm-profiles.json`.

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
      "id": "nano-fast",
      "label": "Nano Fast",
      "model_id": "openai/gpt-5-nano",
      "family": "gpt5",
      "streaming_enabled": true,
      "token_policy": {
        "max_tokens": 200,
        "reasoning_exclude": true,
        "reasoning_effort": "minimal",
        "retry1_max_tokens": 260,
        "retry2_max_tokens": 320
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
- `token_policy`: `max_tokens`, optional `reasoning_effort`, optional `reasoning_exclude`, retry budgets
- `streaming_enabled`: optional, aktiviert SSE-Streaming im VLM-Mode
- `parameter_overrides`: z.B. `temperature`
- `capabilities`: `supports_vision`, `supports_reasoning`, `supports_json`
- `auto_scan`: optional, aktiviert Auto-Toggle im VLM-Screen (`enabled_by_default`, `interval_ms`, `speak_free_every_ms`)

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

## 5) Minimal-Latenz Profil (nano-fast)

## 6) Developer Tools (Repo)

- Editor: `tools/vlm-profile-editor/` (Open JSON, Validate, Save/Download).
- CLI-Check: `python tools/validate_vlm_profiles.py` (optional: Pfad als Argument).

Empfohlen fuer GPT-5-nano mit sehr kurzer Ausgabe:
- `max_tokens`: 180-200
- `reasoning_exclude`: true
- `reasoning_effort`: "minimal" (optional)
- kleine Retries (z.B. 260/320)
- kurze Prompts (max 3 Saetze)

## 6) Streaming-Mode

Wenn `streaming_enabled=true`, werden Antworten per SSE gestreamt:
- UI zeigt fortlaufenden Text an.
- Abschluss kommt mit `onComplete` (finaler Text + usage/finish_reason).
Wenn Streaming aus ist, verhaelt sich die App wie bisher.

## 7) Autoscan (optional)

Wenn ein Profil `auto_scan` setzt, zeigt der VLM-Screen einen Auto-Toggle an und triggert
periodisch denselben "Neue Szene"-Pfad. Autoscan startet nie automatisch, auch wenn
`enabled_by_default=true`. `speak_free_every_ms` wird aktuell nur gespeichert.
Manueller "Neue Szene"-Trigger schaltet Auto aus.

## 8) Response-Parsing

- `finalAnswer` kommt **nur** aus `message.content` (oder kompatiblen Feldern wie
  `message.output_text`).
- `message.reasoning` und `reasoning_details` sind **nur Debug/Telemetry**.
- UI/TTS verwenden ausschliesslich `finalAnswer`.
- `usage` (prompt/completion/reasoning Tokens) wird geloggt.

## 9) Neue Modelle hinzufuegen

1. Profil in `vlm-profiles.json` ergaenzen (neue `id`, `model_id`).
2. Optional `family`, `token_policy`, `parameter_overrides` definieren.
3. App starten und Profil im VLM-Settings-Screen auswaehlen.

Keine Codeaenderung erforderlich, solange das Profilschema korrekt ist.
