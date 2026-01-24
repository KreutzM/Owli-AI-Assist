# Refaktor-Plan OwliAI Assist (priorisiert nach Performance + Wartbarkeit)

Ziel: Schrittweise, risikoarme Verbesserungen mit messbarem Nutzen. Jeder Schritt ist klein, testbar und buildbar.
Hinweis: Abhaengigkeiten/Drittlibs nur nach ausdruecklicher Freigabe aendern.

---

## Phase 0: Vorbereitung (Messbarkeit und Leitplanken)

### 0.1 Baseline-Metriken erfassen (Pflicht vor Performance-Refactors)
**Ziel:** Messbare Vorher/Nachher-Werte fuer FPS, Latenz, GC, VLM.
**Was tun:**
- In Diagnostics-Screen die folgenden Werte pro Session notieren: FPS (avg/P95), Frame-Interval, Preprocess-Zeit, VLM-Latenz (Capture -> Antwort -> TTS Start).
- Einmal 5/15/30 Minuten Laufzeit protokollieren (CPU/Memory grob per Android Studio Profiler).
**Dateien/Ort:** Mess-Notizen in `docs/App-Analyse.md` ergaenzen.

### 0.2 KPI-Ziele definieren
**Ziel:** Akzeptanzkriterien fuer Refactor.
**Was tun:**
- 3-5 KPIs definieren (z. B. FPS >= X, P95 Preprocess <= Y ms, VLM First-TTS <= Z s, GC Pausen < N ms/Minute).
- In diesem Dokument unter "KPI Ziele" dokumentieren.

### 0.3 Energie/Thermal Baseline
**Ziel:** Vermeidung von Regressionen in Akku/Temperatur.
**Was tun:**
- 15/30 Minuten Dauerbetrieb messen (Battery drain, Thermal throttling, CPU/GPU Load).
- Ergebnisse kurz in `docs/App-Analyse.md` dokumentieren.

### 0.4 Logging/Tracing-Plan (Refactor-Enablement)
**Ziel:** Logcat soll Refactor-Messungen unterstuetzen, ohne Performance zu belasten.
**Was tun:**
- Definieren einer Log-Struktur (Tags, Level, Felder) fuer Pipeline/VLM/Audio.
- Festlegen von Sampling/Rate-Limits fuer Frame-Logs.
- Definieren von Korrelation (Session-ID, Frame-ID, VLM-Request-ID).
- Liste der Pflicht-Marker (Pipeline start/stop, Settings-Diff, Pipeline-Rebuild).

---

## Phase 1: Hoher Nutzen, geringes Risiko (Performance + Lifecycle)

### 1.1 Pipeline nur bei relevanten Settings neu bauen (Settings-Diff)
**Nutzen:** grosser Performancegewinn, weniger Re-Init von TFLite, stabilere UX.
**Was tun:**
- In `MainActivity.applySettings()` Settings diffen.
- Pipeline nur neu bauen, wenn detector/preprocess-relevante Parameter geaendert wurden (z. B. analysisIntervalMs, model options, motion gating, stabilization/translation params).
- Audio/Locale/Diagnostics weiterhin ohne Rebuild anwenden.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/MainActivity.kt`, evtl. neue Datenklasse `PipelineConfig`.
**Tests:** `:app:testDebugUnitTest`.

### 1.2 ImageProxy-Ownership fixen (Double-Close verhindern)
**Nutzen:** Stabilitaet, weniger Crash/Undefined Behavior.
**Was tun:**
- Ownership fuer `ImageProxy.close()` eindeutig festlegen (entweder in `CameraFrameSource` oder `DefaultVisionPipeline`, nicht beides).
- Pruefen, ob Analyzer und Pipeline beide schliessen; nur einer darf es.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/camera/CameraFrameSource.kt`, `app/src/main/java/com/owlitech/owli/assist/pipeline/DefaultVisionPipeline.kt`.
**Tests:** `:app:testDebugUnitTest`.

