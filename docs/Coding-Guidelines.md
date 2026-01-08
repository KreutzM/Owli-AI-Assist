# Coding-Guidelines â€“ Fahrrad-Assistenz-App

Diese Richtlinien definieren Stil, Struktur und technische Konventionen fÃ¼r den Code der Fahrrad-Assistenz-App.
ChatGPT5.1-Codex-max soll sich bei allen Code-Generierungen so gut wie mÃ¶glich daran orientieren.

---

## 1. Allgemeines

* **Sprache**: Kotlin fÃ¼r alle Android-Komponenten.
* **Architektur-Referenzen**:

  * Immer im Einklang mit:

    * `System-Architektur.md`
    * `System-Spezifikation.md`
* **Zielplattform**: Android, minSdk (z. B.) 26 oder hÃ¶her.
* **IDE**: Android Studio.

---

## 2. Projektstruktur & Pakete

Die Paketstruktur folgt der in `System-Architektur.md` beschriebenen Struktur, z. B.:

```text
com.owlitech.owli.assist
 â”œâ”€ camera
 â”œâ”€ pipeline
 â”œâ”€ processing
 â”œâ”€ ml
 â”œâ”€ domain
 â”œâ”€ audio
 â”œâ”€ ui
 â””â”€ util
```

**Richtlinien:**

* Keine â€žGod Packagesâ€œ (kein Sammelpaket `misc` oder `helpers` fÃ¼r alles).
* Neue Pakete nur anlegen, wenn sie eine logische Ebene darstellen (z. B. `data` spÃ¤ter fÃ¼r Persistenz).

---

## 3. Namenskonventionen

### 3.1 Klassen und Interfaces

* Klassen: **PascalCase** (z. B. `CameraFrameSource`, `DefaultVisionPipeline`).
* Interfaces: ebenfalls PascalCase, oft mit beschreibenden Namen (z. B. `Detector`, `SceneAnalyzer`).
* Implementierungen dÃ¼rfen PrÃ¤fixe wie `Default`, `Yolo`, `Tflite` haben (z. B. `DefaultSceneAnalyzer`, `YoloTfliteDetector`).

### 3.2 Funktionen

* Kleinbuchstaben und `camelCase` (z. B. `start()`, `stop()`, `preprocessImage()`).
* Funktionsnamen **beschreibend**, keine AbkÃ¼rzungen wie `proc()` oder `doIt()`.

### 3.3 Properties & Variablen

* `camelCase` (z. B. `sceneStateFlow`, `frameListener`).
* Keine Ã¼bermÃ¤ÃŸig kryptischen Namen; `imageProxy` statt `ip`, `detections` statt `dList`.

### 3.4 Konstanten

* `SCREAMING_SNAKE_CASE` innerhalb von `companion object` oder `object` (z. B. `MAX_HAZARD_DISTANCE`).

---

## 4. Kotlin-spezifischer Stil

* Datentransport mit `data class` (z. B. `SceneState`, `Detection`, `ModelSpec`).
* Bevorzugt **immutable** Datenstrukturen (val statt var, wo mÃ¶glich).
* Extension Functions gezielt nutzen, um den Code lesbar zu halten (z. B. Konvertierungen von `ImageProxy`).
* `when`-AusdrÃ¼cke fÃ¼r mehrwertige Verzweigungen (z. B. verschiedenen `HazardType`).

---

## 5. Architektur- & Schichtregeln

* **UI-Schicht** (Paket `ui`):

  * Kennt nur DomÃ¤nenmodelle (z. B. `SceneState`).
  * Verwendet keine ML-spezifischen Klassen (`Detector`, `ModelSpec` etc.).

* **Domain-Schicht** (Paket `domain`):

  * EnthÃ¤lt keine Android-spezifischen Imports (kein `Context`, keine `View`s, kein `ImageProxy`).
  * Nur Plain Kotlin.

* **ML-/Processing-Schicht** (Pakete `ml`, `processing`):

  * Bekommt mÃ¶glichst wenig Android-spezifische Details.
  * `ImageProxy`-Konvertierung nach MÃ¶glichkeit in einem dedizierten Adapter.

* **Audio-Schicht** (Paket `audio`):

  * Bekommt `SceneState` als Input, nicht rohe Detections.
  * EnthÃ¤lt TTS/Audio-spezifische Implementierungen.

---

## 6. Concurrency & Coroutines

* FÃ¼r asynchrone Aufgaben werden **Kotlin Coroutines** verwendet.
* Langlaufende oder rechenintensive Aufgaben (ML-Inferenz, Bildvorverarbeitung) laufen **nicht** auf dem Main-Thread.
* Typische Dispatcher-Zuordnung:

  * `Dispatchers.Main` fÃ¼r UI.
  * `Dispatchers.Default` fÃ¼r ML-Inferenz und Bildverarbeitung.
