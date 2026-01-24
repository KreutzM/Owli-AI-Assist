# App-Analyse OwliAI Assist (Stand: 2026-01-24)

Diese Analyse basiert auf dem aktuellen Quellstand im Repo. Es wurden keine Runtime-Messungen durchgefuehrt; die Einschaetzungen zu Performance und Risiken stuetzen sich auf Code-Review und Architekturbeobachtungen.

## Kurzfazit
- **Architektur:** Gute Paketstruktur und klare Komponentenbezeichnungen, aber starke Kopplung in `MainActivity` und `MainViewModel`, fehlende DI, sowie globale Singletons bremsen Testbarkeit und Weiterentwicklung.
- **Performance:** Mehrere heisse Pfade allokieren pro Frame zahlreiche Bitmaps/Arrays; einige Teile erzeugen vermeidbare GC-Last. Pipeline wird bei vielen Settings-Aenderungen neu gebaut.
- **Wartbarkeit:** Viele magische Parameter und mehrfach duplizierte Logik (insb. VLM-Streaming) erschweren Pflege. Ein Teil der Dokumentation ist veraltet oder inkonsistent.
- **Dokumentation:** Teilweise gut vorhanden (z. B. Paketstruktur-Doku), aber inkonsistente/fehlerhafte Encoding-Probleme (Umlaut-Mojibake) und fehlende KDocs an kritischen Heuristiken.

---

## 1) Architektur-Analyse (Probleme/Schwaechen)

### Positive Architekturmerkmale
- **Klare Paketstruktur** nach Verantwortlichkeiten (camera/processing/ml/domain/blindview/motion/audio/settings/diagnostics/vlm/ui/pipeline). Siehe u. a. `app/src/main/java/com/owlitech/owli/assist/**`.
- **Sinnvolle Abstraktionen** wie `Detector`, `Preprocessor`, `VisionPipeline` in `app/src/main/java/com/owlitech/owli/assist/ml` und `.../pipeline`.
- **Settings via DataStore** und Tests fuer zentrale Domainlogik (z. B. `app/src/test/...`).

### Schwaechen und Risiken
1) **God-Activity / God-ViewModel**
   - `MainActivity` orchestriert Kamera, Motion, Audio, Pipeline-Setup, Settings, Streaming-TTS und Navigation in einer Klasse (`app/src/main/java/com/owlitech/owli/assist/MainActivity.kt`).
   - `MainViewModel` verwaltet Pipeline-Lifecycle, VLM-Session, Attachments, Auto-Scan, Prompting, Networking und Zustandslogik in einer Datei mit hoher Komplexitaet (`app/src/main/java/com/owlitech/owli/assist/ui/MainViewModel.kt`).
   - **Risiko:** Fehleranfaelligkeit bei Aenderungen, geringe Testbarkeit, schweres Refactoring.

2) **Fehlende Dependency Injection / manuelle Objektverkettung**
   - Objekte werden direkt in `MainActivity` und `VisionPipelineModule` gebaut (z. B. `OpenRouterVlmClient`, `CameraFrameSource`, `MotionEstimator`, `DefaultVisionPipeline`).
   - **Risiko:** Enge Kopplung an Android-Kontext, schwieriges Mocking/Testing, erschwertes Austauschen einzelner Implementierungen.

3) **Globale Singletons als versteckte Abhaengigkeiten**
   - `DiagnosticsCollector` ist ein globales Objekt, von mehreren Schichten direkt beschrieben (`app/src/main/java/com/owlitech/owli/assist/diagnostics/DiagnosticsCollector.kt`).
   - **Risiko:** Seiteneffekte, schwer vorhersehbare Tests, globale Zustandsabhaengigkeit.

4) **Pipeline-Neuaufbau bei fast jeder Settings-Aenderung**
   - `MainActivity.applySettings()` erstellt bei jedem Settings-Update eine neue Pipeline (`VisionPipelineModule.create(...)`).
   - **Risiko:** Teure Re-Initialisierung des TFLite Detectors, unnoetige Ressourcenwechsel; Architektur koppelt Settings stark an Pipeline-Objekt.