### 1.3 VLM-Requests sauber abbrechen (Job-Management)
**Nutzen:** stabile UI, weniger Netzlast, bessere Lifecycle-Sicherheit.
**Was tun:**
- VLM Requests in `MainViewModel` als `Job?` speichern.
- `closeVlm()` und `onStop`/`stopForLifecycle()` cancelt laufende Jobs.
- Streaming Callback soll nach Cancel keine UI-Updates mehr ausfuehren.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/ui/MainViewModel.kt`.
**Tests:** neue Unit-Tests fuer Cancel (falls moeglich mit Fake Dispatcher), sonst `:app:testDebugUnitTest`.

### 1.4 UI-IO trennen (VLM Capture)
**Nutzen:** weniger IO im UI, bessere Reaktionszeit.
**Was tun:**
- `VlmScreen` soll keine Datei-IO machen; stattdessen `ImageCapture.OnImageCapturedCallback` nutzen oder IO in ViewModel/UseCase verschieben.
- Bei Umstellung: Rueckgabe weiterhin ByteArray.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/ui/screens/VlmScreen.kt`.
**Tests:** manuell pruefen (Capture + Add Attachment), `:app:testDebugUnitTest` optional.

---

## Phase 2: Performance-Optimierungen im Bildpfad

### 2.1 Pixel-Buffer Reuse in YuvToRgbConverter
**Nutzen:** weniger GC, stabilere FPS.
**Was tun:**
- `IntArray` fuer Pixel als Feld halten und bei gleicher Groesse wiederverwenden.
- Nur neu allokieren, wenn Aufloesung wechselt.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/processing/YuvToRgbConverter.kt`.
**Tests:** `:app:testDebugUnitTest`.

### 2.2 Bitmap-Pooling fuer Preprocess-Intermediate
**Nutzen:** weniger Bitmap-Churn, weniger GC.
**Was tun:**
- Einfuehren eines kleinen Bitmap-Pools fuer Rotate/Crop/Resize.
- Pool groessenbewusst (z. B. nach width/height/config map).
- Sicherstellen, dass ausgehende Bitmaps nicht recycelt werden.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/processing/DefaultPreprocessor.kt` plus neue Pool-Klasse.
**Tests:** `:app:testDebugUnitTest`.

### 2.3 GlobalMotionEstimator Downsample-Array Reuse
**Nutzen:** weniger Allokationen pro Frame.
**Was tun:**
- `IntArray` fuer lowRes und luma als Felder halten.
- `Bitmap.scale` vermeiden, wenn moeglich; alternativ Luma direkt aus vorhandener Bitmap ziehen.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/processing/GlobalMotionEstimator.kt`.
**Tests:** `:app:testDebugUnitTest`.

### 2.4 TrafficLightPhaseClassifier Bitmap Recycling
**Nutzen:** reduziert Memory-Spikes.
**Was tun:**
- `innerCrop()` und `scale()` erzeugte Bitmaps nach Gebrauch recyceln oder aus Pool.
- Sicherstellen, dass keine shared Bitmaps recycelt werden.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/processing/TrafficLightPhaseClassifier.kt`.
**Tests:** `:app:testDebugUnitTest`.

---

## Phase 3: Architektur-Entkopplung (Wartbarkeit/Erweiterbarkeit)

### 3.1 Pipeline-Konfigurationsobjekt einfuehren
**Nutzen:** klare Trennung zwischen Settings und Pipeline, einfacher Diff.
**Was tun:**
- Neue Datenklasse `PipelineConfig` (nur pipeline-relevante Felder).
- `MainActivity` erzeugt `PipelineConfig` aus `AppSettings`.
- `VisionPipelineModule.create()` nimmt `PipelineConfig` statt vieler Parameter.
**Dateien:** `MainActivity.kt`, `VisionPipelineModule.kt`, neue `PipelineConfig`.
**Tests:** `:app:testDebugUnitTest`.

### 3.2 VLM UseCase/Repository Layer
**Nutzen:** Trennung von UI und Netzwerk/Parsing.
**Was tun:**
- `VlmRepository` kapselt Client-Aufrufe, Session-Management, Streaming-Callbacks.
- `MainViewModel` delegiert VLM Logik an UseCase/Repository.
**Dateien:** `app/src/main/java/com/owlitech/owli/assist/vlm/*`, `MainViewModel.kt`.
**Tests:** bestehende VLM-Tests erweitern, neue Unit-Tests fuer Repository.

