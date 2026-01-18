# System-Architektur - AI Assistenz-App (CV fuer blinde Nutzer)

Diese Datei beschreibt die Ziel-Architektur der Android-App, damit ChatGPT5.1-Codex-max (über Codex-CLI) konsistenten, erweiterbaren Code erzeugen kann.

Bitte bei allen Code-Generierungen diese Architektur, Paketstruktur und Begriffe berücksichtigen.

---

## 1. Ziel und Rahmen

Die App ist eine **AI Assistenz-App** für blinde Nutzer:

* Das Smartphone ist am Koerper oder in der Hand, die Kamera zeigt nach vorn.
* Pro Zeiteinheit (z.B. 1–5 FPS) werden Kamerabilder analysiert.
* Die App erkennt:

  * Fußgänger und andere Fahrzeuge (Motorraeder, Autos, Busse …) im Sichtfeld
  * Hindernisse im Sichtfeld (z.B. Äste, Steine, Objekte)
  * Ampeln und ggf. deren Status (rot/grün)
* Der Nutzer erhält **akustische** (und optional haptische) Warnungen.

Wichtige Ziele:

* **Modularität / Testbarkeit**: CV-Pipeline klar getrennt von UI und Audio.
* **Austauschbare Modelle**: Verschiedene YOLO-/CV-Modelle müssen einfach austauschbar sein.
* **On-Device-Inferenz**: Kein Cloud-Zwang, TFLite/LiteRT/ONNX/MediaPipe sind mögliche Backends.
* **Prototyp-freundlich**: Einfach neue Heuristiken und Modelle testen.

---

## 2. Paketstruktur

Basis-Paket (anpassbar, hier als Beispiel):

```text
com.owlitech.owli.assist
 ├─ camera       // Kamera-Integration und Frame-Lieferung
 ├─ pipeline     // VisionPipeline: Orchestrierung von Kamera, ML, Analyzer
 ├─ processing   // Vorverarbeitung von Frames für Modelle
 ├─ ml           // ML-Modelle, Detector-Schnittstellen, Backends
 ├─ domain       // Domänenlogik, Szeneninterpretation, Hazard-Modelle
 ├─ motion       // IMU/Motion-Estimator (Gyro/Rotation Vector)
 ├─ audio        // Audio- und TTS-Feedback
 ├─ ui           // Activities, Compose UI, ViewModels
 └─ util         // Gemeinsame Hilfsklassen (Logger, Dispatchers etc.)
```

Bitte bei Code-Generierung diese Struktur verwenden (oder konsistent erweitern).

---

## 3. Datenfluss (High-Level)

1. **Kamera** (CameraX) liefert kontinuierlich Frames (`ImageProxy`).
2. **IMU** liefert Motion-Snapshots (Gyro/Rotation Vector) zur Stabilisierung.
3. **VisionPipeline** nimmt Frames entgegen, begrenzt die Framerate (z.B. 1-5 FPS) und verarbeitet Frames sequentiell.
4. **Preprocessor** konvertiert `ImageProxy` in Modell-Input (`FloatArray` oder `ByteBuffer`).
5. **Detector** fuehrt das ML-Modell aus und gibt eine Liste von `Detection`-Objekten zurueck.
6. **SceneAnalyzer** interpretiert `Detection`-Listen und erzeugt eine domaenenspezifische `SceneState`-Repraesentation (z.B. Hazard-Level, Warntext, OwliAI-Items) unter Einbezug von Motion-Snapshots.
7. **UI** beobachtet `SceneState` und zeigt Debug-Infos (Overlays) an.
8. **AudioFeedbackEngine** beobachtet `SceneState` und erzeugt gesprochene Warnungen/Toene.

Alle ML- und Bildverarbeitungsaufgaben laufen **nicht** im UI-Thread, sondern auf Worker-Threads (Coroutines/Dispatchers).

---

## 4. Kamera-Schicht (`camera`)

### 4.1 `FrameListener`

```kotlin
interface FrameListener {
    fun onFrame(image: ImageProxy)
}
```

### 4.2 `CameraFrameSource`

* Verantwortlich für Einrichtung von CameraX (Preview + ImageAnalysis).
* Besitzt Methoden:

