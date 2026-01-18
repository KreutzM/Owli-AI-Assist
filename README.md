# Owli-AI Assist

Owli-AI Assist ist eine Android-App fuer blinde Nutzer als AI Assistenz-App mit On-Device-Computer-Vision. Die App nutzt CameraX fuer die Live-Preview, eine Vision-Pipeline (Preprocessing -> Detector -> SceneAnalyzer/OwliAI) und gibt Warnungen bzw. OwliAI-Ansagen per TTS aus. Als Beispielmodell ist EfficientDet-Lite2 (COCO) eingebunden; bei fehlendem Modell faellt die App auf einen FakeDetector zurueck.

## Funktionsumfang
- Live-Kamera-Preview (CameraX) mit Bounding-Box-Overlay
- Optionales Labeling der BBoxen (Klasse + Confidence) per Toggle
- Objekt-Erkennung (EfficientDet-Lite2, COCO 80 Klassen)
- OwliAI-Modus: sagt alle erkannten Objekte auf Deutsch mit Uhrzeit-Position an; IoU-Tracker glaettet BBox/Position, filtert Kurzzeit-Noise (Consecutive Hits, Confidence-EMA)
- Hazard-Auswertung (Basis): Personen/Fahrzeuge -> Warnung; Ampeln -> Info/Phase
- Ampelphasen-Erkennung (rot/gruen) per HSV-Heuristik (stabilisierte Phase im Overlay)
- TTS-Ausgabe mit Cooldown/Spam-Schutz, konfigurierbarer Sprechgeschwindigkeit; Status-Anzeige (RealDetector/Fallback)
- IMU Motion-Gating (Gyro/Rotation Vector) fuer stabileres Tracking und Ansagen ohne Bild-Warp
- DataStore-basiertes Settings-Menue (Detector/Tracking/OwliAI/TTS/Debug/Pipeline) inkl. Reset-to-Defaults
- Diagnostics-Screen mit Live-Metriken und Copy-to-Clipboard Debug-Report
- Start/Stop der Pipeline; Decay-Logik fuer Hazards; Auto-Restart nach Rotation/Settings-Aenderung
- VLM-On-Demand (OpenRouter) mit Profil-Auswahl; Antworten im Raw-Debug-Mode
- VLM-Autoscan (Auto-Toggle) fuer Profile mit `auto_scan`

## Architektur (kurz)
- `camera`: CameraFrameSource (CameraX Preview + ImageAnalysis)
- `processing`: Preprocessor (ImageProxy -> Bitmap), TrafficLightPhaseClassifier (HSV)
- `ml`: Detector-Interface; TfliteTaskDetector (EfficientDet-Lite2) + FakeDetector
- Ansage: Label/Clock/Distanz-Mapper, Announce-/Speech-Planner, IoU-Tracker (EMA, Confidence-Filter)
- `domain`: DefaultSceneAnalyzer (OwliAI-Planung, Hazard-Mapping, Decay)
- `motion`: MotionEstimator (IMU Samples, MotionSnapshot)
- `pipeline`: DefaultVisionPipeline (Frame->Bitmap->Detect->Analyze), VisionPipelineModule (DI + Real/Fake Auswahl, AppMode)
- `audio`: AudioFeedbackEngine (TTS, Cooldown, SpeechRate, pendingMessage)
- `ui`: MainActivity (Compose: PreviewView + Overlay + ControlPanel + OwliAI-Preview + Settings), MainViewModel (Flows, Start/Stop/Status)
- `settings`: DataStore-basierte Settings (Detector/Tracking/OwliAI/TTS/Debug/Pipeline) mit SettingsViewModel

## Voraussetzungen
- Android Studio (AGP 8.x), Kotlin 2.0.x, Compose aktiviert
- Geraet/Emulator mit Kamera (Emulator: Virtual Camera oder Webcam)
- Android 12 oder hoeher empfohlen

## Installation & Build
1. Repository oeffnen.
2. Modell ablegen: `app/src/main/assets/models/efficientdet_lite2_int8.tflite`
  --> getModel.sh laedt es automatisch aus dem Web.
3. OpenRouter API-Key in `local.properties` setzen: `OPENROUTER_API_KEY=...` (nicht committen).
4. Labels liegen unter `app/src/main/assets/models/labels.txt` (COCO-80).
5. Build (PowerShell/Windows): `gradlew.bat :app:assembleDebug`
6. Installieren und starten (ADB/Android Studio).