5) **Netzwerk/Session-Logik im UI-ViewModel**
   - `MainViewModel` laedt und verarbeitet VLM-Requests und steuert Streaming-State in einer UI-nahen Klasse. Kein dedizierter Repository-Layer.
   - **Risiko:** Mischen von UI und Datenlogik, schwerer Austausch von Backend/Provider.

6) **Fehlende Abgrenzung Kamera-Use-Cases**
   - Pipeline nutzt CameraX ImageAnalysis (`CameraFrameSource`), VLM-Screen nutzt `ImageCapture` in Compose (`VlmScreen`).
   - **Risiko:** Ressourcen-Konflikte oder schwer nachvollziehbares Lifecycle-Management, falls beide zeitgleich aktiv sind.

7) **Fehlende Abbruch-/Cancel-Logik fuer VLM-Requests**
   - `MainViewModel.closeVlm()` beendet den UI-State, bricht aber laufende Requests nicht ab (keine Job-Referenzen).
   - **Risiko:** Spaete UI-Updates nach dem Schliessen, unnoetige Netzlast, inkonsistenter Zustand.

---

## 2) Performance-Analyse (Bottlenecks & Verbesserungen)

### Heisse Pfade und Allokationsdruck
1) **YUV->RGB pro Frame mit neuer IntArray-Allocation**
   - `YuvToRgbConverter.yuvToRgb` erzeugt fuer jedes Frame ein neues `IntArray` in Pixelgroesse (`app/src/main/java/com/owlitech/owli/assist/processing/YuvToRgbConverter.kt`).
   - **Auswirkung:** Hoher GC-Druck bei 30 FPS, moegliche Frame-Drops.
   - **Verbesserung:** Puffer wiederverwenden (Pixel-IntArray poolen) oder direkt GPU/RenderScript/Intrinsics verwenden.

2) **Mehrfaches Bitmap-Erzeugen im Preprocessor**
   - `DefaultPreprocessor` erstellt pro Frame mehrere Bitmaps (Rotation, Derotation, Crop, Resize) (`.../processing/DefaultPreprocessor.kt`).
   - **Auswirkung:** Hohe Memory-Churn, zusaetzliche Copy-Kosten.
   - **Verbesserung:** Bitmap-Pool/Reuse, optional in-place Verarbeitung oder RenderScript/BitmapShader-Ansatz.

3) **GlobalMotionEstimator erzeugt Bitmaps + Arrays pro Frame**
   - `GlobalMotionEstimator.downsampleLuma()` erzeugt `scale()` Bitmap und IntArrays (`.../processing/GlobalMotionEstimator.kt`).
   - **Auswirkung:** Zusaetzliche CPU- und GC-Last.
   - **Verbesserung:** Luma-Downsample direkt aus bestehender Bitmap oder aus YUV, Reuse von IntArrays.

4) **TrafficLightPhaseClassifier allokiert ROI/Scaled-Bitmaps ohne Recycling**
   - `innerCrop()` und `scale()` erzeugen Bitmaps; kein `recycle()` der ROI/Scaled Bitmap (`.../processing/TrafficLightPhaseClassifier.kt`).
   - **Auswirkung:** Memory-Leaks/GC-Spitzen, besonders bei hoher FPS.
   - **Verbesserung:** ROI/Scaled Bitmaps explizit freigeben oder per Pool reuse.

5) **Pipeline-Rebuild auf Settings-Events**
   - Jede Settings-Aenderung baut TFLite Detector und Pipeline neu (`MainActivity.applySettings`).
   - **Auswirkung:** Spikes bei CPU/Memory, Start-Verzoegerungen.
   - **Verbesserung:** Settings diffen und Pipeline nur neu bauen, wenn ML- oder Kamera-relevante Parameter geaendert wurden.

6) **VLM Capture via Datei-IO**
   - `VlmScreen` speichert Images zunaechst auf Disk und liest dann wieder ein (`.../ui/screens/VlmScreen.kt`).
   - **Auswirkung:** Langsame IO, besonders bei Auto-Scan.
   - **Verbesserung:** `ImageCapture.OnImageCapturedCallback` nutzen (in-memory) oder Datei-IO reduzieren.

