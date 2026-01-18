# System-Spezifikation – AI Assistenz-App (AI/CV fuer blinde Nutzer)

Diese Spezifikation beschreibt **umfangreiche funktionale und nicht-funktionale Anforderungen** der geplanten Android-App. Sie dient ChatGPT5.1-Codex-max als Referenz für alle Implementierungen.

---

## 1. Zielsetzung

Die App soll blinden Nutzern eine **assistive, echtzeitfaehige Risikoerkennung** bieten, indem die Smartphone- oder Zusatzkamera die Umgebung analysiert und relevante Gefahren akustisch meldet.

Die App ist **kein Sicherheits- oder Medizinprodukt**, sondern ein technischer Assistent.

---

## 2. Kernfunktionen

### 2.1 Objekterkennung im Sichtfeld

* Erkennen von **Fußgängern**.
* Erkennen von **Fahrzeugen** (Autos, Motorraeder, Busse, Lieferwagen).
* Erkennen von **statischen Hindernissen** (z. B. Pfosten, Poller, Steine, Äste, Baustellenobjekte).
* Optional später: Erkennen von **Verkehrszeichen**.

### 2.2 Ampel- und Verkehrsstatus-Erkennung

* Erkennung, ob im Bild eine **Ampel** sichtbar ist.
* Bestimmung des Ampelstatus: **rot / grün** (gelb optional).
* Priorisierte Warnung, wenn der Fahrer auf eine rote Ampel zufährt.

### 2.3 Szeneninterpretation

* Ableitung einer vereinfachten **Risikobewertung**:

  * Richtung: links / rechts / zentral.
  * Gefahr: NONE / WARNING / DANGER.
* Berechnung eines **primären Hinweises** (Text), der über TTS ausgesprochen wird.

### 2.4 Audio-Ausgabe

* Text-to-Speech (TTS)-basierte Ausgabe:

  * "Achtung, Person voraus!"
  * "Rote Ampel!"
  * "Hindernis rechts!"
* Cooldown-Mechanismus, damit nicht zu oft gesprochen wird.
* Optional später: Richtungsbezogene akustische Hinweise (links/rechts-Panorama, Töne statt Sprache).

### 2.5 UI

* Live-Kamera-Preview.
* Debug-Overlay:

  * Bounding Boxes
  * Labels und Konfidenzen
  * Szenenstatus
* Anzeige der letzten Warnung.
* Einfacher Start/Stop-Schalter für Analyse.

---

## 3. Nutzergruppen

### 3.1 Primäre Nutzer

* **Blinde Nutzer**, die zusaetzliche Orientierungshilfe benoetigen.

### 3.2 Sekundäre Nutzer

* Entwickler und Forscher im Bereich Computer Vision, die neue Modelle testen wollen.

---

## 4. Systemgrenzen

### 4.1 Eingaben

* Kamera-Frames der Smartphone-Kamera oder einer extern per USB angeschlossenen Kamera.

### 4.2 Ausgaben

* Akustische Warnungen.
* Overlays (nur visuell, optional für Debugging).

### 4.3 Nicht-Ziele

* Navigation oder Routing.
* Verkehrsvorhersagen.
* Vollständige Verkehrsregelinterpretation.
* Rückseitige Gefahrenerkennung (kann später als Erweiterung folgen).

---

## 5. Funktionale Anforderungen

### 5.1 Analysefrequenz

* Die App soll **1–5 FPS** verarbeiten (konfigurierbar).
* Die Kamera kann mit höheren FPS laufen, aber nicht jeder Frame wird verarbeitet.

### 5.2 Erkennung

* Mindestkonfidenz für Objekte: **0.30** (modellabhängig).
* NMS (Non-Maximum-Suppression): **0.50** als Standardwert.
* Normalisierte Bounding-Box-Koordinaten 0..1.

### 5.3 Hazard-Logik

* Ein Hindernis im **unteren Drittel** des Bildes (Sichtfeld) gilt als potenziell gefährlich.
* Objekte im mittleren Drittel liefern Warnungen.
* Ampelstatus überschreibt andere Warnungen, wenn eine rote Ampel im Sichtfeld ist.

### 5.4 Audio-Ausgabe

