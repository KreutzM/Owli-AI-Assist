# ChangeLog – BikeBuddy / BikeAssist

Dieses Dokument beschreibt die Entwicklungs-Historie der App.

Format (angelehnt an Keep a Changelog / SemVer):

- Versionen: MAJOR.MINOR.PATCH
- Jede Version hat ein Datum und eine Liste von Änderungen.

## [Unreleased]

### Added
- VLM policy layer mit Provider-Interface und GPT-5 Reasoning-Retries.
- Dokumentation `docs/VLM.md` fuer Profile, Policies und Retry-Strategie.
- StreamingTtsController fuer fruehen TTS-Start bei VLM-Streaming inkl. Chunking/Dedup.
- Settings fuer VLM-Streaming-TTS und TTS-Pitch.
- VLM-Overlay zeigt das zuletzt verwendete Snapshot-Bild als Hintergrund (50% Helligkeit).
- Neue Vorlese-Profile in `app/src/main/assets/vlm-profiles.json`.
- Navigation Compose und Material Icons fuer die neue UI-Navigation.

### Changed
- VLM-Profile auf neues Schema mit token_policy, parameter_overrides und defaults umgestellt.
- OpenRouter-Parsing nutzt nur message.content fuer UI/TTS; Reasoning bleibt Debug/Telemetry.
- AudioFeedbackEngine nutzt AudioFocus/AudioAttributes und TTS-Prewarm fuer stabileres Streaming.
- On-Device TTS wird bei aktivem VLM-Streaming unterdrueckt (Warnungen werden verworfen).
- Diagnostics nutzt jetzt LocalClipboard (suspend API) statt LocalClipboardManager.
- Default-VLM-Profil auf nano-fast-blind gesetzt.
- Rueck-Button schliesst Overlays, Close-Buttons wurden entfernt.
- UI-Navigation via NavHost mit TopAppBar und Overflow-Menue eingefuehrt.
- Screens in eigene Dateien migriert (Home/Settings/Diagnostics/VLM/Profiles/About), Home-Controls reduziert.

### Fixed
- GPT-5 Reasoning-only Antworten: automatische Retries mit hoeherem Token-Budget und Final-Only Hinweis.
- App-Start-Crash durch zu fruehe VLM-Profil-Initialisierung in MainActivity.
- VLM Default-Profil folgt jetzt dem JSON-default, sofern kein Nutzerprofil gesetzt ist.
- On-Device TTS unterbricht Streaming-TTS nicht mehr zwischen Saetzen (VLM-Queue wird komplett abgewartet).
- VLM kann bei gestoppter Pipeline ein frisches Snapshot anfordern (One-Shot Kamera).
- One-Shot Snapshot startet/stoppt CameraX auf dem Main-Thread (Fix fuer Crash in Background-Thread).

---

## [0.5.0] – 2025-12-24

### Added
- VLM-Profile aus `app/src/main/assets/vlm-profiles.json` inkl. eigenem Prompt je Profil.
- VLM-Profile-Screen zur Auswahl (LazyColumn) und Speicherung als `vlmProfileId`.
- Default-Profil (gpt-4o-mini) als datengetriebene Voreinstellung.

### Changed
- VLM im Raw-Debug-Mode: Antworten werden als Freitext angezeigt (JSON-Parsing deaktiviert).
- OpenRouter-Client robuster bei leeren Antworten/Fehlertexten.
- Dokumentation zu VLM-Profilen und Raw-Debug-Mode aktualisiert.

---

## [0.4.0] – 2025-12-23

### Added
- VLM-On-Demand-Mode mit Snapshot, OpenRouter-Request, JSON-Parsing und UI-Overlay inkl. "VLM" Button.
- VLM-Follow-up-Fragen ueber Session-History (Text-Input).
- SnapshotProvider fuer den letzten preprocessierten Frame (JPEG) in der Pipeline.
- VLM-Konfiguration via `app/src/main/assets/vlm-config.json` + Loader (Model/Prompts/Token/Temperatur).
- Audio-Ausgabe fuer VLM-Antworten (Kurzsatz + Handlungsempfehlung).
- Dokumentation `docs/VLM-Mode.md` zu Ablauf, Schema und Safety-Regeln.

### Changed
- BuildConfig-API-Key-Handling fuer OpenRouter (local.properties) und Manifest um INTERNET-Permission erweitert.

---

## [0.3.0] – 2025-12-14

### Added
- BlindView-Ansagepfad mit AnnouncePlanner, SpeechPlanner und Utterance-Formatter (Uhrzeit/Distanz, DE-Labels).
- IoU-Tracker erweitert (minConfidenceTrack, consecutiveHits, maxTracks) zur Stabilisierung von Position/Confidence.
- Bounding-Box-Overlay zeigt optional Labels + Confidence (Toggle in Settings).
- DataStore-basiertes Settings-Menü (Detector/Tracking/BlindView/TTS/Debug/Pipeline) mit Reset-to-Defaults.
- Diagnostics-Screen mit Live-Metriken (Pipeline/FPS/Detector/TTS/Tracking) und Copy-to-Clipboard Debug-Report.

### Changed
- BlindView-Preview entkoppelt von TTS-Cooldowns (nur Formatter, kein Hash/Gating).
- AudioFeedbackEngine setzt/aktualisiert Speech-Rate (konfigurierbar, Spam-Protection bleibt erhalten).
- Pipeline/Lifecycle: Auto-Restart nach Rotation oder Settings-Änderungen, wenn vorher aktiv.
- Tracking-Filter gegen False Positives (Confidence-EMA, Decay, BBox-Smoothing, Max-Age).

---

## [0.2.0] – 2025-12-13

### Added
- Ampelphasen-Erkennung (HSV, ROI-Inset, Zonenanalyse, Hysterese) mit Phase-Ausgabe im Overlay und Audio.
- UI-Overlay: Traffic-Light-Phase/Confidence, Rotationstext.
- README aktualisiert (Ampel, Preprocessing, Hazard/TTS).

### Changed
- Preprocessing: direkter YUV_420_888→ARGB_8888, Rotation, optional Downscale; Logging entdrosselt.
- Pipeline/Lifecycle robuster (ImageProxy close, Rotation-Tracking, Pipeline-Neuaufbau pro Activity).
- SceneAnalyzer: label-basiertes Hazard-Mapping, Decay, Ampel-Message (Grün nur nach Rot).

---

## [0.1.4] – 2025-12-13

### Added
- README aktualisiert (YUV→RGB Preprocessing ohne JPEG, Rotation/Downscale, TTS/Hazard-Mapping).
- Rotationstext im Overlay (`Rot: <deg>`), Logs für Pipeline/Camera/Preprocessor.

### Changed
- Lifecycle/CameraX: Pipeline/PipelineHandle neu pro Activity, Stop/Start idempotent, CameraFrameSource Logging.
- Preprocessing: performantes YUV_420_888→ARGB_8888 ohne JPEG, Rotation angewendet, optional Downscale.
- Hazard-Mapping label-basiert mit Decay; TTS reset bei Message=null.

---

## [0.1.3] – 2025-12-13

### Added
- README im Projekt-Root mit Setup/Bedienung/Architektur.
- TTS-Reset, damit mehrfach gesprochen wird, Hazard-Decay 800 ms.

### Changed
- Hazard-Mapping label-basiert (Person/Fahrzeug/Ampel), Threshold 0.4.
- Pipeline stop/close aufgeräumt (Detector erst bei close, nicht bei stop).

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
