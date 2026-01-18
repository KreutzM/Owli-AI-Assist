# Coding-Guidelines – AI Assistenz-App

Diese Richtlinien definieren Stil, Struktur und technische Konventionen für den Code der AI Assistenz-App.
ChatGPT5.1-Codex-max soll sich bei allen Code-Generierungen so gut wie möglich daran orientieren.

---

## 1. Allgemeines

* **Sprache**: Kotlin für alle Android-Komponenten.
* **Architektur-Referenzen**:

  * Immer im Einklang mit:

    * `System-Architektur.md`
    * `System-Spezifikation.md`
* **Zielplattform**: Android, minSdk (z. B.) 26 oder höher.
* **IDE**: Android Studio.

---

## 2. Projektstruktur & Pakete

Die Paketstruktur folgt der in `System-Architektur.md` beschriebenen Struktur, z. B.:

```text
com.owlitech.owli.assist
 ├─ camera
 ├─ pipeline
 ├─ processing
 ├─ ml
 ├─ domain
 ├─ audio
 ├─ ui
 └─ util
```

**Richtlinien:**

* Keine „God Packages“ (kein Sammelpaket `misc` oder `helpers` für alles).
* Neue Pakete nur anlegen, wenn sie eine logische Ebene darstellen (z. B. `data` später für Persistenz).

---

## 3. Namenskonventionen

### 3.1 Klassen und Interfaces

* Klassen: **PascalCase** (z. B. `CameraFrameSource`, `DefaultVisionPipeline`).
* Interfaces: ebenfalls PascalCase, oft mit beschreibenden Namen (z. B. `Detector`, `SceneAnalyzer`).
* Implementierungen dürfen Präfixe wie `Default`, `Yolo`, `Tflite` haben (z. B. `DefaultSceneAnalyzer`, `YoloTfliteDetector`).

### 3.2 Funktionen

* Kleinbuchstaben und `camelCase` (z. B. `start()`, `stop()`, `preprocessImage()`).
* Funktionsnamen **beschreibend**, keine Abkürzungen wie `proc()` oder `doIt()`.

### 3.3 Properties & Variablen

* `camelCase` (z. B. `sceneStateFlow`, `frameListener`).
* Keine übermäßig kryptischen Namen; `imageProxy` statt `ip`, `detections` statt `dList`.

### 3.4 Konstanten

* `SCREAMING_SNAKE_CASE` innerhalb von `companion object` oder `object` (z. B. `MAX_HAZARD_DISTANCE`).

---

## 4. Kotlin-spezifischer Stil

* Datentransport mit `data class` (z. B. `SceneState`, `Detection`, `ModelSpec`).
* Bevorzugt **immutable** Datenstrukturen (val statt var, wo möglich).
* Extension Functions gezielt nutzen, um den Code lesbar zu halten (z. B. Konvertierungen von `ImageProxy`).
* `when`-Ausdrücke für mehrwertige Verzweigungen (z. B. verschiedenen `HazardType`).

---

## 5. Architektur- & Schichtregeln

* **UI-Schicht** (Paket `ui`):

  * Kennt nur Domänenmodelle (z. B. `SceneState`).
  * Verwendet keine ML-spezifischen Klassen (`Detector`, `ModelSpec` etc.).

* **Domain-Schicht** (Paket `domain`):

  * Enthält keine Android-spezifischen Imports (kein `Context`, keine `View`s, kein `ImageProxy`).
  * Nur Plain Kotlin.

* **ML-/Processing-Schicht** (Pakete `ml`, `processing`):

  * Bekommt möglichst wenig Android-spezifische Details.
  * `ImageProxy`-Konvertierung nach Möglichkeit in einem dedizierten Adapter.

* **Audio-Schicht** (Paket `audio`):

  * Bekommt `SceneState` als Input, nicht rohe Detections.
  * Enthält TTS/Audio-spezifische Implementierungen.

---

## 6. Concurrency & Coroutines

