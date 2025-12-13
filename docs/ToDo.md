# ToDo & Meilensteine

Status: `[x]` erledigt, `[ ]` offen

## Setup & Infrastruktur
- [x] Gradle-Basis + Compose/CameraX/Coroutines/TFLite-Dependencies ergänzen.
- [x] Manifest-Berechtigungen/Konfiguration für Kamera/Audio/Vibration ergänzen.
- [x] Basis-Paketstruktur unter `com.example.bikeassist.*` anlegen (camera, pipeline, processing, ml, domain, audio, util, ui placeholder).
- [x] Logging-Utility `util/AppLogger`.
- [ ] Dispatcher-Provider für testbares Threading.

## Kamera & Pipeline
- [x] `camera/FrameListener`.
- [x] `camera/CameraFrameSource` mit CameraX Preview + ImageAnalysis, Frame-Weitergabe.
- [x] `pipeline/VisionPipeline` Interface.
- [x] `pipeline/DefaultVisionPipeline` Skeleton (Logik noch offen).
- [ ] Pipeline-Throttle/FPS-Limiter (`PipelineConfig` o.ä.).
- [ ] ImageProxy→Tensor Helper (YUV→RGB, Resize, Normalize).
- [ ] NMS-/BoundingBox-Utilities.

## Processing & ML
- [x] `processing/ModelInputSpec`.
- [x] `processing/Preprocessor` Interface.
- [ ] `processing/DefaultPreprocessor` (YUV→RGB, Resize, Normalize).
- [x] `ml/BoundingBox`, `ml/Detection`.
- [x] `ml/ModelSpec`, `ml/DetectorConfig` inkl. Backend-Enum.
- [x] `ml/Detector`, `ml/DetectorFactory` Interfaces.
- [ ] `ml/tflite/YoloTfliteDetector` Skeleton mit Load/Infer/NMS-Hooks.
- [ ] Fehler-/Result-Wrapping für Detector-Init/Inference.

## Domain & Audio
- [x] `domain/HazardModels` (HazardLevel, HazardType, Direction, HazardEvent).
- [x] `domain/SceneState`.
- [x] `domain/SceneAnalyzer` Interface.
- [x] `domain/DefaultSceneAnalyzer` Stub.
- [x] `audio/AudioFeedbackEngine` Stub (TTS-Cooldown TODO).
- [ ] Audio-Cooldown-/Config-Klasse ergänzen.
- [x] AudioFeedbackEngine robuster (ttsReady/pendingMessage Logging).

## UI & App-Orchestrierung
- [ ] DI/Factory für VisionPipeline (z. B. `VisionPipelineModule`).
- [ ] Test-Fakes (`FakeDetector`, `FakeCameraFrameSource`) im Test-Quellbaum.
- [x] `ui/MainViewModel` (start/stop, StateFlow).
- [x] UI-Komponenten: `SceneOverlay` (BBox/Status).
- [x] UI-Komponenten: `ControlPanel` (Start/Stop/Status).
- [x] `MainActivity`-Integration: CameraX-Preview + Pipeline + Audio-Hooks + Lifecycle.
- [x] Debug-BBox-Overlay mit Detections (gezeichnete BBoxen).
- [x] Lifecycle-Klarstellungen in Activity (stop onStop, audio close onDestroy).

## Assets & Tests
- [ ] Assets-Struktur für Modelle/Labels (`app/src/main/assets/models/...`).
- [ ] Initiale Test-Suite/Dependencies (JUnit/Compose test) konfigurieren.
- [ ] Unit-Tests: SceneAnalyzer-Heuristiken.
- [ ] Unit-Tests: Pipeline mit Fakes (Frame→SceneState).
- [ ] Unit-Tests: Preprocessor (YUV→RGB/Normalize) mit synthetischen Daten.
- [ ] Unit-Tests: NMS/BoundingBox-Mapper.