## Bedienung
1. App starten, Kamera-Permission erlauben.
2. Start/Stop-Toggle unten rechts nutzen -> Pipeline startet/stoppt, Preview erscheint (nach Rotation auto-restart).
3. Status-Anzeige zeigt, ob RealDetector aktiv ist oder Fallback (FakeDetector).
4. Bounding-Box-Overlay zeigt erkannte Objekte; optional Labels/Confidence via Toggle (Settings).
5. OwliAI-Preview zeigt die aktuelle Ansage (Debug).
6. Overflow-Menue (TopAppBar) -> Settings/VLM Settings/Diagnostics/About oeffnen.
7. VLM ueber TopAppBar -> Szene beschreiben lassen, Follow-up fragen; Profil-Auswahl in VLM Settings.
   Auto-Toggle erscheint im VLM-Screen, wenn das Profil `auto_scan` definiert. Manuelle "Neue Szene" schaltet Auto aus.
   Diktat per Mikrofon: Tippen fuegt Text ins Eingabefeld ein; lang druecken sendet sofort. Sprachausgabe pausiert waehrend der Spracheingabe.
8. Rueck-Button schliesst Unterfenster (Settings/Diag/VLM); App-Ende erst im Hauptfenster.
9. Stop via Toggle -> Pipeline stoppt, Overlay/State wird zurueckgesetzt.

## Konfiguration
- Settings via DataStore (persistiert): Detector/Tracking/OwliAI/TTS/Debug/Pipeline-Intervall; Reset im Settings-Screen
- Sprache: System/Deutsch/English im Settings-Screen (Default: Systemsprache)
- Detector: `TfliteDetectorOptions` (Threads, NNAPI), Pfad: `models/efficientdet_lite2_int8.tflite` (aus Settings steuerbar)
- OwliAI: Konfiguration (minConfidence, minConfidenceTrack, IoU-Threshold, bboxSmoothingAlpha, minConsecutiveHits, maxDetectionsPerFrame, maxTracks, Speak-Intervalle, TTS-Speech-Rate, Decay) via Settings anpassbar
- Stabilisierung: Motion-Gating (Enable, Gyro-Schwellen, Speak-Interval-Multiplikator) via Settings anpassbar
- Hazard (Basis): DefaultSceneAnalyzer `confidenceThreshold = 0.4`, Decay 800 ms. Mapping: Person -> Personenwarnung, Fahrzeugklassen -> Fahrzeugwarnung, Ampel -> Info.
- TTS: Cooldown 2500 ms, Speech-Rate konfigurierbar (Default 2.0, via Settings); OwliAI nutzt Hash/Cooldown fuer Anti-Spam.
- Preprocessing: YUV_420_888 -> ARGB_8888 (ohne JPEG-Roundtrip), Rotation wird angewendet, optional Downscale.
- Ampel: TrafficLightPhaseClassifier (ROI-Inset, Zonenanalyse rot/oben, gruen/unten, Hysterese mit stabiler Phase).
- VLM: Profile und Prompts in `app/src/main/assets/vlm-profiles.json` (On-Demand, Raw-Debug-Mode, optional `auto_scan`).

## Developer Tools
- Lokaler Editor fuer `vlm-profiles.json`: `tools/vlm-profile-editor/` (statische HTML-Seite).
- CLI-Validator fuer VLM-Profile: `tools/validate_vlm_profiles.py`.

## Hinweise / Fehlerquellen
- Fehlt das Modell-Asset, wird automatisch der FakeDetector verwendet (Status-Anzeige).
- TTS braucht ggf. Sekunden bis READY; pendingMessage wird erst bei Ready gesprochen.
- Kurz aufblitzende Objekte (1 Frame) werden durch minConsecutiveHits + minConfidenceTrack nicht angesagt.
- Rotation: Overlay zeigt `Rot: <deg>`, Pipeline setzt Rotation pro Frame; Lifecycle-robust fuer Drehen/Stop/Start.
- Diagnostics: Report enthaelt keine persoenlichen Daten; Clipboard muss erlaubt sein.

## Lizenz / Nutzung
Interner Demo-/Prototyp-Status; keine Produktionsfreigabe, keine Gewaehrleistung. Modelle/Assets nur verwenden, wenn lizenzrechtlich geklaert.
