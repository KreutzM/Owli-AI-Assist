# ToDo & Roadmap

Diese Datei ist ein **lebendes** Team-Dokument (2 Menschen + 1 Codex-Agent).
Bitte bei Aenderungen:
- Items klein halten, klar formulieren, mit Akzeptanzkriterium.
- Erledigte Punkte regelmaessig bereinigen (oder nach `docs/ChangeLog.md` uebernehmen).

Legende:
- `[x]` erledigt
- `[ ]` offen
- `[~]` teilweise / experimentell

---

## A) Doku-Kanon / Qualitaet

- [x] Fast Checks sind in `AGENTS.md` verankert (`:app:testDebugUnitTest`, optional `:app:lintDebug`, `:app:assembleDebug`).
- [x] Lint-Setup ist etabliert (`app/lint.xml`) und Baseline bereinigt.
- [x] CI-Workflow fuer Unit-Tests und Lint bei Pull Requests ist vorhanden (`.github/workflows/android-unit-lint.yml`).
- [x] Unit-Test-Suite fuer aktuelle VLM-/Settings-/Audio-Pfade ist vorhanden.

**Naechste sinnvolle Verbesserungen**
- [ ] `docs/README.md` und die Kern-Dokumente bei Architektur- oder Release-Pfadaenderungen konsequent synchron halten.
- [ ] Optional: `DispatcherProvider`/Clock-Injection an zentralen Stellen, um Tests einfacher und deterministischer zu machen.
- [ ] `docs/DEVELOPMENT.md` als operative Einstiegsdoku aktuell halten.

---

## B) Release-/Produktionsreife

- [x] Produktionsdefault ist `BACKEND_MANAGED`.
- [x] BYOK bleibt als separater produktiver Direktpfad erhalten.
- [x] Release-Builds shippen keinen eingebetteten OpenRouter-Key.
- [~] Profilmigration auf Registry-/Remote-First ist teilweise abgeschlossen; `vlm-profiles.json` bleibt noch Legacy-Fallback.

**Naechste sinnvolle Verbesserungen**
- [ ] Kurzen dokumentierten Release-Smoketest fuer reale Geraete festziehen.
- [ ] Backend-SSE- und `/api/v1/profiles`-Verfuegbarkeit vor Releases operativ absichern.
- [ ] Legacy-Rand zwischen oeffentlichem Profilfeed und app-lokalen BYOK-Details weiter reduzieren.

---

## C) VLM / UX

- [x] On-demand VLM mit Follow-ups, Profilen und optionalem Autoscan ist vorhanden.
- [x] Backend- und BYOK-Transport sind im UI unterscheidbar und dokumentiert.
- [~] Zusatzbilder bei Folgefragen sind aktuell nur im direkten `DIRECT_OPENROUTER_BYOK`-Pfad verfuegbar.

**Naechste sinnvolle Verbesserungen**
- [ ] UI-Wording fuer Backend/BYOK/Debug-Fallback weiterhin konsistent halten.
- [ ] Optional: sichtbarer Release-/Support-Hinweis fuer Backend-Einschraenkungen bei Zusatzbildern.

---

## D) Privacy / Store Readiness

- [x] Oeffentliche Privacy-Policy ist in der App verlinkt.
- [~] Repo-Wording unterscheidet Backend, BYOK und Debug-Fallback inzwischen sauberer.

**Naechste sinnvolle Verbesserungen**
- [ ] Finale Play-Console-Data-Safety-Angaben gegen den aktuellen Produktionspfad abgleichen.
- [ ] Backend-/Website-/App-Wording zu Empfaengern, Aufbewahrung und Verantwortlichkeiten final synchronisieren.

---

## E) Optional / Spaeter

- [ ] Haptisches Feedback (Vibration) nur wenn gewuenscht.
- [ ] Instrumented Tests nur wenn ein konkreter Android-Bug ohne JVM-Test nicht abdeckbar ist.
