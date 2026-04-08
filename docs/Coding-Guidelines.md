# Coding-Guidelines - Owli-AI Assist

Dieses Dokument ergaenzt `AGENTS.md` mit technischen Konventionen, die fuer Menschen und Codex gelten.
Wenn es einen Konflikt gibt: `AGENTS.md` gewinnt.

Stand: VLM-only App-Flow.

## 1) Grundprinzipien

- Small, reviewable changes: lieber 3 kleine Commits als 1 grossen.
- Android-first Robustheit: Lifecycle, Permissions und Threads sauber behandeln.
- Testbarkeit: Parser, Formatter und ViewModel-nahe Logik moeglichst JVM-testbar halten.
- Keine Secrets in Code, Doku oder Logs.

## 2) Projektstruktur (Kurz)

- `ui/`: Compose Screens, Navigation und ViewModels fuer den VLM-Flow
- `vlm/`: OpenRouter, Profile, Parsing und Session-State
- `audio/`: TTS Engine + Streaming Controller
- `settings/`: DataStore + Defaults
- `tools/`: kleine Hilfswerkzeuge fuer Profile und Repo-Workflow

## 3) Kotlin/Android Konventionen

- Kotlin: idiomatisch, null-sicher, keine unnoetigen `!!`.
- Coroutines:
  - keine heavy work auf dem Main Thread
  - klare Dispatcher (IO/Default/Main) je nach Aufgabe
- Logging:
  - sparsam und diagnostisch hilfreich
  - keine sensitiven Daten
- Compose:
  - UI State als `StateFlow`/`Flow` im ViewModel
  - keine direkten Side-Effects im Composable ohne `LaunchedEffect`/`DisposableEffect`

## 4) Tests (JVM-first)

Siehe auch `docs/TESTING.md`.

- Parser, Formatter, ViewModel-Logik und kleine Hilfsfunktionen bevorzugt als JVM-Unit-Tests unter `app/src/test`.
- Tests muessen deterministisch sein:
  - kein echtes Netzwerk
  - keine Sleeps
  - Zeit via FakeClock oder klarer Entkopplung
- Bugfix: Regression-Test, wenn der Scope das sinnvoll zulaesst.

## 5) Statische Checks

- Android Lint ist Teil des Qualitaetsgates (siehe `AGENTS.md`).
- Lint-Suppressions nur:
  - minimal,
  - mit kurzer Begruendung im Kommentar,
  - nicht als reine Noise-Beseitigung ohne Verstaendnis.

## 6) Abhaengigkeiten & Architekturaenderungen

- Keine Dependency-Upgrades oder neue Libraries ohne explizite Anweisung.
- Keine Paket-/Namespace-Renames ohne explizite Anweisung.
- Bei groesseren Architektur-Aenderungen die VLM-only-Dokumentation in `README.md` und `docs/DEVELOPMENT.md` mitziehen.

## 7) Codex/CLI Hinweise

- Default Modell und Reasoning richten sich nach `AGENTS.md` und `.codex/config.toml`.
- Windows: `gradlew.bat` nutzen; keine Bash-only Syntax.
- Standardchecks: siehe `AGENTS.md`.