### 3.3 Service Locator oder DI Light
**Nutzen:** Testbarkeit, klarere Konstruktion.
**Was tun:**
- Leichten Service Locator im App-Start (keine neue Dependency) einfuehren.
- `MainActivity` holt Instanzen aus Locator statt selbst zu bauen.
**Dateien:** neue z. B. `assist/di/AppServices.kt`, `MainActivity.kt`.
**Tests:** `:app:testDebugUnitTest`.

---

## Phase 4: Stabilitaet, Security, Observability

### 4.1 Logging-Policy einziehen
**Nutzen:** Privacy und Performance in Release.
**Was tun:**
- Debug-Only Logging fuer Payload/Response (BuildConfig.DEBUG pruefen).
- Sensible Daten strikt redigieren; max length begrenzen.
- Direktes `Log.*` durch `AppLogger` ersetzen (einheitliche Tags/Level).
- Sampling/Rate-Limits fuer Frame-Logs (z. B. jede N Frames oder P95-Only).
- Strukturierte Marker fuer Stage-Timings (preprocess/detect/analyze) mit Frame-ID.
- Korrelation einfuehren: Session-ID, Frame-ID, VLM-Request-ID.
**Dateien:** `OpenRouterProvider.kt`, `OpenRouterVlmClient.kt`, ggf. `AppLogger`.
**Tests:** `:app:testDebugUnitTest`.

### 4.2 Standardisierte Error-UX
**Nutzen:** konsistente Nutzerfuehrung.
**Was tun:**
- Einheitliches Error-Model (z. B. sealed class) fuer Kamera/Sensor/TTS/VLM.
- UI zeigt klare Handlung (z. B. "Permission erteilen", "TTS aktivieren").
**Dateien:** `MainViewModel.kt`, UI Screens.
**Tests:** UI-Tests optional, `:app:testDebugUnitTest`.

### 4.3 Privacy/Consent-Flow fuer VLM (wenn erforderlich)
**Nutzen:** Compliance und Nutzervertrauen.
**Was tun:**
- Einmaliges Opt-in, das erklaert, dass Bilder an OpenRouter gesendet werden.
- Speicherung in Settings; VLM-Start ohne Consent blocken.
**Dateien:** `SettingsRepository.kt`, `AppSettings`, VLM Screens.
**Tests:** `:app:testDebugUnitTest`.

### 4.4 Observability-Export
**Nutzen:** bessere Diagnose.
**Was tun:**
- Diagnostics Report um kurze Performance-Sektion erweitern (FPS avg/P95, VLM-Latenz).
- Optionaler Export als Datei/Share (falls gewuenscht).
**Dateien:** `diagnostics/DiagnosticsReportBuilder.kt`, `DiagnosticsScreen.kt`.
**Tests:** `:app:testDebugUnitTest`.

---

## Phase 5: Tests und Dokumentation

### 5.1 Test-Luecken schliessen (priorisiert)
**Nutzen:** Sicherheit vor Regressionen.
**Was tun (Reihenfolge):**
1) Pipeline-Lifecycle Test (start/stop, snapshot provider behavior).
2) VLM Cancel Test (Jobs, Streaming callback stop).
3) Settings-Diff Test (Pipeline rebuild only when relevant).
4) MotionEstimator/Preprocessor deterministische Tests (Fake Clock, Fake Bitmap).
**Dateien:** `app/src/test/...`.
**Tests:** `:app:testDebugUnitTest`.

### 5.2 Dokumentation aktualisieren
**Nutzen:** Zukunftssicherheit.
**Was tun:**
- `docs/System-Architektur.md` aktualisieren (PipelineConfig, Repository Layer, Ownership Regeln).
- `docs/TESTING.md` um neue Tests erweitern.
- Encoding-Probleme in Doku/Kommentare beheben (UTF-8 konsistent).

---

## Phase 6: Governance & Release

### 6.1 Dependency-Risikoanalyse
**Nutzen:** kontrollierte Upgrades.
**Was tun:**
- Abhaengigkeiten mit hoher Auswirkung identifizieren (CameraX, TFLite, Compose).
- Upgrade-Pfade dokumentieren (keine Updates ohne Freigabe).
**Dateien:** `docs/App-Analyse.md` erweitern.

