# System-Spezifikation – Owli-AI Assist (Soll + Ist-Markierungen)

Diese Spezifikation beschreibt **funktionale und nicht-funktionale Anforderungen** der App.
Sie ist so geschrieben, dass ein Team aus **2 Menschen + 1 Codex-Agent** schnell entscheiden kann:
- Was ist **bereits implementiert (Ist)**?
- Was ist **geplant (Soll)**?

Legende:
- ✅ Ist: im Code vorhanden
- 🟡 Teilweise: rudimentär/heuristisch/experimentell
- ⏳ Soll: nicht umgesetzt / später

---

## 1. Zielsetzung

Die App unterstützt blinde Nutzer:innen durch **assistive, echtzeitnahe Umgebungswahrnehmung**:
- Live-Kamera wird analysiert (On-Device)
- relevante Objekte/Gefahren werden **akustisch** gemeldet
- optional: On-Demand „Szenenbeschreibung“ über ein VLM (OpenRouter)

Hinweis: Die App ist **kein Sicherheits- oder Medizinprodukt**.

---

## 2. Kernfunktionen

### 2.1 Live-Kamera & Overlay
- ✅ Live-Kamera-Preview (CameraX)
- ✅ Bounding-Box-Overlay (mit korrektem FrameMapping)
- ✅ Toggle: Labels/Confidence anzeigen
- ✅ Start/Stop der Pipeline

### 2.2 Objekterkennung
- ✅ On-Device Detector-Abstraktion (`ml/Detector`)
- ✅ TFLite Task Vision Integration (EfficientDet-Lite2 vorgesehen)
- ✅ Fallback `FakeDetector`, wenn Modell fehlt/Init scheitert
- ✅ konfigurierbare Detector-Parameter (Threads/NNAPI/Threshold/MaxResults)

### 2.3 Szeneninterpretation & Tracking
- ✅ IoU-Tracking + Smoothing/EMA + Consecutive Hits (BlindView)
- ✅ Clock-Position („auf 2 Uhr“) + Label-Übersetzung (DE)
- 🟡 einfache Distanz-/Near-Mapping (heuristisch)
- 🟡 einfache Hazard-Logik (Personen/Fahrzeuge/Ampeln; Decay)

### 2.4 Ampel-Erkennung
- ✅ Erkennung „Ampel vorhanden“ aus Detections
- 🟡 Phase rot/grün per HSV-Heuristik (gelb optional/später)
- ✅ Stabilisierung/Decay der Phase im State

### 2.5 Audio-Feedback (TTS)
- ✅ Android TTS Ausgabe
- ✅ Cooldown/Spam-Schutz
- ✅ konfigurierbare SpeechRate/Pitch
- ✅ StreamingTTS Controller (u. a. für VLM Streaming)

### 2.6 Motion / Stabilisierung
- ✅ MotionEstimator (Gyro + Rotation Vector) → MotionSnapshot
- 🟡 Motion-Gating: Tracking/Ansage-Parameter abhängig vom Motion-Level
- 🟡 IMU Roll-Derotation (experimentell, quality-gated)
- 🟡 Translation-Stabilisierung des Crop-Windows (Patch-Matching; quality-gated)
- ✅ Debug-Metriken für Stabilisierung (dx/dy/quality/crop)

### 2.7 VLM (OpenRouter) – On-Demand
- ✅ Profilbasiertes Setup (`vlm-profiles.json`)
- ✅ On-Demand Anfrage: Bild + Prompt → Antwort (Raw-Debug)
- 🟡 Autoscan (optional, abhängig vom Profil)
- ⏳ Produktiv-UI für „konversationelle Assistenz“ (Roadmap, wenn gewünscht)

### 2.8 Settings & Diagnostics
- ✅ DataStore Settings (Toggles + Parameter) inkl. Reset
- ✅ Diagnostics Screen: Live-Metriken + Copy-to-Clipboard Report

---

## 3. Nicht-funktionale Anforderungen

### 3.1 Performance
- ✅ Frame-Analyse mit Intervall/Throttling (Konfiguration)
- ✅ „latest wins“ (CameraX Analysis) zur Reduktion von Backpressure
- 🟡 Zielwerte (z. B. 1–5 FPS Analyse) abhängig von Geräten (nicht strikt enforced)

### 3.2 Robustheit
- ✅ Fallback, wenn Modell fehlt
- ✅ Defensive Error-Handling in Pipeline
- ✅ Decay/Timeouts, um „hängende“ States zu vermeiden

### 3.3 Datenschutz & Security
- ✅ On-Device CV standardmäßig; VLM nur On-Demand
- ✅ API Key via `local.properties` (nicht committen)
- ✅ Keine Secrets in Logs/Doku

### 3.4 Testbarkeit & Qualität
- ✅ JVM Unit-Tests für zentrale Logik (Tracker/Planner/Analyzer/TTS)
- ✅ Statische Checks via Lint (`:app:lintDebug`) als Qualitätsgate (Workflow in `AGENTS.md`)
- ⏳ CI Pipeline (optional, empfohlen)

---

## 4. Explizite Nicht-Ziele (aktuell)
- Kein sicherheitskritisches Warnsystem mit Garantien
- Kein kontinuierlicher Cloud-Upload
- Keine automatischen Device-Tests im Standard-Workflow

---

## 5. Dokumente, die dazu gehören
- `docs/System-Architektur.md` (Ist-Architektur, Paketstruktur, Datenfluss)
- `docs/DEVELOPMENT.md` (Team-Workflow, Setup, Fast/Full Checks)
- `docs/TESTING.md` (Test-Policy, Patterns, deterministische Tests)
- `docs/MODEL-ASSETS.md` (Modellbeschaffung & Windows/PowerShell Hinweise)