7) **Netzwerk mit HttpURLConnection ohne Pooling**
   - `OpenRouterProvider` nutzt `HttpURLConnection` und erzeugt pro Request neue Verbindungen (`.../vlm/OpenRouterProvider.kt`).
   - **Auswirkung:** Hoehere Latenz, kein Connection Pooling, schwache Cancellation.
   - **Verbesserung:** OkHttp (Pooling, Timeout, Cancel, Interceptor), JSON-Streaming.

8) **Log-Overhead und grosse Payload-Logs**
   - VLM Payload/Response werden teils vollstaendig geloggt (`OpenRouterProvider`, `OpenRouterVlmClient`).
   - **Auswirkung:** Hoher IO- und Speicheraufwand in Debug; Risiko in Release.
   - **Verbesserung:** Log-Levels strikt auf Debug, Sampling, truncation.

### Potenzielle Hotspots (messen empfohlen)
- `DefaultVisionPipeline.onFrame` (Prozesszeit pro Frame, Pipeline-FPS).
- `DefaultPreprocessor.preprocess` und `TrafficLightPhaseClassifier.classify`.
- VLM-Streaming (SSE Parser + TTS-Queueing).

---

## 3) Wartbarkeit & Erweiterbarkeit

### Starke Punkte
- Viele Kernalgorithmen sind in eigenen Klassen isoliert (z. B. `IouObjectTracker`, `BlindViewAnnouncePlanner`, `StreamingTtsController`) und werden getestet.
- Settings sind zentral in `AppSettings` und `SettingsRepository` definiert, was zunaechst transparent wirkt.

### Schwaechen
1) **Monolithisches Modul**
   - Keine modulare Trennung (z. B. `core`, `domain`, `feature-vlm`, `feature-detector`).
   - **Folge:** Jede Aenderung betrifft den gesamten Build, schwerer zu parallelisieren, schwieriges Ownership.

2) **Viele Hardcoded Parameter**
   - Heuristik-Konstanten und Schwellwerte sind tief im Code verankert (`DefaultSceneAnalyzer`, `TrafficLightPhaseClassifier`, `GlobalMotionEstimator`).
   - **Folge:** Tuning nur durch Codeaenderung, kein klarer Config-Override.

3) **Duplizierte Logik fuer Streaming**
   - Streaming-Callback-Logik ist in `performNewSceneWithSnapshot` und `askVlm` nahezu identisch (`MainViewModel.kt`).
   - **Folge:** Fehleranfaellige Pflege, inkonsistentes Verhalten moeglich.

4) **Unklare Verantwortungsgrenzen**
   - UI-Schicht (Compose Screens) erzeugt Kamera-Captures und fuehrt IO durch (`VlmScreen.kt`).
   - **Folge:** Testbarkeit geringer, UI wird durch IO-Fehler belastet.

5) **Fehlende Abstraktionen fuer Zeit / Clock**
   - Ueberall `System.currentTimeMillis()` oder `System.nanoTime()` ohne Abstraktion (z. B. `DefaultPreprocessor`, `MotionEstimator`, `MainViewModel`).
   - **Folge:** Tests schwierig, Debug/Replay schwer.

6) **Tote/ungenutzte Pfade**
   - `useStructuredVlmParsing = false` im `MainViewModel` deutet auf inaktiven Codepfad.
   - **Folge:** Ballast, schwer wartbar.

---

## 4) In-Code-Dokumentation

### Positiv
- Viele Klassen haben kurze KDocs (z. B. `CameraFrameSource`, `DefaultVisionPipeline`, `AudioFeedbackEngine`).
- Es existiert eine umfangreiche Systemarchitektur-Doku (`docs/System-Architektur.md`).

### Defizite
1) **Encoding-Probleme (Umlaut-Mojibake)**
   - Mehrere Kommentare/Dokumente enthalten fehlerhafte Umlaute (z. B. F\\xC3\\xBCr, gegl\\xC3\\xA4ttet), u. a. in:
     - `app/src/main/java/com/owlitech/owli/assist/camera/CameraFrameSource.kt`
     - `app/src/main/java/com/owlitech/owli/assist/domain/DefaultSceneAnalyzer.kt`
     - `app/src/main/java/com/owlitech/owli/assist/blindview/IouObjectTracker.kt`
     - `docs/System-Architektur.md`
   - **Folge:** Lesbarkeit und Professionalitaet leiden, besonders fuer neue Teammitglieder.

