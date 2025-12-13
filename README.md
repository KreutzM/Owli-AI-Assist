# BikeBuddy / BikeAssist

BikeBuddy ist eine Android-Demo-App für ein Fahrrad-Assistenzsystem mit On-Device-Computer-Vision. Sie nutzt CameraX für die Live-Preview, eine Vision-Pipeline (Preprocessing → Detector → SceneAnalyzer) und gibt Warnungen per TTS aus. Als Beispielmodell ist EfficientDet-Lite2 (COCO) eingebunden; bei fehlendem Modell fällt die App auf einen FakeDetector zurück, damit die Demo immer lauffähig bleibt.

## Funktionsumfang
- Live-Kamera-Preview (CameraX)
- Objekt-Erkennung (EfficientDet-Lite2, COCO) mit Bounding-Box-Overlay
- Hazard-Auswertung: Personen/Fahrzeuge → Warnung; Ampeln → Info (derzeit ohne Status)
- TTS-Ausgabe mit Cooldown und Status-Anzeige (RealDetector/Fallback)
- Start/Stop der Pipeline; Decay-Logik für Hazards

## Architektur (kurz)
- `camera`: CameraFrameSource (CameraX Preview + ImageAnalysis)
- `processing`: Preprocessor (ImageProxy → Bitmap)
- `ml`: Detector-Interface; TfliteTaskDetector (EfficientDet-Lite2) + FakeDetector
- `domain`: DefaultSceneAnalyzer (Label-basiert, Hazard-Mapping, Decay)
- `pipeline`: DefaultVisionPipeline (Frame→Bitmap→Detect→Analyze), VisionPipelineModule (DI + Real/Fake Auswahl)
- `audio`: AudioFeedbackEngine (TTS, Cooldown, pendingMessage)
- `ui`: MainActivity (Compose: PreviewView + Overlay + ControlPanel), MainViewModel (Flows, Start/Stop/Status)

## Voraussetzungen
- Android Studio (AGP 8.x), Kotlin 2.0.x, Compose aktiviert
- Gerät/Emulator mit Kamera (Emulator: Virtual Camera oder Webcam)
- Android 12 (getestet auf S10+) oder höher empfohlen

## Installation & Build
1. Repository öffnen.
2. Modell ablegen: `app/src/main/assets/models/efficientdet_lite2_int8.tflite`
3. Labels liegen unter `app/src/main/assets/models/labels.txt` (COCO).
4. Build: `./gradlew :app:assembleDebug`
5. Installieren und starten (ADB/Android Studio).

## Bedienung
1. App starten, Kamera-Permission erlauben.
2. Start-Button drücken → Pipeline startet, Preview erscheint.
3. Status-Anzeige zeigt, ob RealDetector aktiv ist oder Fallback (FakeDetector).
4. Bounding-Box-Overlay zeigt erkannte Objekte; Hazard-Text/Level oben links.
5. Stop-Button → Pipeline stoppt, Overlay/State wird zurückgesetzt.

## Konfiguration
- Detector-Optionen: `TfliteDetectorOptions` (Threads, NNAPI), Pfad: `models/efficientdet_lite2_int8.tflite`
- Hazard-Schwelle: DefaultSceneAnalyzer nutzt `confidenceThreshold = 0.4`, Decay 800 ms. Mapping: Person → Personenwarnung, Fahrzeugklassen → Fahrzeugwarnung, Ampel → Info.
- TTS-Cooldown: 2500 ms, spricht bei Message-Wechsel oder Hazard-Level-Anstieg; Reset, wenn keine Meldung vorliegt.
- Preprocessing: YUV_420_888 → ARGB_8888 ohne JPEG-Roundtrip, Rotation wird angewendet (`rotationDegrees`), optional Downscale.

## Fehlerquellen / Hinweise
- Fehlt das Modell-Asset, wird automatisch der FakeDetector verwendet (Status-Anzeige).
- TTS braucht ggf. Sekunden bis READY; pendingMessage wird erst bei Ready gesprochen.
- Auf schwachen Geräten kann der JPEG-Roundtrip im Preprocessor bremsen; YUV→RGB-Optimierung empfehlenswert.
- Traffic-Light wird aktuell nur als Info ohne Level/Sprachmeldung behandelt.
- Bei Rotation: Overlay zeigt `Rot: <deg>`, Pipeline setzt Rotation pro Frame; Preview sollte nach Drehen zurückkehren (Lifecycle-robust).

## Lizenz / Nutzung
Interner Demo-/Prototyp-Status; keine Produktionsfreigabe, keine Gewährleistung. Modelle/Assets nur verwenden, wenn lizenzrechtlich geklärt.
