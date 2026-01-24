# Testing Guide (JVM-first)

Ziel: Fehler früh finden, ohne Device-/Emulator-Läufe als Standard zu benötigen.

## 1) Wo Tests hingehören

- **JVM Unit Tests**: `app/src/test/...` (Default)
  - Domain-/Heuristiklogik, Parser/Formatter, Stabilisierung/Mapping, Tracker, Planner
- **Instrumented Tests**: `app/src/androidTest/...` (nur wenn unvermeidbar und explizit angefordert)

## 2) Standard-Kommandos (Windows / PowerShell)

- Schnell: `./gradlew.bat :app:testDebugUnitTest`
- Full (selten): `./gradlew.bat :app:test`
- Android Lint (statisch): `./gradlew.bat :app:lintDebug`

> Verbot im Standard-Flow: `connectedAndroidTest`, `connectedCheck`, `installDebug`, Emulator/Device-Tasks.

## 3) Test-Policy

- Bugfix ⇒ **Regression Test** (fail → fix → pass)
- Feature/Behavior Change ⇒ **1–3 fokussierte Tests**
- Tests müssen **deterministisch** sein:
  - kein echtes Netzwerk
  - keine `Thread.sleep`
  - Zeit über FakeClock/Injected Clock
  - Randomness vermeiden oder seedbar machen

## 4) Patterns (empfohlen)

- Pure Kotlin Klassen/Funktionen bevorzugen (leicht JVM-testbar)
- Android Abhängigkeiten (Context, TTS, CameraX) als dünne Adapter kapseln
- Bei stateful Logik:
  - kleine Test-Helper
  - klare Arrange/Act/Assert Struktur

## 5) High-Value Test Targets (orientierend)

- `blindview/`: Tracker/Planner/Formatter/Clock/Distance
- `domain/`: Hazard-Entscheidungen, Decay, Ampelstate
- `processing/`: FrameMapping, crop update, dx/dy smoothing, quality gating
- `vlm/`: SSE parser, profile parsing, response parsing
- `audio/`: StreamingTTS Chunking/Queueing

## 6) Wann doch Instrumented Tests?

Nur wenn ein Bug **nur** im Android Runtime-Kontext reproduzierbar ist (z. B. Permission/Lifecycle Interaktion),
und sich kein sinnvoller JVM-Test formulieren lässt. Dann:
- minimaler Scope
- klarer Grund in der Testklasse
- keine langfristige Abhängigkeit auf Emulator in jedem PR
