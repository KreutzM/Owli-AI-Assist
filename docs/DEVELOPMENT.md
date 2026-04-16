# Development Guide (Team)

Dieses Dokument ist die Einstiegstelle fuer den taeglichen Workflow (2 Menschen + 1 Codex-Agent).

## 1) Quickstart

### Voraussetzungen
- Android Studio (AGP 8.x), Android SDK
- JDK (passend zur Android-Toolchain; Android Studio bringt in der Regel eine kompatible JDK mit)
- Windows 10/11 + PowerShell (oder WSL2)

### OpenRouter client key (interim)
- `OPENROUTER_API_KEY=...` in `local.properties` setzen (nicht committen).
- Debug-Builds uebernehmen diesen Wert in `BuildConfig`, damit der direkte OpenRouter-Pfad lokal getestet werden kann.
- Release-Builds shippen bewusst keinen eingebetteten OpenRouter-Key.
- Das ist nur ein Debug-/Entwicklungs-Fallback und keine sichere Secret-Speicherung.
- Produktion arbeitet standardmaessig ueber das Owli-Backend; direkter OpenRouter-Betrieb ist nur der separate BYOK-Pfad mit lokal verschluesselt gespeichertem Nutzer-Key.

### Build (PowerShell)
- `gradlew.bat :app:assembleDebug`

### Release defaults
- Release-Builds aktivieren R8/Minify und Resource-Shrinking.
- App-Backup und Android data extraction sind fuer die shipped App deaktiviert.
- Der normale Produktionspfad ist `BACKEND_MANAGED` gegen `https://api.owli-ai.com`.
- Bilder und Fragetexte verlassen das Geraet nur nach expliziter Nutzeraktion, je nach aktivem Transport entweder an das Owli-Backend oder direkt an OpenRouter im BYOK-Modus.
- Release-Builds bieten keinen eingebetteten Provider-Key als normalen Produktionspfad an. Siehe `docs/PLAYSTORE-PRIVACY-READINESS.md`.

---

## 1a) VLM-First UX
- App startet standardmaessig im VLM-Modus.
- Die Live-Kamera dient zum Ausrichten; eine Szene wird erst bei `Neue Szene` aufgenommen.
- Einstellungen betreffen Sprache, TTS, VLM-Profilauswahl sowie den Transport-/Key-Bildschirm fuer Backend, BYOK und Debug-Fallback.

---

## 2) Daily Workflow (empfohlen)

### Branching
- Feature/Fix: `feat/...`, `fix/...`
- Chore/Tooling/Docs: `chore/...`
- Refactor/Quality: `ref/...` (z. B. `ref/Test-Optimization`)
- Mergen: per PR / Review (auch im 2er-Team sinnvoll)

### Chat -> Codex -> Chat Review
1. Planung und Scope passieren im Chat.
2. Codex fuehrt genau einen kleinen thematischen Run aus.
3. Codex arbeitet auf einem thematisch benannten Branch.
4. Codex erstellt kleine, buildbare Commits.
5. Codex beendet den Run mit dem `RUN REVIEW`-Paket aus `AGENTS.md`, damit der Review direkt in Chat/Repo passieren kann.

### Definition of Done pro Commit
- **klein & fokussiert** (eine logische Aenderung)
- **buildbar**
- mindestens: `./gradlew.bat :app:testDebugUnitTest`
- wenn relevant: `./gradlew.bat :app:lintDebug`
- wenn UI/Resources/Manifest/Gradle betroffen: `./gradlew.bat :app:assembleDebug`

> Wichtig: Keine automatischen Device-/Emulator-Tasks im Standard-Flow (`connectedAndroidTest`, `connectedCheck`, `installDebug` usw.)

---

## 3) Fast Checks vs Full Checks

### Fast (Default)
- `./gradlew.bat :app:testDebugUnitTest`

### Zusaetzlich bei Android-Komponenten / Public API Aenderungen
- `./gradlew.bat :app:lintDebug`

### Zusaetzlich bei UI/Resources/Manifest/Gradle Aenderungen
- `./gradlew.bat :app:assembleDebug`

### Full (vor Merge oder grossen Refactors)
- `./gradlew.bat :app:check`
  (oder: `:app:test` + `:app:lintDebug` + `:app:assembleDebug`)

---

## 4) Debugging

- VLM-Antworten werden nur nach expliziter Nutzeraktion angefordert.
- Kamera-Permission muss fuer Live-Vorschau und Bildaufnahme vorliegen.
- Die kanonische Profil-Registry liegt unter `app/src/main/assets/vlm-profile-registry.json`; `vlm-profiles.json` ist nur noch der letzte Legacy-Fallback.
- Oeffentliche Remote-Profile werden ueber `GET https://api.owli-ai.com/api/v1/profiles` geladen, lokal gecached und bei Bedarf gegen Registry/Fallback zusammengefuehrt.

---

## 5) Zusammenarbeit mit Codex

- `AGENTS.md` ist die ausfuehrbare Arbeitsanweisung.
- `.codex/config.toml` enthaelt die projektlokalen Codex-Defaults.
- Prompts/Vorlagen: `docs/Prompts-Codex-CLI.md`
- Empfehlung:
  - kleine Commits
  - pro Run nur ein Thema
  - kurze Feedback-Loops
  - keine speculative rewrites