* Für asynchrone Aufgaben werden **Kotlin Coroutines** verwendet.
* Langlaufende oder rechenintensive Aufgaben (ML-Inferenz, Bildvorverarbeitung) laufen **nicht** auf dem Main-Thread.
* Typische Dispatcher-Zuordnung:

  * `Dispatchers.Main` für UI.
  * `Dispatchers.Default` für ML-Inferenz und Bildverarbeitung.
* In ViewModels wird `viewModelScope` verwendet.

**Regel:**

* Kein `runBlocking` im Produktionscode.
* `GlobalScope` nicht verwenden – stattdessen explizite Scopes (z. B. `viewModelScope`, selbst verwaltete `CoroutineScope` in der Pipeline).

---

## 7. Fehlerbehandlung & Logging

* Fehler (z. B. beim Laden von Modellen, Kamera-Setup) werden **explizit behandelt**.
* Exceptions werden nicht stillschweigend geschluckt; mindestens Logging.

**Empfehlung:**

* Eine einfache Logging-Utility in `util` (z. B. `AppLogger`) einführen.
* Für Prototypen kann Androids `Log` genutzt werden, später ggf. `Timber`.

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

* Unit-Tests für:

  * `SceneAnalyzer` (Domain-Heuristiken).
  * Hilfsfunktionen (z. B. Konvertierung von Koordinaten, Bounding Box-Logik).
* Tests sollen **deterministisch** sein.

**Richtlinien:**

* ML-Detektoren werden über **Fakes/Mocks** simuliert (z. B. `FakeDetector` mit vordefinierten Detections).
* Keine echten Kamera-Aufrufe in Unit-Tests.
* Testpakete spiegeln die Hauptpaketstruktur wider (z. B. `com.owlitech.owli.assist.domain` in `src/test`).

---

## 9. UI & Compose

* UI wird bevorzugt mit **Jetpack Compose** umgesetzt.
* State-Management über `StateFlow` / `collectAsState()` aus ViewModels.

**Regeln:**

* Keine Geschäftslogik in Composables.
* Composables sollten wenn möglich **stateless** sein und ihren State über Parameter erhalten.

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

* Lifecycle-awareness: Kamera/Audio/Pipeline starten/stoppen in Activity/Fragment-Lifecycle, nicht „irgendwo“.

---

## 11. Dokumentation & Kommentare

* Kurze KDoc-Kommentare für öffentliche Klassen/Interfaces/Funktionen:

```kotlin
/**
 * Führt die Analyse der Szene basierend auf den erkannten Objekten durch
 * und erstellt eine domänenspezifische SceneState-Repräsentation.
 */
interface SceneAnalyzer {
    fun analyze(detections: List<Detection>): SceneState
}
```

* Kommentare in **Deutsch**, wenn sie Use-Case-spezifisch sind.
* Komplexe Heuristiken (z. B. Hazard-Berechnung) werden kurz erklärt.

---

## 12. Abhängigkeiten & Libraries

* Möglichst wenige externe Libraries, insbesondere im ML-Pfad.
* Standard-Stack (Vorschlag):

  * AndroidX (Core, AppCompat, Lifecycle, ViewModel, Activity-Compose).
  * Jetpack Compose.
  * CameraX.
  * TFLite / LiteRT / ONNX Runtime (je nach gewähltem Backend).

Neue Libraries sollen im Code und in der Build-Datei klar begründet kommentiert werden.

---

## 13. Umgang mit Codex-Generierungen

Wenn ChatGPT5.1-Codex-max Code generiert:

* Code an `System-Architektur.md`, `System-Spezifikation.md` und diese `Coding-Guidelines.md` anpassen, falls nötig.
* Nicht-blind übernehmen; immer prüfen auf:

  * Threading (kein ML auf Main-Thread).
  * Lifecycle-Korrektheit.
  * Konsistenz der Paket- und Klassennamen.

Diese Guidelines können im Projektverlauf erweitert werden, sollten aber immer konsistent bleiben.