2) **Wenig Erklaerungen fuer Heuristiken**
   - Komplexe Heuristiken (z. B. HSV-Classifier, Motion-Gating, Translation-Stabilisierung) sind kaum dokumentiert.
   - **Folge:** Erhoehte Einarbeitungszeit und Risiko bei Aenderungen.

3) **Teilweise veraltete Kommentare**
   - Z. B. `DefaultSceneAnalyzer` bezeichnet sich als Platzhalter, implementiert aber konkrete Logik.
   - **Folge:** Vertrauen in Kommentare sinkt.

4) **Fehlende Dokumentation fuer Parameter/Units**
   - Zahlreiche Parameter (ms, rad/s, normalized coords) werden nicht explizit dokumentiert.
   - **Folge:** Hoeheres Fehlerrisiko bei Anpassungen.

---

## 5) Weitere Optimierungs-Moeglichkeiten (konkret)

### Architektur & Wartbarkeit (high impact)
- **DI-Einfuehrung (Hilt/Koin oder leichter Service Locator):** Entkoppelt `MainActivity` und `MainViewModel` von konkreten Implementierungen.
- **Pipeline-Konfigurationsobjekt + diffing:** Nur Pipeline neu bauen, wenn relevante Einstellungen geaendert wurden (Detector/Preprocessing).
- **Aufteilung MainViewModel:** VLM-UseCases, Pipeline-UseCases und UI-Coordination trennen.
- **Repository-Layer fuer VLM:** Trennung von Netzwerk/Parsing und UI-Logik.

### Performance (medium-high impact)
- **Bitmap-/Array-Pooling:** Wiederverwendung in `YuvToRgbConverter`, `DefaultPreprocessor`, `GlobalMotionEstimator`.
- **TrafficLightClassifier Recycling:** ROI/Scaled Bitmaps explizit freigeben oder poolen.
- **In-Memory Capture fuer VLM:** `ImageCapture.OnImageCapturedCallback` statt Datei-IO.
- **Netzwerk-Stack verbessern:** OkHttp mit Cancellation, GZIP und Timeouts.

### Stabilitaet & UX
- **VLM-Request-Cancel:** Jobs speichern und bei `closeVlm()`/`onStop` abbrechen.
- **Saubere Logging-Policies:** Reduzierte Logs in Release, sensible Daten redigieren.

### Dokumentation
- **Encoding-Fix:** Einheitliche UTF-8 Kodierung im Repo sicherstellen.
- **Heuristik-Dokumentation:** KDoc mit Ein- und Ausgabeeinheiten, Grenzfaellen und rationale.
- **Parameter-Referenz:** Eine zentrale Tabelle fuer heuristische Schwellwerte.

---

## 6) Ergaenzungen vor Refactor-Plan (soweit moeglich gefuellt)

Diese Punkte sind soweit moeglich mit Ist-Stand aus Code und Doku gefuellt. Wo Messungen oder Entscheidungen fehlen, ist das als Luecke markiert.

### 6.1 Zielbild und Anforderungen
- **Ist-Stand (Dokumente):** `docs/System-Spezifikation.md` beschreibt die Zielsetzung (assistive echtzeitnahe Umgebungswahrnehmung), On-Device-Detektion und optionales VLM. Hinweis: kein Sicherheits- oder Medizinprodukt.
- **Nicht-Ziele (explizit):** keine sicherheitskritische Garantie, kein kontinuierlicher Cloud-Upload, keine automatischen Device-Tests im Standard-Workflow.
- **Luecke:** keine messbaren KPIs (FPS/Latenz/Qualitaet) als akzeptanzreife Zielwerte, keine formale Safety-Analyse.