```kotlin
class CameraFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    var frameListener: FrameListener? = null

    fun start(cameraSelector: CameraSelector)
    fun stop()
}
```

* `start()` bindet CameraX an den Lifecycle und liefert Frames an `frameListener` (ImageAnalysis).
* *Wichtig*: Kein direktes ML im `onFrame` der CameraX-Callback-Funktion.

---

## 5. Vision-Pipeline-Schicht (`pipeline`)

Die Pipeline kapselt den kompletten Weg von Kamera-Frame bis `SceneState`.

### 5.1 API

```kotlin
interface VisionPipeline {
    val sceneStates: Flow<SceneState>
    fun start()
    fun stop()
}
```

### 5.2 Beispiel-Implementierung: `DefaultVisionPipeline`

* Konstruktor-Parameter:

  * `cameraSource: CameraFrameSource`
  * `preprocessor: Preprocessor`
  * `detector: Detector`
  * `analyzer: SceneAnalyzer`
  * `scope: CoroutineScope` (z.B. ViewModelScope oder AppScope)

* Aufgaben:

  * `CameraFrameSource` als `FrameListener` registrieren.
  * Frames in einen `SharedFlow<ImageProxy>` pushen.
  * Framerate drosseln (z.B. `sample` / eigene Logik, ca. 1–5 FPS).
  * Frames sequentiell auf einem geeigneten Dispatcher verarbeiten.
  * `SceneState` in `sceneStates`-Flow emittieren.
  * Backpressure-Strategie: Bei Überlast **ältere Frames verwerfen**, neueste priorisieren.

Kein direkter Bezug auf UI-Elemente oder Android-spezifische Views in der Pipeline.

---

## 6. Vorverarbeitung (`processing`)

### 6.1 `ModelInputSpec`

```kotlin
data class ModelInputSpec(
    val width: Int,
    val height: Int,
    val channels: Int = 3,
    val normalizeMean: FloatArray,
    val normalizeStd: FloatArray
)
```

### 6.2 `Preprocessor`

```kotlin
data class PreprocessResult(
    val bitmap448: Bitmap,
    val mapping: FrameMapping?
)

interface Preprocessor {
    fun preprocess(image: ImageProxy, motion: MotionSnapshot? = null): PreprocessResult
}
```

Es soll mindestens eine Implementierung geben:

* `DefaultPreprocessor`:

  * YUV-zu-RGB-Konvertierung
  * Roll-Lock per IMU (optional) nach Rotation
  * Stabilisiertes Crop-Window (geglättetes Center + Translation-Schaetzung), danach Center-Crop auf Square, Resize auf 448x448
  * FrameMapping fuer Overlay (448x448 -> Preview)
  * Normalisierung (z.B. Wertebereich [0,1] oder ImageNet-Mean/Std)

Ziel: Vorverarbeitung ist **modellkonfigurierbar** (auf Basis von `ModelInputSpec`).

---

## 7. ML-Schicht (`ml`)

### 7.1 Datenklassen

```kotlin
data class Detection(
    val label: String,
    val confidence: Float,
    val bbox: BoundingBox
)

data class BoundingBox(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
    // Normalisierte Koordinaten 0..1 im Bildkoordinatensystem
)
```

### 7.2 Modellbeschreibung

```kotlin
data class ModelSpec(
    val modelPath: String,             // z.B. "models/yolo_v8n.tflite"
    val inputSpec: ModelInputSpec,
    val labels: List<String>,
    val outputShape: IntArray,
    val scoreThreshold: Float = 0.3f,
    val nmsThreshold: Float = 0.5f
)
```

### 7.3 Backend-Typen

```kotlin
enum class Backend {
    TFLITE,
    ONNX,
    MEDIAPIPE
}


data class DetectorConfig(
    val modelSpec: ModelSpec,
    val backend: Backend
)
```

### 7.4 `Detector`-Interface

```kotlin
interface Detector : Closeable {
    fun warmup()
    fun detect(input: FloatArray): List<Detection>
}
```

* `warmup()` optional, um das Modell einmalig zu laden/initialisieren.
* `detect()` wird von der `VisionPipeline` auf Worker-Threads aufgerufen.

