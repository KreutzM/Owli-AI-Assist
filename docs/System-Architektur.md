# System-Architektur â€“ Fahrrad-Assistenz-App (CV fÃ¼r sehbehinderte Radfahrer)

Diese Datei beschreibt die Ziel-Architektur der Android-App, damit ChatGPT5.1-Codex-max (Ã¼ber Codex-CLI) konsistenten, erweiterbaren Code erzeugen kann.

Bitte bei allen Code-Generierungen diese Architektur, Paketstruktur und Begriffe berÃ¼cksichtigen.

---

## 1. Ziel und Rahmen

Die App ist ein **Fahrrad-Assistenzsystem** fÃ¼r sehbehinderte Radfahrer:

* Das Smartphone ist am Lenker montiert, die Kamera zeigt nach vorn.
* Pro Zeiteinheit (z.B. 1â€“5 FPS) werden Kamerabilder analysiert.
* Die App erkennt:

  * FuÃŸgÃ¤nger und andere Fahrzeuge (FahrrÃ¤der, Autos, Busse â€¦) im Fahrweg
  * Hindernisse im Fahrweg (z.B. Ã„ste, Steine, Objekte)
  * Ampeln und ggf. deren Status (rot/grÃ¼n)
* Der Nutzer erhÃ¤lt **akustische** (und optional haptische) Warnungen.

Wichtige Ziele:

* **ModularitÃ¤t / Testbarkeit**: CV-Pipeline klar getrennt von UI und Audio.
* **Austauschbare Modelle**: Verschiedene YOLO-/CV-Modelle mÃ¼ssen einfach austauschbar sein.
* **On-Device-Inferenz**: Kein Cloud-Zwang, TFLite/LiteRT/ONNX/MediaPipe sind mÃ¶gliche Backends.
* **Prototyp-freundlich**: Einfach neue Heuristiken und Modelle testen.

---

## 2. Paketstruktur

Basis-Paket (anpassbar, hier als Beispiel):

```text
com.owlitech.owli.assist
 â”œâ”€ camera       // Kamera-Integration und Frame-Lieferung
 â”œâ”€ pipeline     // VisionPipeline: Orchestrierung von Kamera, ML, Analyzer
 â”œâ”€ processing   // Vorverarbeitung von Frames fÃ¼r Modelle
 â”œâ”€ ml           // ML-Modelle, Detector-Schnittstellen, Backends
 â”œâ”€ domain       // DomÃ¤nenlogik, Szeneninterpretation, Hazard-Modelle
 â”œâ”€ audio        // Audio- und TTS-Feedback
 â”œâ”€ ui           // Activities, Compose UI, ViewModels
 â””â”€ util         // Gemeinsame Hilfsklassen (Logger, Dispatchers etc.)
```

Bitte bei Code-Generierung diese Struktur verwenden (oder konsistent erweitern).

---

## 3. Datenfluss (High-Level)

1. **Kamera** (CameraX) liefert kontinuierlich Frames (`ImageProxy`).
2. **VisionPipeline** nimmt Frames entgegen, begrenzt die Framerate (z.B. 1â€“5 FPS) und verarbeitet Frames sequentiell.
3. **Preprocessor** konvertiert `ImageProxy` in Modell-Input (`FloatArray` oder `ByteBuffer`).
4. **Detector** fÃ¼hrt das ML-Modell aus und gibt eine Liste von `Detection`-Objekten zurÃ¼ck.
5. **SceneAnalyzer** interpretiert `Detection`-Listen und erzeugt eine domÃ¤nenspezifische `SceneState`-ReprÃ¤sentation (z.B. Hazard-Level, Warntext, OwliAI-Items).
6. **UI** beobachtet `SceneState` und zeigt Debug-Infos (Overlays) an.
7. **AudioFeedbackEngine** beobachtet `SceneState` und erzeugt gesprochene Warnungen/TÃ¶ne.

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

* Verantwortlich fÃ¼r Einrichtung von CameraX (Preview + ImageAnalysis).
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
  * Framerate drosseln (z.B. `sample` / eigene Logik, ca. 1â€“5 FPS).
  * Frames sequentiell auf einem geeigneten Dispatcher verarbeiten.
  * `SceneState` in `sceneStates`-Flow emittieren.
  * Backpressure-Strategie: Bei Ãœberlast **Ã¤ltere Frames verwerfen**, neueste priorisieren.

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
interface Preprocessor {
    fun preprocess(image: ImageProxy): FloatArray // oder ByteBuffer, je nach Backend
}
```

Es soll mindestens eine Implementierung geben:

* `DefaultPreprocessor`:

  * YUV-zu-RGB-Konvertierung
  * Resize/Crop auf gewÃ¼nschte ModellgrÃ¶ÃŸe (z.B. 640x640)
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

* LÃ¤dt ein YOLO-kompatibles TFLite-Modell aus Assets/Dateisystem anhand von `ModelSpec`.
* ErhÃ¤lt `FloatArray` oder `ByteBuffer` als Input.
* FÃ¼hrt TFLite-Inferenz durch.
* Dekodiert Output in `Detection`-Liste.
* Wendet NMS mit `modelSpec.nmsThreshold` an.

Weitere konkrete Implementierungen (spÃ¤ter):

* `YoloOnnxDetector`
* `MediaPipeObjectDetector`

---

## 8. DomÃ¤nen-Schicht (`domain`)

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
    val detections: List<Detection>,   // optional fÃ¼r Debug/Overlay
    val hazards: List<HazardEvent>,
    val primaryMessage: String?,       // z.B. "Achtung, Person voraus!"
    val overallHazardLevel: HazardLevel
)
```

### 8.2 `SceneAnalyzer`

```kotlin
interface SceneAnalyzer {
    fun analyze(detections: List<Detection>): SceneState
}
```

