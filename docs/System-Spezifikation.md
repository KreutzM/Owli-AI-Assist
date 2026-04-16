# System-Spezifikation

## Zielbild

Die App ist eine VLM-first-Assistenzanwendung mit Live-Kameravorschau zur Ausrichtung und explizit nutzergetriggerter Bildanalyse.
Im Produktionsfall arbeitet sie standardmaessig ueber das Owli-Backend und bietet einen getrennten direkten BYOK-Pfad fuer OpenRouter.

## Funktionsumfang

- Live-Kameravorschau fuer Ausrichtung und Snapshot-Erfassung
- On-demand Szenenbeschreibung ueber `Neue Szene`
- Folgefragen zur zuletzt beschriebenen Szene
- Zusatzbilder bei Folgefragen aktuell nur im direkten `DIRECT_OPENROUTER_BYOK`-Pfad
- Textausgabe in der UI und optionale TTS-/Streaming-TTS-Wiedergabe
- persistente Einstellungen fuer Sprache, TTS, Profilwahl und VLM-Transport
- Hilfe-, About- und Privacy-Zugaenge in der App

## Transport- und Datenmodell

- Produktionsdefault: `BACKEND_MANAGED` gegen `https://api.owli-ai.com`
- Alternativer produktiver Direktpfad: `DIRECT_OPENROUTER_BYOK` mit lokal verschluesselt gespeichertem Nutzer-Key
- Debug-/Entwicklungsfallback: `EMBEDDED_DEBUG` nur in Debug-Builds mit lokalem `OPENROUTER_API_KEY`
- Bilder und Texte verlassen das Geraet nur nach expliziter Nutzeraktion
- Android-Backup fuer App-Daten ist fuer die shipped App deaktiviert

## Konfigurationsmodell

- Die Profilauswahl versucht in dieser Reihenfolge:
  1. `GET /api/v1/profiles`
  2. lokaler Cache
  3. `app/src/main/assets/vlm-profile-registry.json`
  4. `app/src/main/assets/vlm-profiles.json` als letzter Legacy-Fallback
- Persistierte ungueltige Transportzustaende werden beim Start auf einen nutzbaren Produktionspfad normalisiert

## Ausserhalb des Scopes

- Keine dauerhafte Live-Cloudanalyse ohne Nutzeraktion
- Keine aktuelle detector-/hazard-/blindview-Pipeline mehr als Produktkern
- Kein Release-Pfad mit eingebettetem OpenRouter-Key

Detailtiefe zu Build, Workflow und Laufzeitverhalten liegt in `docs/DEVELOPMENT.md`, `docs/VLM.md` und `docs/RELEASE-READINESS-CHECKLIST.md`.
