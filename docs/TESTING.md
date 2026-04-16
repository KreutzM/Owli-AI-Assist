# Testing Guide (JVM-first)

Ziel: Fehler frueh finden, ohne Device-/Emulator-Laeufe als Standard zu benoetigen.

## 1) Wo Tests hingehoeren

- **JVM Unit Tests**: `app/src/test/...` (Default)
  - VLM-Parsing, Profile/Registry-Logik, Settings, UI-Hilfslogik, Audio-Logik
- **Instrumented Tests**: `app/src/androidTest/...` (nur wenn unvermeidbar und explizit angefordert)

## 2) Standard-Kommandos (Windows / PowerShell)

- Schnell: `./gradlew.bat :app:testDebugUnitTest`
- Full (selten): `./gradlew.bat :app:test`
- Android Lint (statisch): `./gradlew.bat :app:lintDebug`

> Verbot im Standard-Flow: `connectedAndroidTest`, `connectedCheck`, `installDebug`, Emulator/Device-Tasks.

## 3) Test-Policy

- Bugfix -> **Regression Test** (fail -> fix -> pass)
- Feature/Behavior Change -> **1-3 fokussierte Tests**
- Tests muessen **deterministisch** sein:
  - kein echtes Netzwerk
  - keine `Thread.sleep`
  - Zeit ueber FakeClock/Injected Clock
  - Randomness vermeiden oder seedbar machen

## 4) Patterns (empfohlen)

- Pure Kotlin Klassen/Funktionen bevorzugen (leicht JVM-testbar)
- Android-Abhaengigkeiten (Context, TTS, CameraX, WebView) als duenne Adapter kapseln
- Bei stateful Logik:
  - kleine Test-Helper
  - klare Arrange/Act/Assert Struktur

## 5) High-Value Test Targets (orientierend)

- `vlm/`: SSE-Parser, Backend-Streaming-Fallbacks, Profile/Registry-Fallbacks, Response-Parsing, Attachment-Handling
- `settings/`: App-Defaults, Transportnormalisierung, QR-Parsing, Key-Blob-/Key-Info-Parser
- `audio/`: StreamingTTS Chunking/Queueing
- `ui/screens/`: zustandsarme Composer-/Screen-Logik, die ohne Android-Runtime testbar ist

Die aktuelle Teststruktur im Repo liegt schwerpunktmaessig unter `vlm/`, `settings/`, `audio/` und `ui/screens/`.

## 6) Wann doch Instrumented Tests?

Nur wenn ein Bug **nur** im Android-Runtime-Kontext reproduzierbar ist, z. B. bei Permission-/Lifecycle-Interaktion,
CameraX, WebView oder TTS-Framework-Verhalten, und sich kein sinnvoller JVM-Test formulieren laesst. Dann:
- minimaler Scope
- klarer Grund in der Testklasse
- keine langfristige Abhaengigkeit auf Emulator in jedem PR