### 7.5 `DetectorFactory`

```kotlin
interface DetectorFactory {
    fun create(config: DetectorConfig): Detector
}
```

### 7.6 Beispiel-Implementierung: `YoloTfliteDetector`

* Lädt ein YOLO-kompatibles TFLite-Modell aus Assets/Dateisystem anhand von `ModelSpec`.
* Erhält `FloatArray` oder `ByteBuffer` als Input.
* Führt TFLite-Inferenz durch.
* Dekodiert Output in `Detection`-Liste.
* Wendet NMS mit `modelSpec.nmsThreshold` an.

Weitere konkrete Implementierungen (später):

* `YoloOnnxDetector`
* `MediaPipeObjectDetector`

---

## 8. Domänen-Schicht (`domain`)

Diese Schicht kennt keine Android-spezifischen Klassen und keine ML-Backends.

### 8.1 Hazard-Level und Szenenstatus

```kotlin
enum class HazardLevel { NONE, WARNING, DANGER }


data class HazardEvent(
    val type: HazardType,
    val direction: Direction?,
    val urgency: HazardLevel
)

enum class HazardType {
    PERSON_AHEAD,
    VEHICLE_AHEAD,
    OBSTACLE_AHEAD,
    TRAFFIC_LIGHT_RED,
    TRAFFIC_LIGHT_GREEN,
    UNKNOWN
}

enum class Direction { LEFT, RIGHT, CENTER }



data class SceneState(
    val timestamp: Long,
    val detections: List<Detection>,   // optional für Debug/Overlay
    val hazards: List<HazardEvent>,
    val primaryMessage: String?,       // z.B. "Achtung, Person voraus!"
    val overallHazardLevel: HazardLevel
)
```

### 8.2 `SceneAnalyzer`

```kotlin
interface SceneAnalyzer {
    fun analyze(
        detections: List<Detection>,
        trafficLights: List<TrafficLightObservation> = emptyList(),
        motion: MotionSnapshot? = null
    ): SceneState
}
```

* Implementierung enthält Heuristiken:

  * Auswahl relevanter Objekte im Sichtfeld (z.B. mittleres Drittel, unteres Drittel des Bildes)
  * Schätzung Richtung (links/rechts/zentral) anhand BBox
  * Zusammenfassung zu `HazardEvent`-Liste und `primaryMessage`.

Diese Schicht ist gut unit-testbar (Pure Kotlin, keine Android-Klassen).

---

## 9. Audio-Schicht (`audio`)

### 9.1 `AudioFeedbackEngine`

```kotlin
class AudioFeedbackEngine(
    private val context: Context
) : Closeable {
    fun onSceneUpdated(state: SceneState)
    override fun close()
}
```

* Intern: `TextToSpeech`-Instanz oder ähnliche TTS-Engine.
* Implementiert eine Cooldown-/Debounce-Logik, damit nicht ständig gesprochen wird.
* Spricht nur relevante Änderungen, z.B. Wechsel in `HazardLevel` oder neue, kritische `HazardEvent`s.

Audio-Schicht sollte **nicht** direkt von ML oder Kamera abhängen, sondern nur von `SceneState`.

---

## 10. UI-Schicht (`ui` + Settings)

Die UI-Schicht ist **Konsument** von `SceneState` und orchestriert Start/Stop der Pipeline.

### 10.1 `MainViewModel`

```kotlin
class MainViewModel(
    private val visionPipeline: VisionPipeline
) : ViewModel() {

    val sceneState: StateFlow<SceneState?>

    fun start()
    fun stop()
}
```

* `start()` & `stop()` delegieren an `visionPipeline.start/stop`.
* Der ViewModel beobachtet `visionPipeline.sceneStates` und mapped sie auf `sceneState`.

### 10.2 `MainActivity`

* Verantwortlich für:

  * Setup von CameraX (via `CameraFrameSource`).
  * Erzeugen der `VisionPipeline`-Instanz (oder via Dependency Injection erhalten), basierend auf aktuellen Settings.
  * Instanziieren von `AudioFeedbackEngine` und Registrierung als Beobachter von `sceneState`.
  * Compose-UI:

    * Kamerapreview (z.B. `AndroidView` mit `PreviewView`).
    * Overlay-Layer für Bounding Boxes (Debug).
    * Textanzeige für `primaryMessage` / Status.
    * Settings-Screen für Detector/OwliAI/TTS/Debug/Pipeline-Parameter (DataStore-basiert, Reset möglich).