* Implementierung enthÃ¤lt Heuristiken:

  * Auswahl relevanter Objekte im Fahrweg (z.B. mittleres Drittel, unteres Drittel des Bildes)
  * SchÃ¤tzung Richtung (links/rechts/zentral) anhand BBox
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

* Intern: `TextToSpeech`-Instanz oder Ã¤hnliche TTS-Engine.
* Implementiert eine Cooldown-/Debounce-Logik, damit nicht stÃ¤ndig gesprochen wird.
* Spricht nur relevante Ã„nderungen, z.B. Wechsel in `HazardLevel` oder neue, kritische `HazardEvent`s.

Audio-Schicht sollte **nicht** direkt von ML oder Kamera abhÃ¤ngen, sondern nur von `SceneState`.

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

* Verantwortlich fÃ¼r:

  * Setup von CameraX (via `CameraFrameSource`).
  * Erzeugen der `VisionPipeline`-Instanz (oder via Dependency Injection erhalten), basierend auf aktuellen Settings.
  * Instanziieren von `AudioFeedbackEngine` und Registrierung als Beobachter von `sceneState`.
  * Compose-UI:

    * Kamerapreview (z.B. `AndroidView` mit `PreviewView`).
    * Overlay-Layer fÃ¼r Bounding Boxes (Debug).
    * Textanzeige fÃ¼r `primaryMessage` / Status.
    * Settings-Screen fÃ¼r Detector/OwliAI/TTS/Debug/Pipeline-Parameter (DataStore-basiert, Reset mÃ¶glich).

Lifecycle-Regeln:

* `onResume()` â†’ Pipeline & Kamera starten.
* `onPause()` â†’ Pipeline & Kamera stoppen (oder Auto-Restart-Flag merken).
* `onDestroy()` â†’ TTS/Audio und ggf. Detector/Pipeline sauber schlieÃŸen.

---

## 11. Concurrency & Performance

Wichtige Regeln fÃ¼r Implementierungen:

1. **Kein ML im UI-Thread**:

   * Alle Inferenz- und Vorverarbeitungsschritte laufen auf `Dispatchers.Default` oder dedizierten `ExecutorService`.

2. **Frame-Drosselung & Backpressure**:

   * Kamera liefert ggf. 30 FPS, Pipeline verarbeitet nur 1â€“5 FPS.
   * Bei Ãœberlast alte Frames verwerfen ("latest wins").

3. **Ressourcen-Lifecycle**:

   * `Detector`-Instanzen werden einmalig initialisiert und wiederverwendet.
   * `Detector.close()` im richtigen Lifecycle-Hook aufrufen.
   * `AudioFeedbackEngine.close()` beim ZerstÃ¶ren der UI.

---

## 12. ExtensibilitÃ¤t

Die Architektur soll es Codex ermÃ¶glichen, neue Features relativ einfach einzufÃ¼gen:

* **Weitere Modelle**:

  * Neue `Detector`-Implementierungen (z.B. fÃ¼r Depth/Segmentation) kÃ¶nnen parallel existieren.
  * Kombination mehrerer Detektoren durch z.B. `CompositeDetector` oder durch Parallel-Pipelines.

* **Weitere Frontends**:

  * DomÃ¤nenschicht (`SceneState`, `SceneAnalyzer`) bleibt unverÃ¤ndert, neue UIs (Wear OS, Head-Up-Display) konsumieren dieselben Daten.

* **Experimentelle Features (z.B. CLIP/OpenCLIP)**:

  * Eigene `Detector`-Varianten oder zusÃ¤tzliche Analyzerschichten kÃ¶nnen hinzugefÃ¼gt werden, ohne UI/Audio zu brechen.

---

## 13. Coding-Guidelines (fÃ¼r Codex)

* Sprache: **Kotlin** fÃ¼r alle Android-Komponenten.
* UI: bevorzugt **Jetpack Compose**, aber klassische Views sind fÃ¼r Kamera-Preview erlaubt.
* Folgende Prinzipien beachten:

  * Single Responsibility fÃ¼r Klassen.
  * Klare Trennung von Android-spezifischem Code (UI, CameraX, TTS) und core logic (SceneAnalyzer, Detector-Interfaces).
  * Keine harten AbhÃ¤ngigkeiten von UI auf ML-Details: UI konsumiert nur `SceneState`.
  * Bei Generierung von Beispielcode bitte sinnvolle Default-Werte, Kommentare in Deutsch und Threading-Hinweise ergÃ¤nzen.

Diese Architektur dient als Zielbild. Codex soll alle Implementierungen mÃ¶glichst eng an dieses Design anlehnen.

---

## 14. OwliAI-Hinweise

* OwliAI nutzt einen IoU-basierten Lightweight-Tracker pro Label (EMA-BBox/Confidence, Max-Age, minConsecutiveHits), um Position/Uhrzeit pro Objekt zu stabilisieren.
* Announce-Planer aggregiert Objekte nach Label/Uhrzeit/Distanz, sortiert NEAR->MID->FAR und links->rechts; Speech-Planer/Cooldown verhindert Spam.
* TTS-Sprechrate ist konfigurierbar (z.B. 2.0) und unabhaengig von den Speak-Intervallen/Cooldowns.

## 15. Settings, Debug & Diagnostics (aktuell)

* Persistente Settings per DataStore (Detector/Tracking/OwliAI/TTS/Debug/Pipeline-Intervall) mit Reset-to-Defaults.
* UI-Toggles: Overlay, Overlay-Labels (BBox + Confidence), OwliAI-Preview.
* Diagnostics-Screen zeigt Pipeline/Detector/Tracking/TTS-Status (FPS, Intervall, Thresholds, AutoStart) und kann einen Debug-Report ins Clipboard kopieren.
