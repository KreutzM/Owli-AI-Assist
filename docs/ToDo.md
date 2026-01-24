# ToDo & Roadmap

Diese Datei ist ein **lebendes** Team-Dokument (2 Menschen + 1 Codex-Agent).
Bitte bei Änderungen:
- Items klein halten, klar formulieren, mit Akzeptanzkriterium.
- Erledigte Punkte regelmäßig bereinigen (oder nach `docs/ChangeLog.md` übernehmen).

Legende:
- `[x]` erledigt
- `[ ]` offen
- `[~]` teilweise / experimentell

---

## A) Developer Workflow / Qualität

- [x] Fast Checks definiert und in `AGENTS.md` verankert (`:app:testDebugUnitTest`, optional `:app:lintDebug`, `:app:assembleDebug`).
- [x] Lint-Setup etabliert (`app/lint.xml`) und Baseline bereinigt.
- [x] Unit-Test-Suite ausgebaut (u. a. BlindView Planner/Tracker/Formatter/Distance, SceneAnalyzer, StreamingTTS).

**Nächste sinnvolle Verbesserungen**
- [ ] CI Workflow (ohne Emulator): `:app:testDebugUnitTest` + `:app:lintDebug` bei PR/Push.
- [ ] Optional: `DispatcherProvider`/Clock-Injection an zentralen Stellen, um Tests einfacher/deterministischer zu machen.
- [ ] Dokument: `docs/DEVELOPMENT.md` aktuell halten (Setup + Checks + Troubleshooting).

---

## B) CV Pipeline / Stabilisierung

- [x] CameraX Preview + ImageAnalysis (latest wins).
- [x] Preprocessing: Rotation, 448×448 Input, FrameMapping.
- [~] IMU Roll-Derotation (experimentell, quality-gated).
- [~] Translation-Stabilisierung des Crop-Windows (Patch-Matching; quality-gated).

**Nächste sinnvolle Verbesserungen**
- [ ] Mehr Unit-Tests für `processing/` (Mapping, crop update, dx/dy smoothing, quality gating).
- [ ] Optional: NMS-/BoundingBox-Utilities (nur wenn Model/Backend es benötigt).
- [ ] Performance-Messung: einfache Timing-Metriken in Diagnostics verifizieren (kein Benchmarking-Overkill).

---

## C) Scene / BlindView / Hazard

- [x] IoU-Tracking + EMA + Consecutive Hits.
- [x] Announce Planner Tests vorhanden (Basis).
- [~] Hazard-Logik heuristisch (Person/Fahrzeug/Ampel) + Decay.

**Nächste sinnvolle Verbesserungen**
- [ ] Tests für Hazard-Priorisierung & Message-Auswahl (Regressionen vermeiden).
- [ ] Bessere Distanz-Heuristik / Kategorien (nur wenn Produktziel es braucht).

---

## D) VLM (OpenRouter)

- [x] On-Demand VLM mit Profilen (`vlm-profiles.json`), optional Autoscan.
- [x] Streaming-Handling (SSE) + Raw-Debug.

**Nächste sinnvolle Verbesserungen**
- [ ] Dokumentation der Profile/Parameter in `docs/VLM.md` konsolidieren (nur wenn Team es braucht).
- [ ] UX: klare „Privacy“-Hinweise im UI (On-Demand, kein Dauerupload).

---

## E) Optional / Später

- [ ] Haptisches Feedback (Vibration) – nur wenn gewünscht.
- [ ] Instrumented Tests – nur wenn ein konkreter Android-Bug ohne JVM-Test nicht abdeckbar ist.
