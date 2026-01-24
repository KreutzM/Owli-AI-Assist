# Development Guide (Team)

Dieses Dokument ist die **einzige** Einstiegstelle für den täglichen Workflow (2 Menschen + 1 Codex-Agent).

## 1) Quickstart

### Voraussetzungen
- Android Studio (AGP 8.x), Android SDK
- JDK (passend zur Android-Toolchain; Android Studio bringt i. d. R. eine kompatible JDK mit)
- Windows 10/11 + PowerShell (oder WSL2)

### Secrets
- `OPENROUTER_API_KEY=...` in `local.properties` (nicht committen).

### Modell
- Lege das TFLite Modell unter `app/src/main/assets/models/efficientdet_lite2_int8.tflite` ab.  
  Siehe `docs/MODEL-ASSETS.md`.

### Build (PowerShell)
- `gradlew.bat :app:assembleDebug`

---

## 1a) VLM-First (Beta UX)
- App startet standardmaessig im VLM-Modus.
- Offline Detector ist nur sichtbar, wenn aktiviert: Settings -> VLM Settings -> Developer / Experimental -> "Enable Offline Detector (Experimental)".
- Detector Settings sind im VLM-Settings-Screen verlinkt ("Open Detector Settings") und zusaetzlich im Top-Menue sichtbar, sobald aktiviert.

---

## 2) Daily Workflow (empfohlen)

### Branching
- Feature/Fix: `feat/...`, `fix/...`
- Refactor/Quality: `ref/...` (z. B. `ref/Test-Optimization`)
- Mergen: per PR / Review (auch im 2er-Team sinnvoll)

### Definition of Done pro Commit
- **klein & fokussiert** (eine logische Änderung)
- **buildbar**
- mindestens: `./gradlew.bat :app:testDebugUnitTest`
- wenn relevant: `./gradlew.bat :app:lintDebug`
- wenn UI/Resources/Manifest/Gradle betroffen: `./gradlew.bat :app:assembleDebug`

> Wichtig: Keine automatischen Device-/Emulator-Tasks im Standard-Flow (`connectedAndroidTest`, `connectedCheck`, `installDebug` usw.)

---

## 3) Fast Checks vs Full Checks

### Fast (Default)
- `./gradlew.bat :app:testDebugUnitTest`

### Zusätzlich bei Android-Komponenten / Public API Änderungen
- `./gradlew.bat :app:lintDebug`

### Zusätzlich bei UI/Resources/Manifest/Gradle Änderungen
- `./gradlew.bat :app:assembleDebug`

### Full (vor Merge oder großen Refactors)
- `./gradlew.bat :app:check`  
  (oder: `:app:test` + `:app:lintDebug` + `:app:assembleDebug`)

---

## 4) Debugging / Diagnostics

- Diagnostics Screen: Live-Metriken + Copy-to-Clipboard Report
- Wichtige Debug-Hinweise:
  - CameraX Preview zeigt **Originalbild**
  - Stabilisierung passiert im **448×448 Model-Input** (Debug-Preview vorhanden)
  - FakeDetector ist aktiv, wenn das Modell fehlt → Statusanzeige beachten

---

## 5) Zusammenarbeit mit Codex

- `AGENTS.md` ist die „ausführbare“ Arbeitsanweisung.
- Prompts/Vorlagen: `docs/Prompts-Codex-CLI.md`
- Empfehlung:
  - kleine Commits
  - kurze Feedback-Loops
  - keine speculative rewrites