* Mindestabstand zwischen Sprachmeldungen: z. B. **3 Sekunden**.
* Beim Wechsel von WARNING → DANGER sofortige Meldung.

### 5.5 Benutzerinteraktion

* Start/Stop über Schalter.
* Anzeigen von:

  * Kamera-Feed
  * Detections
  * Szenenstatus

---

## 6. Nicht-funktionale Anforderungen

### 6.1 Performance

* Inferenzzeit pro Frame: < **300 ms** auf einem mittleren Smartphone.
* Keine UI-Blockaden.
* Smoothes Live-Preview.

### 6.2 Energieverbrauch

* Pipeline soll adaptiv laufen können (z. B. bei Hitze/Überlast FPS reduzieren).

### 6.3 Robustheit

* Alle Komponenten müssen gegen Abstürze abgesichert sein.
* Kamerafehler sauber abfangen.
* Modell lädt zuverlässig aus Assets.

### 6.4 Lokale Verarbeitung

* Für Prototyp und Datenschutz: **keine Cloud-Verarbeitung**.
* Optional kann eine spätere Cloud- oder Edge-Option ergänzt werden.

---

## 7. Erweiterungen (Future Scope)

### 7.1 Zusätzliche Modelle

* Tiefenschätzung
* Free-Space-Segmentierung
* Verkehrszeichenerkennung
* CLIP/OpenCLIP für Szenenverständnis

### 7.2 Weitere Sensorintegration

* GPS
* IMU (Beschleunigung, Gyroskope)
* Radar oder Ultraschall

### 7.3 Alternative Feedbackkanäle

* Haptische Feedbackmodule am Koerper
* Panoramische Audiowiedergabe

### 7.4 Mehr Kamera-Modi

* Rückfahrkamera
* Weitwinkel
* Externe AI-Kamera

---

## 8. Qualitätsmerkmale

### 8.1 Konsistenz

* Modell-Input-Spezifikationen sind zentral definiert.
* Detector-Interfaces klar entkoppelt von UI.

### 8.2 Testbarkeit

* SceneAnalyzer rein logisch, vollständig unit-testbar.
* Detector mit Fakes testbar.
* Pipeline kann mit Dummy-Frames getestet werden.

### 8.3 Erweiterbarkeit

* Neue Modelle nur über `Detector` hinzufügen.
* Neue Szenen-Logiken nur über neue `SceneAnalyzer`-Varianten.
* UI bleibt stabil.

---

## 9. Risiken & Einschränkungen

* Schlechte Sicht (Regen, Nacht, Gegenlicht) reduziert Leistung.
* GPU/NNAPI-Verfügbarkeit je nach Smartphone.
* Wärmeentwicklung bei Dauerbetrieb.
* Kein Ersatz menschlicher Aufmerksamkeit.

---

## 10. Zusammenfassung

Diese System-Spezifikation legt fest:

* Funktionen der App
* Verhalten der Detektion & Szenenanalyse
* Anforderungen an Performance, Robustheit, Nutzerfeedback
* klare Grenzen (kein Navigationssystem, kein Cloud-Zwang)

Sie dient Codex als Grundlage für nachvollziehbare, konsistente Implementierungsvorschläge.


---

## OwliAI-Modus (aktuell)

- Ansage aller erkannten COCO-Objekte auf Deutsch mit Uhrzeit-Position.
- IoU-Tracker glattet BBox/Confidence (EMA), filtert Noise via minConfidenceTrack, minConsecutiveHits und Max-Age.
- Announce-Planer gruppiert nach Label/Uhrzeit/Distanz und sortiert NEAR->MID->FAR, links->rechts.
- TTS mit Cooldown/Hash-Anti-Spam; Speech-Rate konfigurierbar (Default 2.0).

## Settings & Robustheit (aktuell)

- Settings-Screen (DataStore) für Detector/Tracking/OwliAI/TTS/Debug/Pipeline-Intervall, inkl. Reset auf Defaults.
- Stabilisierung: Motion-Gating (IMU) fuer weniger Ansage-Spam und stabileres Tracking.
- Pipeline wird nach Rotation oder Settings-Änderungen automatisch neu aufgebaut, wenn zuvor aktiv (AutoStart-Flag).
- Overlay/OwliAI-Preview per Setting ein-/ausblendbar.