* In ViewModels wird `viewModelScope` verwendet.

**Regel:**

* Kein `runBlocking` im Produktionscode.
* `GlobalScope` nicht verwenden â€“ stattdessen explizite Scopes (z. B. `viewModelScope`, selbst verwaltete `CoroutineScope` in der Pipeline).

---

## 7. Fehlerbehandlung & Logging

* Fehler (z. B. beim Laden von Modellen, Kamera-Setup) werden **explizit behandelt**.
* Exceptions werden nicht stillschweigend geschluckt; mindestens Logging.

**Empfehlung:**

* Eine einfache Logging-Utility in `util` (z. B. `AppLogger`) einfÃ¼hren.
* FÃ¼r Prototypen kann Androids `Log` genutzt werden, spÃ¤ter ggf. `Timber`.

Beispiel:

```kotlin
try {
    detector.warmup()
} catch (e: Exception) {
    AppLogger.e(e, "Failed to warm up detector")
}
```

---

## 8. Tests

* Unit-Tests fÃ¼r:

  * `SceneAnalyzer` (Domain-Heuristiken).
  * Hilfsfunktionen (z. B. Konvertierung von Koordinaten, Bounding Box-Logik).
* Tests sollen **deterministisch** sein.

**Richtlinien:**

* ML-Detektoren werden Ã¼ber **Fakes/Mocks** simuliert (z. B. `FakeDetector` mit vordefinierten Detections).
* Keine echten Kamera-Aufrufe in Unit-Tests.
* Testpakete spiegeln die Hauptpaketstruktur wider (z. B. `com.owlitech.owli.assist.domain` in `src/test`).

---

## 9. UI & Compose

* UI wird bevorzugt mit **Jetpack Compose** umgesetzt.
* State-Management Ã¼ber `StateFlow` / `collectAsState()` aus ViewModels.

**Regeln:**

* Keine GeschÃ¤ftslogik in Composables.
* Composables sollten wenn mÃ¶glich **stateless** sein und ihren State Ã¼ber Parameter erhalten.

Beispiel:

```kotlin
@Composable
fun SceneOverlay(
    sceneState: SceneState?,
    modifier: Modifier = Modifier
) {
    // zeichne BBoxes etc.
}
```

---

## 10. Ressourcen & Lifecycle

* Ressourcenintensive Objekte (z. B. `Detector`, `TextToSpeech`) werden sauber verwaltet und geschlossen:

  * `Detector.close()` in geeigneten Lifecycle-Hooks.
  * `TextToSpeech.shutdown()` in `onDestroy()`.

* Kein Halten von `Context` in langlebigen Singletons, sofern nicht notwendig.

* Lifecycle-awareness: Kamera/Audio/Pipeline starten/stoppen in Activity/Fragment-Lifecycle, nicht â€žirgendwoâ€œ.

---

## 11. Dokumentation & Kommentare

* Kurze KDoc-Kommentare fÃ¼r Ã¶ffentliche Klassen/Interfaces/Funktionen:

```kotlin
/**
 * FÃ¼hrt die Analyse der Szene basierend auf den erkannten Objekten durch
 * und erstellt eine domÃ¤nenspezifische SceneState-ReprÃ¤sentation.
 */
interface SceneAnalyzer {
    fun analyze(detections: List<Detection>): SceneState
}
```

* Kommentare in **Deutsch**, wenn sie Use-Case-spezifisch sind.
* Komplexe Heuristiken (z. B. Hazard-Berechnung) werden kurz erklÃ¤rt.

---

## 12. AbhÃ¤ngigkeiten & Libraries

* MÃ¶glichst wenige externe Libraries, insbesondere im ML-Pfad.
* Standard-Stack (Vorschlag):

  * AndroidX (Core, AppCompat, Lifecycle, ViewModel, Activity-Compose).
  * Jetpack Compose.
  * CameraX.
  * TFLite / LiteRT / ONNX Runtime (je nach gewÃ¤hltem Backend).

Neue Libraries sollen im Code und in der Build-Datei klar begrÃ¼ndet kommentiert werden.

---

## 13. Umgang mit Codex-Generierungen

Wenn ChatGPT5.1-Codex-max Code generiert:

* Code an `System-Architektur.md`, `System-Spezifikation.md` und diese `Coding-Guidelines.md` anpassen, falls nÃ¶tig.
* Nicht-blind Ã¼bernehmen; immer prÃ¼fen auf:

  * Threading (kein ML auf Main-Thread).
  * Lifecycle-Korrektheit.
  * Konsistenz der Paket- und Klassennamen.

Diese Guidelines kÃ¶nnen im Projektverlauf erweitert werden, sollten aber immer konsistent bleiben.
