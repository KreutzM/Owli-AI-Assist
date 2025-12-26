# BikeBuddy / BikeAssist

BikeBuddy ist eine Android-Demo-App fuer ein Fahrrad-Assistenzsystem mit On-Device-Computer-Vision. Die App nutzt CameraX fuer die Live-Preview, eine Vision-Pipeline (Preprocessing -> Detector -> SceneAnalyzer/BlindView) und gibt Warnungen bzw. BlindView-Ansagen per TTS aus. Als Beispielmodell ist EfficientDet-Lite2 (COCO) eingebunden; bei fehlendem Modell faellt die App auf einen FakeDetector zurueck.

## Funktionsumfang
- Live-Kamera-Preview (CameraX) mit Bounding-Box-Overlay
- Optionales Labeling der BBoxen (Klasse + Confidence) per Toggle
- Objekt-Erkennung (EfficientDet-Lite2, COCO 80 Klassen)
- BlindView-Modus: sagt alle erkannten Objekte auf Deutsch mit Uhrzeit-Position an; IoU-Tracker glaettet BBox/Position, filtert Kurzzeit-Noise (Consecutive Hits, Confidence-EMA)
- Hazard-Auswertung (Basis): Personen/Fahrzeuge -> Warnung; Ampeln -> Info/Phase
- Ampelphasen-Erkennung (rot/gruen) per HSV-Heuristik (stabilisierte Phase im Overlay)
- TTS-Ausgabe mit Cooldown/Spam-Schutz, konfigurierbarer Sprechgeschwindigkeit; Status-Anzeige (RealDetector/Fallback)
- DataStore-basiertes Settings-Menue (Detector/Tracking/BlindView/TTS/Debug/Pipeline) inkl. Reset-to-Defaults
- Diagnostics-Screen mit Live-Metriken und Copy-to-Clipboard Debug-Report
- Start/Stop der Pipeline; Decay-Logik fuer Hazards; Auto-Restart nach Rotation/Settings-Aenderung
- VLM-On-Demand (OpenRouter) mit Profil-Auswahl; Antworten im Raw-Debug-Mode

## Architektur (kurz)
- `camera`: CameraFrameSource (CameraX Preview + ImageAnalysis)
- `processing`: Preprocessor (ImageProxy -> Bitmap), TrafficLightPhaseClassifier (HSV)
- `ml`: Detector-Interface; TfliteTaskDetector (EfficientDet-Lite2) + FakeDetector
- `blindview`: Label/Clock/Distanz-Mapper, Announce-/Speech-Planner, IoU-Tracker (EMA, Confidence-Filter)
- `domain`: DefaultSceneAnalyzer (BlindView-Planung, Hazard-Mapping, Decay)
- `pipeline`: DefaultVisionPipeline (Frame->Bitmap->Detect->Analyze), VisionPipelineModule (DI + Real/Fake Auswahl, AppMode)
- `audio`: AudioFeedbackEngine (TTS, Cooldown, SpeechRate, pendingMessage)
- `ui`: MainActivity (Compose: PreviewView + Overlay + ControlPanel + BlindView-Preview + Settings), MainViewModel (Flows, Start/Stop/Status)
- `settings`: DataStore-basierte Settings (Detector/Tracking/BlindView/TTS/Debug/Pipeline) mit SettingsViewModel

## Voraussetzungen
- Android Studio (AGP 8.x), Kotlin 2.0.x, Compose aktiviert
- Geraet/Emulator mit Kamera (Emulator: Virtual Camera oder Webcam)
- Android 12 oder hoeher empfohlen

## Installation & Build
1. Repository oeffnen.
2. Modell ablegen: `app/src/main/assets/models/efficientdet_lite2_int8.tflite`
3. Labels liegen unter `app/src/main/assets/models/labels.txt` (COCO-80).
4. Build: `./gradlew :app:assembleDebug`
5. Installieren und starten (ADB/Android Studio).

## Bedienung
1. App starten, Kamera-Permission erlauben.
2. Start-Button druecken -> Pipeline startet, Preview erscheint (nach Rotation auto-restart).
3. Status-Anzeige zeigt, ob RealDetector aktiv ist oder Fallback (FakeDetector).
4. Bounding-Box-Overlay zeigt erkannte Objekte; optional Labels/Confidence via Toggle (Settings).
5. BlindView-Preview zeigt die aktuelle Ansage (Debug).
6. Overflow-Menue (TopAppBar) -> Settings/Diagnostics/About oeffnen.
7. VLM ueber TopAppBar -> Szene beschreiben lassen, Follow-up fragen; Profil-Auswahl in Settings.
8. Rueck-Button schliesst Unterfenster (Settings/Diag/VLM); App-Ende erst im Hauptfenster.
9. Stop-Button -> Pipeline stoppt, Overlay/State wird zurueckgesetzt.

## Konfiguration
- Settings via DataStore (persistiert): Detector/Tracking/BlindView/TTS/Debug/Pipeline-Intervall; Reset im Settings-Screen
- Detector: `TfliteDetectorOptions` (Threads, NNAPI), Pfad: `models/efficientdet_lite2_int8.tflite` (aus Settings steuerbar)
- BlindView: `BlindViewConfig` (minConfidence, minConfidenceTrack, IoU-Threshold, bboxSmoothingAlpha, minConsecutiveHits, maxDetectionsPerFrame, maxTracks, Speak-Intervalle, TTS-Speech-Rate, Decay) via Settings anpassbar
- Hazard (Basis): DefaultSceneAnalyzer `confidenceThreshold = 0.4`, Decay 800 ms. Mapping: Person -> Personenwarnung, Fahrzeugklassen -> Fahrzeugwarnung, Ampel -> Info.
- TTS: Cooldown 2500 ms, Speech-Rate konfigurierbar (Default 2.0, via Settings); BlindView nutzt Hash/Cooldown fuer Anti-Spam.
- Preprocessing: YUV_420_888 -> ARGB_8888 (ohne JPEG-Roundtrip), Rotation wird angewendet, optional Downscale.
- Ampel: TrafficLightPhaseClassifier (ROI-Inset, Zonenanalyse rot/oben, gruen/unten, Hysterese mit stabiler Phase).
- VLM: Profile und Prompts in `app/src/main/assets/vlm-profiles.json` (On-Demand, Raw-Debug-Mode).

## Hinweise / Fehlerquellen
- Fehlt das Modell-Asset, wird automatisch der FakeDetector verwendet (Status-Anzeige).
- TTS braucht ggf. Sekunden bis READY; pendingMessage wird erst bei Ready gesprochen.
- Kurz aufblitzende Objekte (1 Frame) werden durch minConsecutiveHits + minConfidenceTrack nicht angesagt.
- Rotation: Overlay zeigt `Rot: <deg>`, Pipeline setzt Rotation pro Frame; Lifecycle-robust fuer Drehen/Stop/Start.
- Diagnostics: Report enthaelt keine persoenlichen Daten; Clipboard muss erlaubt sein.

## Lizenz / Nutzung
Interner Demo-/Prototyp-Status; keine Produktionsfreigabe, keine Gewaehrleistung. Modelle/Assets nur verwenden, wenn lizenzrechtlich geklaert.
