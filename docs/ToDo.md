# ToDo & Meilensteine

Status: `[x]` erledigt, `[ ]` offen

## Setup & Infrastruktur
- [x] Gradle-Basis + Compose/CameraX/Coroutines/TFLite-Dependencies ergänzt.
- [x] Manifest-Berechtigungen/Konfiguration für Kamera/Audio/Vibration ergänzt.
- [x] Basis-Paketstruktur unter `com.example.bikeassist.*` angelegt (camera, pipeline, processing, ml, domain, audio, util, ui).
- [x] Logging-Utility `util/AppLogger`.
- [ ] Dispatcher-Provider für testbares Threading.

## Kamera & Pipeline
- [x] `camera/FrameListener`.
- [x] `camera/CameraFrameSource` mit CameraX Preview + ImageAnalysis, Frame-Weitergabe.
- [x] `pipeline/VisionPipeline` Interface.
- [x] `pipeline/DefaultVisionPipeline` mit einfachem Throttle (ca. 250 ms) und Latest-Wins-Processing.
- [x] ImageProxy→Tensor Helper (YUV→ARGB/Resize/Rotate) im Preprocessor.
- [ ] NMS-/BoundingBox-Utilities.

## Processing & ML
- [x] `processing/ModelInputSpec`.
- [x] `processing/Preprocessor` Interface.
- [x] `processing/DefaultPreprocessor` mit YUV→ARGB, Rotation, optional Downscale.
- [x] `ml/BoundingBox`, `ml/Detection`.
- [x] `ml/ModelSpec`, `ml/DetectorConfig` inkl. Backend-Enum.
- [x] `ml/Detector`, `ml/DetectorFactory` Interfaces.
- [x] `ml/tflite/TfliteTaskDetector` (EfficientDet-Lite2, Task API).
- [ ] Fehler-/Result-Wrapping für Detector-Init/Inference.

## Domain & Audio
- [x] `domain/HazardModels` (HazardLevel, HazardType, Direction, HazardEvent).
- [x] `domain/SceneState`.
- [x] `domain/SceneAnalyzer` Interface + `DefaultSceneAnalyzer`.
- [x] BlindView: Announce-/Speech-Planner, Uhrzeit/Distanz-Mapping, DE-Labels, IoU-Tracker (EMA/Confidence/Max-Age/Min-Hits).
- [x] `audio/AudioFeedbackEngine` mit Cooldown/Speech-Rate/Spam-Schutz.

## UI & App-Orchestrierung
- [x] DI/Factory für VisionPipeline (`VisionPipelineModule`, AppMode).
- [ ] Test-Fakes (`FakeDetector`, `FakeCameraFrameSource`) im Test-Quellbaum.
- [x] `ui/MainViewModel` (start/stop, StateFlow, Auto-Start nach Rotation).
- [x] UI-Komponenten: `SceneOverlay` (BBox/Status) inkl. Overlay-Labels (Label + Confidence) per Toggle.
- [x] UI-Komponenten: `ControlPanel` (Start/Stop/Status).
- [x] `MainActivity`-Integration: CameraX-Preview + Pipeline + Audio-Hooks + Lifecycle + Auto-Restart bei Rotation/Settings-Änderung.
- [x] Settings-Screen (DataStore) mit Reset-to-Defaults und Debug-Toggles.
- [x] Diagnostics-Screen (Live-Metriken, Copy-to-Clipboard Report).

## Assets & Tests
- [x] Assets-Struktur für Modelle/Labels (`app/src/main/assets/models/...`).
- [ ] Initiale Test-Suite/Dependencies (JUnit/Compose test) konfigurieren.
- [x] Unit-Tests: SceneAnalyzer-Heuristiken.
- [ ] Unit-Tests: Pipeline mit Fakes (Frame→SceneState).
- [ ] Unit-Tests: Preprocessor (YUV→RGB/Normalize) mit synthetischen Daten.
- [ ] Unit-Tests: NMS/BoundingBox-Mapper.
- [ ] Manuelle Rotation/Lifecycle-Tests dokumentieren (Start, drehen Portrait/Landscape, Home/Zurück, Start/Stop mehrfach, keine Black Screens).
- [ ] Modell-Asset ablegen: `app/src/main/assets/models/efficientdet_lite2_int8.tflite` (COCO, EfficientDet-Lite2).
