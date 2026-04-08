# Owli-AI Assist

Owli-AI Assist ist eine Android-App fuer blinde Nutzer mit einem VLM-first-Workflow: Die App zeigt eine Live-Kameravorschau, nimmt auf Anforderung ein Bild auf und beantwortet danach Fragen zur Szene per Text und optionaler Sprachausgabe.

## Funktionsumfang
- Live-Kameravorschau mit CameraX
- VLM-On-Demand ueber OpenRouter inklusive Profil-Auswahl
- Follow-up-Fragen zu einer aktiven Szene
- Mehrfach-Anhaenge pro VLM-Session
- Optionale Streaming-TTS fuer fruehe Sprachausgabe
- DataStore-basierte App-Einstellungen fuer Sprache, TTS und Profilwahl

## Architektur (kurz)
- `ui`: Compose Screens, Navigation und ViewModels fuer den VLM-Flow
- `vlm`: Profile, Request-Aufbau, Parsing und Session-State
- `audio`: TTS und Streaming-TTS-Steuerung
- `settings`: persistente App-Einstellungen via DataStore

## Voraussetzungen
- Android Studio (AGP 8.x), Kotlin 2.0.x, Compose aktiviert
- Android-Geraet mit Kamera
- Internetverbindung fuer VLM-Anfragen

## Installation & Build
1. Repository oeffnen.
2. `OPENROUTER_API_KEY=...` in `local.properties` setzen (nicht committen).
3. Build unter PowerShell: `gradlew.bat :app:assembleDebug`
4. App aus Android Studio starten und Kamera-Permission erlauben.

## Bedienung
1. App starten -> VLM-Ansicht oeffnet sich direkt.
2. Kamera ausrichten und `Neue Szene` tippen.
3. Frage per Text oder Spracheingabe stellen.
4. Optional weitere Bilder anhaengen oder die letzte Antwort erneut sprechen lassen.
5. `Reset` bringt die Ansicht zurueck in die Live-Vorschau.

## Konfiguration
- Sprache: System / Deutsch / English
- TTS: Ein/Aus, Sprechtempo, Tonhoehe
- Streaming-TTS: frueher Sprachstart waehrend die Antwort eintrifft
- VLM-Profile und Prompts: `app/src/main/assets/vlm-profiles.json`

## Developer Tools
- Lokaler Editor fuer `vlm-profiles.json`: `tools/vlm-profile-editor/`
- CLI-Validator fuer VLM-Profile: `tools/validate_vlm_profiles.py`

## Workflow
- Team-Workflow: `docs/DEVELOPMENT.md`
- Repo-Regeln: `AGENTS.md`
- Codex-Prompts: `docs/Prompts-Codex-CLI.md`

## Lizenz / Nutzung
Interner Demo-/Prototyp-Status; keine Produktionsfreigabe, keine Gewaehrleistung.
