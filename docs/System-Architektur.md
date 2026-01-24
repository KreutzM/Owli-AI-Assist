# System-Architektur – Owli-AI Assist (Ist-Zustand)

Dieses Dokument beschreibt den **aktuellen Ist-Zustand** der Architektur im Repo (Branch/Stand: `ref/Test-Optimization`).
Ziel: Für ein Team aus **2 Menschen + 1 Codex-Agent** eine gemeinsame, präzise Referenz zu haben, die **mit dem Code übereinstimmt**.

> Hinweis: Falls du eine Ziel-/Soll-Architektur dokumentieren willst, lege dafür eine separate Datei an (z. B. `docs/System-Architektur-Soll.md`).
> Dieses Dokument bleibt bewusst **Ist-orientiert**.

---

## Überblick

Owli-AI Assist ist eine Android-App für blinde Nutzer:innen mit:

- **Live-Kamera (CameraX)** + optionalem Overlay
- **On-Device Objekterkennung** (TFLite Task Vision; vorgesehen: EfficientDet-Lite2)
- **Szenen-/Gefahrenheuristik** (Domain-Logik)
- **Audio-Feedback (TTS)** inkl. Cooldowns/Spam-Schutz
- **Motion-Gating** (IMU) zur Stabilisierung von Tracking/Ansagen
- **VLM On-Demand** via OpenRouter (profilbasiert, optional Autoscan)
- **Diagnostics** (Live-Metriken + Copy-to-Clipboard Report)
- **DataStore Settings** (Feature-Toggles + Parameter)

Wichtig: Die sichtbare CameraX-Preview ist **das Originalbild**. Stabilisierung passiert primär im **Model-Input (448×448)**, der optional als Debug-Preview angezeigt wird.

---

## Paketstruktur (tatsächliche Namespaces)

Basis-Paket: `com.owlitech.owli.assist`

- `camera/`
  - CameraX Integration: Preview + ImageAnalysis
  - liefert Frames an die Pipeline

- `processing/`
  - Preprocessing (ImageProxy → Bitmap)
  - 448×448 Model-Input, FrameMapping (Model↔Source) für Overlay
  - optionale Stabilisierung:
    - IMU Roll-Derotation (quality-gated)
    - Translation-Stabilisierung des Crop-Windows (Low-Res Patch-Matching)

- `ml/`
  - Detector-Abstraktion
  - TFLite Task Vision Detector (EfficientDet-Lite2 vorgesehen)
  - Fallback: FakeDetector, wenn Modell fehlt oder Init scheitert

- `domain/`
  - SceneAnalyzer: Tracking-/Hazard-Logik, Ampel-Status-Ableitung
  - SceneState/Models (HazardLevel, Messages, Ampelphase etc.)

- `blindview/`
  - IoU-Tracker, Smoothing/EMA, Consecutive Hits
  - Objekt-Label-Übersetzung, Clock-Position-Mapping („auf 2 Uhr“)
  - Speech-/Announce-Planung und Formatierung

- `motion/`
  - MotionEstimator: Sensorfusion (Gyro + Rotation Vector)
  - liefert MotionSnapshot (Level/Quality, Roll etc.) für Gating

- `audio/`
  - AudioFeedbackEngine: Android TTS, Cooldown/Rate/Pitch
  - StreamingTtsController: chunked/streaming Ausgabe (u. a. VLM)

- `settings/`
  - DataStore Preferences + Defaults
  - Settings Repository + ViewModels

- `diagnostics/`
  - Sammeln von Live-Metriken (Pipeline, Motion, Stabilisierung, Detections)
  - ReportBuilder für Copy-to-Clipboard

- `vlm/`
  - OpenRouter Client, Profile Loader (JSON), Session, SSE/Streaming Parser
  - On-Demand Queries: Bild + Prompt; Raw-Debug Ausgabe

- `pipeline/`
  - DefaultVisionPipeline orchestriert: Frame → Preprocess → Detect → Analyze
  - SnapshotProvider: Debug-/Diagnostics-Snapshots