### 6.2 Asset-Integrity Check
**Nutzen:** robuste Modell-Integritaet.
**Was tun:**
- Optional Checksums fuer Modell/Labels dokumentieren und beim Start pruefen.
- Abweichungen im Diagnostics Report anzeigen.
**Dateien:** `processing/` oder `ml/`, `diagnostics/`.

### 6.3 Accessibility Audit (gezielt)
**Nutzen:** bessere Nutzbarkeit.
**Was tun:**
- Screen-by-screen Checklist (Semantics, Focus order, Error copy).
- VLM/Settings/Diagnostics als Startpunkte.
**Dateien:** `docs/App-Analyse.md` oder `docs/TESTING.md` als Checklist.

### 6.4 Rollback/Feature-Flags
**Nutzen:** risikoarme Releases.
**Was tun:**
- Feature-Flags fuer neue Pipeline oder VLM Flow (Settings-basiert).
- Rollback-Doku: Wie alte Pipeline wieder aktivieren.
**Dateien:** `SettingsRepository.kt`, `AppSettings`, `docs/DEVELOPMENT.md`.

---

## KPI Ziele (vor Refactor festlegen)
- FPS (avg/P95): ________
- Preprocess-Zeit (P95): ________
- VLM First-TTS Latenz: ________
- GC Pause pro Minute: ________
- Memory Peak (MB): ________

---

## Hinweise zur Umsetzung
- Jede Aenderung in kleinen Schritten mit `:app:testDebugUnitTest` verifizieren.
- Bei UI/Resources/Manifest/Gradle Aenderungen zusaetzlich `:app:assembleDebug`.
- Keine neuen Dependencies ohne Freigabe.

---

## Commit-fertige Tasks (klein, fokussiert)
1. **Mess-Baseline notieren**: FPS/Latenz/GC/VLM in `docs/App-Analyse.md` ergaenzen. (Docs only)
2. **KPI-Ziele eintragen**: Zielwerte in diesem Dokument setzen. (Docs only)
3. **Settings-Diff Einfuehrung**: Pipeline nur bei relevanten Settings neu bauen.
4. **ImageProxy Ownership fixen**: Single-Owner fuer `close()`.
5. **VLM Cancel**: Job-Management und Abbruch bei `closeVlm()`/`onStop`.
6. **VLM Capture IO aus UI**: Umstellung auf In-Memory Capture oder ViewModel/UseCase.
7. **YuvToRgb Pixel-Buffer Reuse**: IntArray Pooling.
8. **Preprocess Bitmap Pool**: Pool fuer Rotate/Crop/Resize.
9. **GlobalMotionEstimator Array Reuse**: Luma/lowRes Buffer pooling.
10. **TrafficLight Bitmap Recycling**: ROI/Scaled Bitmap Lebenszyklus klarmachen.
11. **Logging-Policy Schritt 1**: direkte `Log.*` zu `AppLogger` + Debug-Only Logs.
12. **Logging-Policy Schritt 2**: Sampling/Rate-Limits + strukturierte Stage-Timings + IDs.
13. **PipelineConfig Datenklasse**: Konfigobjekt + Anpassung Module/Activity.
14. **VLM Repository Layer**: VLM Logik aus ViewModel extrahieren.
15. **Service Locator Light**: Instanzen zentral bereitstellen.
16. **Error-Model**: Einheitliche Fehler fuer Kamera/Sensor/TTS/VLM.
17. **Privacy/Consent**: Opt-in Flow fuer VLM (falls erforderlich).
18. **Observability Export**: Diagnostics Report erweitern.
19. **Test-Luecken #1**: Pipeline-Lifecycle Test.
20. **Test-Luecken #2**: VLM Cancel Test.
21. **Test-Luecken #3**: Settings-Diff Test.
22. **Test-Luecken #4**: Preprocessor/MotionEstimator Tests.
23. **Dependency-Risikoanalyse**: Dokumentation der Upgrade-Pfade.
24. **Asset-Integrity Check**: Modell/Label Checksums.
25. **Accessibility Audit**: Checklist + Fixes.
26. **Rollback/Feature-Flags**: Flags fuer neue Pipeline/Flows.
