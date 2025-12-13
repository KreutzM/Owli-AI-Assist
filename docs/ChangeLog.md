# ChangeLog – BikeBuddy / BikeAssist

Dieses Dokument beschreibt die Entwicklungs-Historie der App.

Format (angelehnt an Keep a Changelog / SemVer):

- Versionen: MAJOR.MINOR.PATCH
- Jede Version hat ein Datum und eine Liste von Änderungen.

## [Unreleased]

### Added
- Assets-Struktur unter `app/src/main/assets/models/` mit Beispiel-Labels.
- Unit-Test für `DefaultSceneAnalyzer` (Hazard/Messages).
- TFLite Task Vision Detector (`TfliteTaskDetector`) mit EfficientDet-Lite2-Konfiguration und Fallback auf FakeDetector.
- Bitmap-basierter Preprocessor für ImageProxy→Bitmap.
- UI zeigt Detector-Status (Real/Fallback) an; Modell-Asset wird geprüft (`models/efficientdet_lite2_int8.tflite`), Fallback wird gemeldet.
- Labels-Datei (`labels.txt`) im Modell-Asset-Pfad wiederhergestellt.

### Changed
- Preprocessor/Detector-Interfaces angepasst auf Bitmap-Input, Pipeline nutzt echten Detector-Slot.
- Gradle-Dependency: `tensorflow-lite-task-vision` ergänzt.
- Pipeline schließt ImageProxy sicher, nutzt detect(bitmap).

---

## [0.1.2] – 2025-12-10

### Added
- Einfache FPS-Drosselung (ca. 250 ms) in `DefaultVisionPipeline`.
- Placeholder `DefaultPreprocessor` für zukünftiges YUV→RGB/Normalize.
- Detection-Overlay zeichnet Bounding Boxes über der Preview.

### Changed
- CameraFrameSource stoppt Executor beim Stopp, Pipeline-Guards für mehrfaches Starten/Stoppen.

---

## [0.1.1] – 2025-12-10

### Added
- CameraX-gestützte `CameraFrameSource` mit Preview- und ImageAnalysis-UseCases.
- Skelett-Implementierungen für Vision-Pipeline, Processing, ML-Modelle, Domain-Hazard-Logik, Audio-Engine und Logging-Utility.
- ToDo-Tracker unter `docs/ToDo.md` mit Meilenstein-Status.
- Demo-Pipeline mit `FakePreprocessor` und `FakeDetector` für lauffähigen Vertical Slice ohne echte ML-Assets.
- Minimalheuristik im `DefaultSceneAnalyzer` für Hazard-Level/Sprachmeldung.
- Android-TTS in `AudioFeedbackEngine` mit einfachem Cooldown.
- `MainViewModel` mit StateFlows und Start/Stop-Steuerung der Pipeline.
- Compose-Demo-UI in `MainActivity` (CameraPreview, Overlay-Text, ControlPanel).
- TTS-Init robuster (pendingMessage, ttsReady-Flag, Logging) und UI-Running-Status-Update nach erfolgreichem Start.
- Detection-Overlay zeichnet Bounding Boxes; CameraFrameSource bindet SurfaceProvider und Executor-Shutdown in stop().

### Changed
- Version Catalog um CameraX-, Coroutines- und TFLite-Dependencies erweitert; `app/build.gradle.kts` entsprechend eingebunden.
- Manifest um Kamera-Permission und Kamera-Feature ergänzt.

---

## [0.1.0] – 2025-12-10

### Added
- Initiales Android-Projekt angelegt (BikeBuddy-Template).
- Architektur-Skelette für Camera, Pipeline, Processing, ML, Domain, Audio, Util.
- Basis-Dependencies für Compose, CameraX, Coroutines, TFLite.

### Changed

### Fixed