### 6.2 Architektur-Abhaengigkeiten (Layering)
- **Ist-Stand (High-Level):**
  - UI (Compose Screens) -> `MainViewModel`, `SettingsViewModel`
  - `MainViewModel` -> `VisionPipelineHandle` (Pipeline + SnapshotProvider), VLM Client/Attachment Store
  - Pipeline -> `CameraFrameSource` -> CameraX (ImageAnalysis, Preview)
  - Pipeline -> `Preprocessor` -> `YuvToRgbConverter`, `GlobalMotionEstimator`
  - Pipeline -> `Detector` (`TfliteTaskDetector`/`FakeDetector`)
  - Pipeline -> `SceneAnalyzer` -> BlindView (Tracking/Planner) + `TrafficLightPhaseClassifier`
  - `MainActivity` -> `AudioFeedbackEngine`, `MotionEstimator`, `VisionPipelineModule`, `SettingsRepository`
  - `DiagnosticsCollector` wird aus mehreren Schichten beschrieben
- **Kopplungspunkte:** `DiagnosticsCollector`, `AppLogger`, `Settings` (DataStore), VLM Streaming State.
- **Luecke:** kein dokumentierter Layering-Guide (z. B. "UI darf nicht direkt X"), kein Modul-Schnitt.

#### Abhaengigkeitsgraph (ASCII)
```text
[UI: Compose Screens]
  -> [MainViewModel] -> [VisionPipelineHandle]
                        -> [VisionPipeline] -> [CameraFrameSource] -> [CameraX]
                                            -> [Preprocessor] -> [YuvToRgbConverter]
                                                              -> [GlobalMotionEstimator]
                                            -> [Detector] -> [TfliteTaskDetector | FakeDetector]
                                            -> [SceneAnalyzer] -> [BlindView Tracker/Planner]
                                                              -> [TrafficLightPhaseClassifier]
  -> [SettingsViewModel] -> [SettingsRepository] -> [DataStore]

[MainActivity]
  -> [AudioFeedbackEngine] -> [TTS]
  -> [MotionEstimator] -> [Sensors]
  -> [VisionPipelineModule] (builds pipeline)
  -> [SettingsRepository]
  -> [DiagnosticsCollector] (global)

[VLM]
  [MainViewModel] -> [OpenRouterVlmClient] -> [OpenRouterProvider] -> [HttpURLConnection]
                  -> [VlmAttachmentStore]

[Cross-cut]
  [DiagnosticsCollector] <- updates from Pipeline, MainViewModel, MainActivity
  [AppLogger] <- used by most packages
```

### 6.3 Laufzeit-Profiling (Baseline)
- **Ist-Stand:**
  - `DiagnosticsCollector.updateFrameProcessed` berechnet Frame-Interval und FPS.
  - `DefaultPreprocessor` loggt Zeit alle N Frames (`logInterval`).
  - `TrafficLightPhaseClassifier` loggt Heuristikdaten alle N Frames.
- **Luecke:** keine gespeicherten Baselines (FPS/P95/Memory/GC), keine Trace/Perfetto-Messungen im Repo.

### 6.4 Energie/Throttling
- **Ist-Stand:**
  - `analysisIntervalMs` begrenzt die Pipeline-Frequenz (Default 250ms).
  - CameraX `STRATEGY_KEEP_ONLY_LATEST` reduziert Backpressure.
  - Motion-Gating reduziert Ansagefrequenz bei hoher Bewegung.
- **Luecke:** keine Battery/thermal Messungen; kein gezieltes Throttling bei Hitze/low battery.

### 6.5 Concurrency & Lifecycle
- **Ist-Stand:**
  - `MainActivity.onStart/onStop` steuert Motion + Pipeline; `repeatOnLifecycle` fuer Flows.
  - `CameraFrameSource` nutzt Single-Thread Executor; Pipeline nutzt "latest wins" und `processing` Flag.
  - VLM Requests laufen in `viewModelScope` und sind nur durch `vlmRequestInFlight` serialisiert.
- **Risiken/Beobachtungen:**
  - `CameraFrameSource` schliesst `ImageProxy` im Analyzer, `DefaultVisionPipeline` schliesst es erneut (moeglicher Double-Close).
  - `MainViewModel.closeVlm()` beendet UI-State, bricht aber laufende VLM Jobs nicht ab.