Lifecycle-Regeln:

* `onResume()` → Pipeline & Kamera starten.
* `onPause()` → Pipeline & Kamera stoppen (oder Auto-Restart-Flag merken).
* `onDestroy()` → TTS/Audio und ggf. Detector/Pipeline sauber schließen.

---

## 11. Concurrency & Performance

Wichtige Regeln für Implementierungen:

1. **Kein ML im UI-Thread**:

   * Alle Inferenz- und Vorverarbeitungsschritte laufen auf `Dispatchers.Default` oder dedizierten `ExecutorService`.

2. **Frame-Drosselung & Backpressure**:

   * Kamera liefert ggf. 30 FPS, Pipeline verarbeitet nur 1–5 FPS.
   * Bei Überlast alte Frames verwerfen ("latest wins").

3. **Ressourcen-Lifecycle**:

   * `Detector`-Instanzen werden einmalig initialisiert und wiederverwendet.
   * `Detector.close()` im richtigen Lifecycle-Hook aufrufen.
   * `AudioFeedbackEngine.close()` beim Zerstören der UI.

---

## 12. Extensibilität

Die Architektur soll es Codex ermöglichen, neue Features relativ einfach einzufügen:

* **Weitere Modelle**:

  * Neue `Detector`-Implementierungen (z.B. für Depth/Segmentation) können parallel existieren.
  * Kombination mehrerer Detektoren durch z.B. `CompositeDetector` oder durch Parallel-Pipelines.

* **Weitere Frontends**:

  * Domänenschicht (`SceneState`, `SceneAnalyzer`) bleibt unverändert, neue UIs (Wear OS, Head-Up-Display) konsumieren dieselben Daten.

* **Experimentelle Features (z.B. CLIP/OpenCLIP)**:

  * Eigene `Detector`-Varianten oder zusätzliche Analyzerschichten können hinzugefügt werden, ohne UI/Audio zu brechen.

---

## 13. Coding-Guidelines (für Codex)

* Sprache: **Kotlin** für alle Android-Komponenten.
* UI: bevorzugt **Jetpack Compose**, aber klassische Views sind für Kamera-Preview erlaubt.
* Folgende Prinzipien beachten:

  * Single Responsibility für Klassen.
  * Klare Trennung von Android-spezifischem Code (UI, CameraX, TTS) und core logic (SceneAnalyzer, Detector-Interfaces).
  * Keine harten Abhängigkeiten von UI auf ML-Details: UI konsumiert nur `SceneState`.
  * Bei Generierung von Beispielcode bitte sinnvolle Default-Werte, Kommentare in Deutsch und Threading-Hinweise ergänzen.

Diese Architektur dient als Zielbild. Codex soll alle Implementierungen möglichst eng an dieses Design anlehnen.

---

## 14. OwliAI-Hinweise

* OwliAI nutzt einen IoU-basierten Lightweight-Tracker pro Label (EMA-BBox/Confidence, Max-Age, minConsecutiveHits), um Position/Uhrzeit pro Objekt zu stabilisieren.
* Announce-Planer aggregiert Objekte nach Label/Uhrzeit/Distanz, sortiert NEAR->MID->FAR und links->rechts; Speech-Planer/Cooldown verhindert Spam.
* TTS-Sprechrate ist konfigurierbar (z.B. 2.0) und unabhaengig von den Speak-Intervallen/Cooldowns.

## 15. Settings, Debug & Diagnostics (aktuell)

* Persistente Settings per DataStore (Detector/Tracking/OwliAI/TTS/Debug/Pipeline-Intervall) mit Reset-to-Defaults.
* UI-Toggles: Overlay, Overlay-Labels (BBox + Confidence), OwliAI-Preview.
* Diagnostics-Screen zeigt Pipeline/Detector/Tracking/TTS-Status (FPS, Intervall, Thresholds, AutoStart) und kann einen Debug-Report ins Clipboard kopieren.