- `ui/`
  - Jetpack Compose Screens, Navigation, Overlay, Settings UI, Diagnostics UI
  - MainActivity + ViewModels (Start/Stop, Flows, Status)

---

## Datenfluss zur Laufzeit

### 1) Kamera → Pipeline
1. `camera/CameraFrameSource` startet CameraX Preview + ImageAnalysis.
2. ImageAnalysis liefert `ImageProxy` Frames (typ. KEEP_ONLY_LATEST) an die Pipeline.

### 2) Preprocessing (processing/)
1. YUV → RGB Bitmap
2. Rotation gemäß `ImageInfo.rotationDegrees`
3. optional: IMU Roll-Derotation (nur wenn MotionSnapshot quality ausreichend)
4. optional: Translation-Stabilisierung (Patch-Matching) → aktualisiert Crop-Fenster
5. erzeugt **Model-Input Bitmap (448×448)** + `FrameMapping` (für Overlay)

### 3) Detection (ml/)
- `Detector.detect(input: Bitmap)` liefert Detections (BoundingBoxes, score, labelIndex/label)
- Wenn TFLite-Modell nicht vorhanden/initialisierbar: FakeDetector liefert stabile Dummy-Ergebnisse (für UI/Integration)

### 4) Scene Analysis (domain/ + blindview/)
- `DefaultSceneAnalyzer` verarbeitet Detections:
  - Tracking über `blindview/IouObjectTracker`
  - Label/Clock/Distance Mapping
  - Hazard-Entscheidungen (Warnlevel + Message)
  - Ampelphase per `processing/TrafficLightPhaseClassifier` (HSV-Heuristik) + Stabilisierung/Decay
- Ergebnis: `SceneState` (für UI + Audio)

### 5) Ausgabe: UI + Audio
- UI zeigt:
  - Preview (Original CameraX)
  - Overlay (BoundingBoxes) via `FrameMapping`
  - optional Labels/Confidence
  - optional Debug-Preview des 448×448 Input
  - Status (RealDetector/Fallback)
- Audio:
  - `AudioFeedbackEngine` spricht ausgewählte Messages/Announce-Pläne per TTS
  - Cooldowns verhindern Spam; Parameter sind über Settings konfigurierbar

### 6) Diagnostics
- `diagnostics/` sammelt Snapshots:
  - Stabilisierung: dx/dy/quality/crop
  - Motion: level/quality/roll
  - Pipeline timing / last frame / detection counts
- Diagnostics-Screen zeigt Live-Metriken + Debug-Report.

---

## Konfiguration & Assets

- TFLite Modell (wenn genutzt): `app/src/main/assets/models/efficientdet_lite2_int8.tflite`
- Labels: `app/src/main/assets/models/labels.txt`
- VLM Profile: `app/src/main/assets/vlm-profiles.json`
- OpenRouter API Key: `OPENROUTER_API_KEY` in `local.properties` (nicht committen)

---

## Tests & Quality Gates (Ist-Zustand)

- JVM Unit-Tests: `app/src/test/...`
  - u. a. BlindView-Tracker/Planner/Formatter, SceneAnalyzer, StreamingTTS
- Instrumented Tests: aktuell nur Minimal-Beispiele (keine Device-Tests als Standard-Workflow)
- Empfohlene Checks (siehe `AGENTS.md`):
  - `:app:testDebugUnitTest`
  - `:app:lintDebug` (wenn relevant)
  - `:app:assembleDebug` (wenn UI/Resources/Manifest/Gradle betroffen)

---

## Grenzen des aktuellen Stands

- Stabilisierung wird nicht in der CameraX-Preview „gerendert“, sondern im Model-Input. (Debug-Preview existiert.)
- Hazard-Logik ist heuristisch und nicht als Sicherheitsprodukt zu verstehen.
- Haptik ist (Stand jetzt) nicht Kernbestandteil.
