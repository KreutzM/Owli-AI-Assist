# ToDo & Meilensteine

Status: `[x]` erledigt, `[ ]` offen

## Setup & Infrastruktur
- [x] Gradle-Basis + Compose/CameraX/Coroutines/TFLite-Dependencies ergÃ¤nzt.
- [x] Manifest-Berechtigungen/Konfiguration fÃ¼r Kamera/Audio/Vibration ergÃ¤nzt.
- [x] Basis-Paketstruktur unter `com.owlitech.owli.assist.*` angelegt (camera, pipeline, processing, ml, domain, audio, util, ui).
- [x] Logging-Utility `util/AppLogger`.
- [ ] Dispatcher-Provider fÃ¼r testbares Threading.

## Kamera & Pipeline
- [x] `camera/FrameListener`.
- [x] `camera/CameraFrameSource` mit CameraX Preview + ImageAnalysis, Frame-Weitergabe.
- [x] `pipeline/VisionPipeline` Interface.
- [x] `pipeline/DefaultVisionPipeline` mit einfachem Throttle (ca. 250 ms) und Latest-Wins-Processing.
- [x] ImageProxyâ†’Tensor Helper (YUVâ†’ARGB/Resize/Rotate) im Preprocessor.
- [ ] NMS-/BoundingBox-Utilities.

## Processing & ML
- [x] `processing/ModelInputSpec`.
- [x] `processing/Preprocessor` Interface.
- [x] `processing/DefaultPreprocessor` mit YUVâ†’ARGB, Rotation, optional Downscale.
- [x] `ml/BoundingBox`, `ml/Detection`.
- [x] `ml/ModelSpec`, `ml/DetectorConfig` inkl. Backend-Enum.
- [x] `ml/Detector`, `ml/DetectorFactory` Interfaces.
- [x] `ml/tflite/TfliteTaskDetector` (EfficientDet-Lite2, Task API).
- [ ] Fehler-/Result-Wrapping fÃ¼r Detector-Init/Inference.

## Domain & Audio
- [x] `domain/HazardModels` (HazardLevel, HazardType, Direction, HazardEvent).
- [x] `domain/SceneState`.
- [x] `domain/SceneAnalyzer` Interface + `DefaultSceneAnalyzer`.
- [x] OwliAI: Announce-/Speech-Planner, Uhrzeit/Distanz-Mapping, DE-Labels, IoU-Tracker (EMA/Confidence/Max-Age/Min-Hits).
- [x] `audio/AudioFeedbackEngine` mit Cooldown/Speech-Rate/Spam-Schutz.

## UI & App-Orchestrierung
- [x] DI/Factory fÃ¼r VisionPipeline (`VisionPipelineModule`, AppMode).
- [ ] Test-Fakes (`FakeDetector`, `FakeCameraFrameSource`) im Test-Quellbaum.
- [x] `ui/MainViewModel` (start/stop, StateFlow, Auto-Start nach Rotation).
- [x] UI-Komponenten: `SceneOverlay` (BBox/Status) inkl. Overlay-Labels (Label + Confidence) per Toggle.
- [x] UI-Komponenten: `ControlPanel` (Status).
- [x] `MainActivity`-Integration: CameraX-Preview + Pipeline + Audio-Hooks + Lifecycle + Auto-Restart bei Rotation/Settings-Ã„nderung.
- [x] Settings-Screen (DataStore) mit Reset-to-Defaults und Debug-Toggles.
- [x] Diagnostics-Screen (Live-Metriken, Copy-to-Clipboard Report).

## Assets & Tests
- [x] Assets-Struktur fÃ¼r Modelle/Labels (`app/src/main/assets/models/...`).
- [ ] Initiale Test-Suite/Dependencies (JUnit/Compose test) konfigurieren.
- [x] Unit-Tests: SceneAnalyzer-Heuristiken.
- [ ] Unit-Tests: Pipeline mit Fakes (Frameâ†’SceneState).
- [ ] Unit-Tests: Preprocessor (YUVâ†’RGB/Normalize) mit synthetischen Daten.
- [ ] Unit-Tests: NMS/BoundingBox-Mapper.
- [ ] Manuelle Rotation/Lifecycle-Tests dokumentieren (Start, drehen Portrait/Landscape, Home/ZuruÌˆck, Start/Stop mehrfach, keine Black Screens).
- [ ] Modell-Asset ablegen: `app/src/main/assets/models/efficientdet_lite2_int8.tflite` (COCO, EfficientDet-Lite2).
