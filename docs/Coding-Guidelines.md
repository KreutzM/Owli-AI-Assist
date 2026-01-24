# Coding-Guidelines – Owli-AI Assist

Dieses Dokument ergänzt `AGENTS.md` mit technischen Konventionen, die **für Menschen und Codex** gelten.
Wenn es einen Konflikt gibt: **`AGENTS.md` gewinnt** (weil es die ausführbare Arbeitsanweisung ist).

Stand: Branch `ref/Test-Optimization`.

---

## 1) Grundprinzipien

- **Small, reviewable changes**: lieber 3 kleine Commits als 1 großen.
- **Android-first Robustheit**: Lifecycle, Permissions, Threads sauber behandeln.
- **Testbarkeit**: Domain-/Heuristiklogik möglichst als **pure Kotlin** (JVM-testbar).
- **Keine Secrets**: niemals Keys in Code/Doku/Logs.

---

## 2) Projektstruktur (Kurz)

- `camera/`: CameraX (Preview + ImageAnalysis)
- `processing/`: Frame → Bitmap, 448×448 Input, Mapping, Stabilisierung
- `ml/`: Detector Interface + TFLite + FakeDetector
- `domain/`: SceneAnalyzer, Hazard/Ampel-Models
- `blindview/`: Tracking, Clock/Label/Distance, Speech-/Announce-Planung
- `motion/`: IMU MotionSnapshot
- `pipeline/`: Orchestrierung (Preprocess → Detect → Analyze)
- `audio/`: TTS Engine + Streaming Controller
- `settings/`: DataStore + Defaults
- `diagnostics/`: Live-Metriken + Report
- `vlm/`: OpenRouter, Profiles, SSE/Streaming
- `ui/`: Compose Screens + Overlays

---

## 3) Kotlin/Android Konventionen

- Kotlin: idiomatisch, null-sicher, keine unnötigen `!!`.
- Coroutines:
  - keine heavy work auf Main thread
  - klare Dispatcher (IO/Default/Main) je nach Aufgabe
- Logging:
  - sparsam, aber diagnostisch hilfreich
  - keine sensitiven Daten
- Compose:
  - UI State als `StateFlow`/`Flow` im ViewModel
  - keine direkten Side-Effects im Composable ohne `LaunchedEffect`/`DisposableEffect`

---

## 4) Tests (JVM-first)

Siehe auch `docs/TESTING.md`.

- Neue Logik in `domain/`, `blindview/`, `processing/`, Parser/Formatter → **JVM Unit Tests** unter `app/src/test`.
- Tests müssen deterministisch sein:
  - kein echtes Netzwerk
  - keine Sleeps
  - Zeit via FakeClock/Injected Clock
- Bugfix: Regression-Test (fail → fix → pass).

---

## 5) Statische Checks

- Android Lint ist Teil des Qualitätsgates (siehe `AGENTS.md`).
- Lint-Suppressions nur:
  - minimal,
  - mit kurzer Begründung im Kommentar,
  - nicht als „Noise-Beseitigung“ ohne Verständnis.

---

## 6) Abhängigkeiten & Architekturänderungen

- Keine Dependency-Upgrades oder neue Libraries ohne explizite Anweisung.
- Keine Paket-/Namespace-Renames ohne explizite Anweisung.
- Bei Architekturänderungen: `docs/System-Architektur.md` aktualisieren.

---

## 7) Codex/CLI Hinweise

- Default Modell: `gpt-5.2-codex` (Reasoning: medium; high/xhigh nur bei Bedarf).
- Windows: `gradlew.bat` nutzen; keine Bash-only Syntax.
- Standardchecks: siehe `AGENTS.md`.