- **Luecke:** keine dokumentierten Threading-Regeln; kein Cancel-Design fuer laufende Requests.

#### Risiko-Matrix (Lifecycle/Concurrency/Privacy)
| Area | Risk | Impact | Likelihood | Evidence | Mitigation |
| --- | --- | --- | --- | --- | --- |
| Lifecycle/Concurrency | ImageProxy double-close | Medium | Medium | CameraFrameSource closes ImageProxy; DefaultVisionPipeline closes again | Ensure single ownership of close |
| Lifecycle/Concurrency | VLM job continues after close | Medium | Medium | closeVlm resets UI but no Job cancel | Track Job and cancel on closeVlm/onStop |
| Privacy/Security | Full VLM responses logged | Medium | Medium | OpenRouterProvider logs response body | Limit logs in release, add redaction |

### 6.6 Fehlerbehandlung & Resilienz
- **Ist-Stand (Beispiele):**
  - Kamera: Permission-Handling in `MainActivity` (Log, kein explizites UI-Fallback).
  - Sensoren: `MotionEstimator.start()` loggt fehlende Sensoren, aber kein UI-Feedback.
  - TTS: Init-Fehler werden geloggt; UI bekommt kein explizites Signal.
  - VLM: fehlender API-Key -> `VlmUiState.Error`; Netzfehler -> Catch und Error-State.
- **Luecke:** keine Retry/Backoff-Strategie fuer Netzwerk; keine standardisierte User-Fehlerkommunikation.

### 6.7 Security & Privacy
- **Ist-Stand:**
  - Kamera-Pipeline ist lokal; VLM sendet Snapshot als data URL (`MainViewModel.jpegToDataUrl`).
  - `OpenRouterProvider` redigiert image_url in Logs, text wird gekuerzt; Responses werden jedoch teilweise vollstaendig geloggt.
  - API-Key wird via `local.properties` in BuildConfig injiziert.
- **Luecke:** keine dokumentierte Consent/Privacy-UX; Logging-Policy fuer Release fehlt.

### 6.8 Testabdeckung und Luecken
- **Ist-Stand (Tests vorhanden):**
  - BlindView: Tracker/Planner/Formatter
  - Audio: `StreamingTtsController`
  - Domain: `DefaultSceneAnalyzer`
  - VLM: Payload/Parser/SSE/AttachmentStore
- **Luecke:** keine Tests fuer Pipeline/Preprocessor/MotionEstimator/CameraFrameSource; keine Integrationstests fuer VLM + UI; kaum Lifecycle/Cancel-Tests.

### 6.9 Dependency-Status
- **Ist-Stand:** Versionskatalog in `gradle/libs.versions.toml` (u. a. AGP 8.13.1, Kotlin 2.0.21, Compose BOM 2024.09.00, CameraX 1.4.0, TFLite 2.16.1, Task Vision 0.4.4, Coroutines 1.9.0, DataStore 1.1.1).
- **Luecke:** kein Upgrade-Plan; keine dokumentierten Kompatibilitaetstests fuer neue Modelle oder CameraX.

### 6.10 Asset/Model-Management
- **Ist-Stand:**
  - Modellpfad erwartet: `app/src/main/assets/models/efficientdet_lite2_int8.tflite`.
  - Labels: `app/src/main/assets/models/labels.txt`.
  - `VisionPipelineModule.modelExists()` prueft Assets; Fallback auf `FakeDetector` bei Fehlschlag.
  - `docs/MODEL-ASSETS.md` beschreibt Download und Verifikation.
- **Luecke:** keine Checksums/Signaturen, keine Versionierung der Modelle, keine automatisierte Asset-Pruefung.

### 6.11 Observability
- **Ist-Stand:**
  - `DiagnosticsCollector` trackt Pipeline-Status, FPS, Motion, Stabilisierung, Translation, TTS.
  - Diagnostics-Screen zeigt Live-Metriken und Report (siehe `diagnostics/`).
  - Logging ist umfangreich (VLM Payload/Response, Pipeline Status).
- **Luecke:** keine dauerhafte Telemetrie/Export; Logging-Policy fuer Release unklar.

