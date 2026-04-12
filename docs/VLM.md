# VLM (OpenRouter) - Profile-driven Setup

Dieses Dokument beschreibt die VLM-Integration ueber OpenRouter mit profilbasierter Konfiguration.
Ziel: Modelle ohne Codeaenderung per JSON austauschen, stabile Antworten (message.content) erzwingen
und Reasoning nur fuer Debug/Telemetry nutzen.

## 0) Transport-Modell (aktueller Stand)

- `AppSettings.vlmTransportMode` modelliert die echten Laufzeitpfade jetzt explizit:
  - `BACKEND_MANAGED`
  - `DIRECT_OPENROUTER_BYOK`
  - `EMBEDDED_DEBUG`
- Der normale Produktionspfad ist `BACKEND_MANAGED` gegen `https://api.owli-ai.com`.
- Direkter OpenRouter-Betrieb bleibt als separater BYOK-Pfad erhalten und nutzt den lokal verschluesselt gespeicherten Nutzer-Key.
- Die App liest `OPENROUTER_API_KEY` weiter lokal aus `local.properties`, uebernimmt ihn in `BuildConfig`, behandelt ihn aber nur noch als Debug-/Entwicklungs-Fallback.
- In Settings fuehrt ein eigener Unterbildschirm fuer Transport- und Key-Verwaltung zur manuellen Eingabe, expliziten Paste-Aktion oder zum QR-Code-Import.
- QR-Import akzeptiert rohe OpenRouter-Keys, `openrouter:key=<KEY>` und PIN-geschuetzte QR-Payloads im Format `openrouter:keyenc:v1:pbkdf2-sha256:<iterations>:<salt_b64url>:<iv_b64url>:<ciphertext_b64url>`.
- Im Key-Verwalten-Bildschirm kann die App fuer den aktuell aktiven direkten OpenRouter-Key ueber `GET /api/v1/key` Key-Infos wie Label, Limits, Reset-Info, Nutzungswerte und Free-Tier-Status abrufen.
- QR-Decoding nutzt ML Kit Barcode Scanning; CameraX liefert nur Kamera-Frames und keinen QR-Decoder.
- Beim Speichern eines Nutzer-Keys wird `DIRECT_OPENROUTER_BYOK` aktiv, beim Loeschen faellt die App auf `BACKEND_MANAGED` zurueck.
- `OpenRouterUserKeyStore` ist die schmale Storage-API fuer spaetere Import-/Eingabe-Flows (`saveKey`, `loadKey`, `hasKey`, `clearKey`).
- `AndroidOpenRouterUserKeyStore` verschluesselt den Nutzer-Key mit einem Android-Keystore-backed AES-GCM-Key und speichert nur Version, IV und Ciphertext in separaten privaten Preferences.
- Der Nutzer-Key wird nicht in `AppSettings`, DataStore, Logs oder Docs persistiert; leere Keys werden abgelehnt, vorhandene Keys werden beim Speichern ueberschrieben, `clearKey` entfernt den gespeicherten Blob.
- Zusaetzlich liegt jetzt eine neue kanonische Registry-Grundlage in `app/src/main/assets/vlm-profile-registry.json`; Details und Migrationsziel stehen in `docs/VLM-Profile-Registry.md`.

## 0b) Backend-Transport (Phase 6)

- `POST /api/v1/session/bootstrap` liefert ein kurzlebiges `sessionToken` fuer das aktuelle `installationId`/App-Version/Locale-Paar.
- `POST /api/v1/scene/describe` nutzt dieses `sessionToken` plus Snapshot-Bild und liefert `answerText` sowie ein kurzlebiges `sceneToken`.
- `POST /api/v1/scene/followup` nutzt `sessionToken` + `sceneToken` + `questionText` fuer Rueckfragen zur zuletzt beschriebenen Szene.
- Wenn `streaming_enabled=true`, sendet der Backend-Pfad fuer `scene/describe` und `scene/followup` `stream: true` und konsumiert den normalisierten SSE-Eventstrom mit `metadata`, `delta`, `done` und `error`.
- Der Backend-Pfad ist bewusst explizit und nicht nur ein versteckter Ersatz fuer den alten OpenRouter-Client.
- Zusatzbilder bei Folgefragen bleiben vorerst ein Direct-BYOK-Only-Pfad; Backend-Follow-up arbeitet in diesem Stand nur mit Textfragen auf Basis des `sceneToken`.

## 0a) Datenfluss (aktueller App-Stand)

- Lokal auf dem Geraet bleiben Live-Kameravorschau, Snapshot-Erfassung bis zur Nutzeraktion, DataStore-Einstellungen, VLM-Profilwahl und TTS-Wiedergabe.
- An das Owli-Backend oder direkt an OpenRouter gehen nur Daten aus expliziten VLM-Aktionen.
- Backend-Modus: Snapshot-Bild fuer `scene/describe`, danach Textfragen fuer `scene/followup`.
- Direct-BYOK-Modus: Snapshot-Bild, optionale weitere Bild-Anhaenge und der zugehoerige Nutzertext gehen direkt an OpenRouter.
- Die Antwort kommt als Text zurueck; die App zeigt aktuell Rohtext an und kann ihn optional per TTS ausgeben.
- Android-Backup und Device-Transfer-Restore sind fuer die shipped App deaktiviert; lokale App-Daten werden daher nicht ueber Android-Backup migriert.

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
- Im Backend-Modus kommen die Deltas aus dem Owli-Backend-SSE-Strom; faellt dieser vor dem ersten Delta aus dem Protokoll, bleibt ein non-streaming Fallback verfuegbar.
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
