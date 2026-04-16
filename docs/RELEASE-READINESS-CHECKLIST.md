# Release Readiness Checklist

Stand: 2026-04-16

Zweck: knapper, technischer Ist-Abgleich fuer den aktuellen Android-App-Stand vor einer Play-Store-Veroeffentlichung.

Dieses Dokument ist bewusst kein Release-Plan und keine Privacy Policy. Es beschreibt:

- was im App-Repo bereits produktionsnah ist,
- was vor einem Release noch manuell geprueft werden sollte,
- was aktuell als Blocker, Should-fix oder Nice-to-have einzuordnen ist.

## 1) Produktionsmodell (Ist-Stand)

- Normaler Produktionspfad: `BACKEND_MANAGED` gegen `https://api.owli-ai.com`
- Alternativer produktiver Direktpfad: `DIRECT_OPENROUTER_BYOK` mit lokal verschluesselt gespeichertem Nutzer-Key
- Debug-/Entwicklungsfallback: `EMBEDDED_DEBUG` nur fuer Debug-Builds mit lokal gesetztem `OPENROUTER_API_KEY`
- Backend-VLM:
  - `session/bootstrap`
  - `scene/describe`
  - `scene/followup`
  - SSE-Streaming mit `metadata`, `delta`, `done`, `error`
- Profilquelle:
  1. Remote `GET /api/v1/profiles`
  2. lokaler Cache
  3. `vlm-profile-registry.json`
  4. `vlm-profiles.json` als letzter Legacy-Fallback

## 2) Bereits release-nah

- Produktionsdefault ist backend-managed und nicht mehr ein eingebetteter Provider-Key.
- Release-Builds shippen keinen eingebetteten OpenRouter-Key.
- BYOK bleibt als klar separater Modus erhalten.
- Nutzer-Key fuer BYOK wird lokal verschluesselt gespeichert und nicht in DataStore abgelegt.
- QR-Import, manueller Import und Key-Info-Flow sind vorhanden.
- Backend-Streaming ist integriert, inklusive fruehem Fallback auf non-streaming bei Fehlern vor dem ersten sinnvollen Delta.
- Remote-Profile werden bevorzugt geladen, lokal gecached und fallen kontrolliert zurueck.
- Persistierte ungueltige Transportzustaende werden auf einen nutzbaren Produktionspfad normalisiert.
- Privacy-/Data-Safety-Wording im Repo und in der App unterscheidet Backend, BYOK und Debug-Fallback inzwischen klarer.

## 3) Manuelle Verifikation vor Release

### Kritische Geraete-/Build-Checks

- Release-Build auf realem Geraet installieren und pruefen, dass `EMBEDDED_DEBUG` in `Key verwalten` nicht als normaler Produktionspfad auftaucht.
- Erststart online und offline pruefen:
  - online mit erfolgreichem Backend-Bootstrap
  - offline bzw. Backend nicht erreichbar mit sauberem Verhalten der Profil-Fallback-Kette
- Backend-Describe und Backend-Follow-up auf realem Geraet pruefen:
  - fruehe Streaming-Deltas sichtbar
  - sauberer Abschluss
  - kein Rohfehler wie `closed`
- BYOK-Pfad separat pruefen:
  - manueller Key speichern
  - QR-Import mit plain Payload
  - QR-Import mit PIN-geschuetzter Payload
  - Key loeschen
  - Key-Info-Ansicht fuer aktiven BYOK-Key
- Profilwechsel in beiden produktiven Modi pruefen:
  - `BACKEND_MANAGED`
  - `DIRECT_OPENROUTER_BYOK`
- Sprachwechsel pruefen, damit es nur zu der beabsichtigten einzelnen Activity-Neuerstellung kommt.

### UI-/Produktchecks

- Deutsche und englische Texte in `Key verwalten`, Transportwahl, About und Profilauswahl auf echten Geraeten pruefen.
- Kleine Displays pruefen, insbesondere den Key-Management-Unterbildschirm und QR-Import-Flows.
- TTS-Verhalten pruefen, wenn Backend-Streaming fruehe Teiltexte liefert.

## 4) Backend-Abhaengigkeiten fuer Release

- `https://api.owli-ai.com` muss fuer die geplante Release-Welle stabil verfuegbar sein.
- `GET /api/v1/profiles` muss ein client-sicheres, transportbewusstes Profilset liefern, das mit der App-Fallback-Logik kompatibel bleibt.
- Backend-SSE muss weiterhin das normalisierte Eventmodell liefern:
  - `metadata`
  - `delta`
  - `done`
  - `error`
- Rate Limits und Betriebsgrenzen muessen fuer reale Mobile-Nutzung ausreichend sein; das ist app-seitig nicht wegzuabstrahieren.

## 5) Privacy / Data Safety Readiness

- App-Repo-Wording ist jetzt deutlich naeher am realen Verhalten.
- Noch nicht erledigt in diesem Repo:
  - rechtlich gepruefte oeffentliche Privacy Policy
  - finale Play Console Data Safety Angaben
  - finaler Backend-/Website-Abgleich zu Empfaengern, Aufbewahrung und Verantwortlichkeiten

## 6) Release Blockers

- Reale Device-Smoketests fuer Backend-Describe, Backend-Follow-up und BYOK muessen vor Freigabe einmal sauber durchlaufen.
- Release-Build muss explizit bestaetigen, dass kein eingebetteter Provider-Key ausgeliefert wird und kein versteckter Produktionspfad ueber `EMBEDDED_DEBUG` sichtbar bleibt.
- Backend-Verfuegbarkeit, SSE-Verhalten und Profilendpoint muessen fuer den Release-Zeitpunkt operativ abgesichert sein.
- Finale Privacy-/Data-Safety-Angaben ausserhalb des App-Repos muessen vor Store-Einreichung abgestimmt werden.

## 7) Should-fix vor breiter Auslieferung

- Ein kurzer dokumentierter Release-Smoketest-Ablauf fuer menschliche Tester fehlt noch.
- Die aktuelle Profilmigration ist bewusst nur teilweise abgeschlossen; der verbleibende Adapterrand zwischen oeffentlichem Profilfeed und app-lokalen BYOK-Details sollte weiter reduziert werden.
- `docs/VLM.md` sollte der alleinige kanonische VLM-Referenzpunkt bleiben; neue Transport- oder Profilpfade duerfen nicht wieder parallel in Neben-Dokumenten abdriften.

## 8) Nice-to-have

- Eine kleine interne Release-Matrix fuer:
  - online/offline Start
  - Backend/BYOK
  - DE/EN
  - QR plain / QR PIN / manuell
- Ein expliziter Release-Operator-Check fuer Backend-SSE und `/api/v1/profiles` vor jedem App-Release.
- Eine spaetere weitere Reduktion des Legacy-Fallbacks `vlm-profiles.json`, sobald die Registry-Migration abgeschlossen ist.

## 9) Empfohlene naechste Schritte

1. Einen kurzen menschlichen Release-Smoketest definieren und auf mindestens einem echten Android-Geraet durchlaufen.
2. Backend-Verfuegbarkeit, SSE-Verhalten und Profilendpoint fuer den geplanten Release-Termin operativ bestaetigen.
3. Privacy Policy, Play Console Data Safety und Backend-Doku gegen genau dieses Produktionsmodell finalisieren.