#### Logging-Audit (logcat)
- **Ist-Stand:** Mischung aus `AppLogger` und direkten `Log.*` Aufrufen (z. B. `AudioFeedbackEngine`, `DefaultPreprocessor`, `TrafficLightPhaseClassifier`). Tags und Formate sind inkonsistent, keine standardisierte Korrelation (Session/Frame/Request IDs).
- **Luecken fuer Performance-Refactor:**
  - Keine konsistente, strukturierte Stage-Timing Logs (Preprocess/Detect/Analyze/Overlay) pro Frame.
  - Keine Sampling- oder Rate-Limits fuer sehr haeufige Logs (Frame-Pfad).
  - Keine einheitliche Schicht-/Feature-Tags (z. B. `PIPELINE`, `PREPROCESS`, `VLM`, `AUDIO`).
  - Keine zentrale Umschaltung fuer Debug-Logging (BuildConfig oder Setting).
  - VLM Logs koennen sehr gross sein; redaction ist partiell, aber die Response wird teils vollstaendig geloggt.
  - Fehlende Marker bei zentralen Zustandswechseln (z. B. Pipeline-Config-Change, Settings-Diff, Pipeline-Rebuild).

### 6.12 Accessibility & UX Audit
- **Ist-Stand:**
  - VLM Screen nutzt Semantics (contentDescription, stateDescription) und Snackbar-Feedback.
  - TTS ist primare Ausgabe; Voice-Input ueber `RecognizerIntent` vorhanden.
- **Luecke:** keine systematische Accessibility-Pruefung fuer alle Screens; kein globaler Flow fuer Permission-Fehler.

### 6.13 Release- und Rollback-Strategie
- **Ist-Stand:**
  - Build Types: Debug/Release; Feature-Toggles via DataStore (z. B. Detector an/aus, Debug-View).
- **Luecke:** kein Feature-Flag-System fuer stufenweise Rollouts; kein dokumentiertes Rollback.

### Minimal-Deliverables fuer Refactor-Start (Status)
1. Baseline-Metriken (FPS, Latenz, GC, VLM) - fehlen.
2. Abhaengigkeitsgraph (High-level, 1 Seite) - initial vorhanden (ASCII), keine Grafik.
3. Risiko-Matrix (Lifecycle/Concurrency/Privacy) - initial vorhanden (qualitativ).
4. Test-Lueckenliste mit Prioritaet - teilweise hier, aber ohne Priorisierung.
5. Zielkriterien (messbar, 3-5 KPIs) - fehlen.

---
## Priorisierte Empfehlung (Kurzliste)

1. **Pipeline-Rebuild nur bei relevanten Settings** (MainActivity/Settings diffing) - grosser Performancegewinn mit geringem Risiko.
2. **Bitmap/Array-Pooling in Preprocessing/GlobalMotionEstimator** - reduziert GC und Frame-Drops.
3. **VLM-Request Abbruch / Job-Management** - verhindert inkonsistente UI-States.
4. **TrafficLightClassifier Bitmap-Recycling** - verhindert Memory-Spikes.
5. **DI oder Service-Layer fuer VLM/Pipeline** - verbessert Testbarkeit und Erweiterbarkeit.
6. **Encoding-Probleme beheben** - Lesbarkeit und Professionalitaet.

---

## Offene Fragen / Annahmen
- Es wurde nicht gemessen, ob die Pipeline (inkl. Detector) real-time stabil bei Ziel-Hardware laeuft.
- Es ist unklar, ob es bewusst gewuenscht ist, dass Settings-Aenderungen die Pipeline komplett neu initialisieren.
- VLM-Use-Case Parallelitaet (gleichzeitig Pipeline + VLM + AutoScan) ist nur anhand des Codes beurteilt.

---

## Fazit
Die App hat eine solide Basis und klare Feature-Segmente, leidet jedoch an starker Kopplung in zentralen Klassen, unnoetigen Ressourcen-Neuinitialisierungen und Performance-Kosten im Bildverarbeitungspfad. Durch gezielte Entkopplung, Puffer-Reuse und strengere Lifecycle/Job-Steuerung laesst sich die Stabilitaet und Erweiterbarkeit deutlich verbessern.
