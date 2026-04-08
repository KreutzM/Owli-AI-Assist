# VLM-Mode (On-Demand)

## Ziel
- On-demand Szenenbeschreibung ueber OpenRouter (Bild + Text).
- Kein Dauerbetrieb, nur per Button/Frage.
- Antwort wird im Raw-Debug-Mode als Freitext angezeigt (JSON-Parsing aktuell deaktiviert).

## Ablauf
1. UI-Button "VLM" fordert Snapshot an (letzter preprocessierter Frame als JPEG).
2. OpenRouter Chat-Completions Request mit Bild als base64 data URL.
3. Antwort als Freitext anzeigen (Raw-Debug-Mode).
4. Follow-up "Frage stellen" nutzt dieselbe Session-History.

## Datenfluss
- Lokal: Die Live-Kameravorschau laeuft auf dem Geraet; Bilder bleiben lokal, bis der Nutzer `Neue Szene` oder eine Folgefrage mit Bild-Anhaengen ausloest.
- Cloud: Bei `Neue Szene` sendet die App genau ein Snapshot-Bild an OpenRouter. Bei Folgefragen sendet sie den Fragetext sowie optional weitere Bild-Anhaenge derselben Session.
- Rueckfluss: OpenRouter liefert Textantworten zurueck. Diese werden in der App angezeigt und optional per TTS vorgelesen.
- Lokal persistent: Sprache, TTS-Optionen und Profilwahl werden per DataStore auf dem Geraet gespeichert.
- Backup/Restore: Android-Backup und Device-Transfer-Restore sind fuer Release deaktiviert, daher werden diese lokalen Einstellungen nicht per Android-Backup uebertragen.

## Autoscan (optional)
- Wenn das aktive Profil `auto_scan` definiert, zeigt der VLM-Screen einen Auto-Toggle.
- Auto an triggert periodisch denselben "Neue Szene"-Pfad (kein Parallel-Request).
- Auto aus wird beim Verlassen des VLM-Screens automatisch ausgefuehrt.
- Manueller "Neue Szene"-Trigger schaltet Auto aus.

## JSON-Schema (Antwort)
Der JSON-Modus ist aktuell deaktiviert. Das Schema bleibt fuer eine spaetere Reaktivierung vorgesehen.

## Safety-Regeln
- Niemals "Weg frei" sagen, wenn `obstacles` nicht leer sind oder `overall_confidence` nicht `high` ist.
- Keine absoluten Freigaben; immer vorsichtig formulieren.
- Bei unklarer Sicht lieber warnen als freigeben.

## Hinweise
- Der aktuelle OpenRouter-Client-Key kommt lokal aus `local.properties` via `OPENROUTER_API_KEY`.
- Fuer den aktuellen Release-Pfad wird dieser Wert in `BuildConfig` uebernommen und damit mit der App ausgeliefert.
- Das ist eine pragmatische Zwischenloesung, keine sichere Secret-Speicherung.
- Profile und Prompts werden aus `app/src/main/assets/vlm-profiles.json` geladen.
- Fehlerfaelle (Timeout, Parse-Fehler, kein Snapshot) muessen UI-seitig angezeigt werden.
- Aktuell laeuft der VLM im Raw-Debug-Mode: Antwort wird als Freitext angezeigt, JSON-Parsing ist voruebergehend deaktiviert.
- Diktat per Mikrofon: Tippen fuegt Text ins Eingabefeld ein; lang druecken sendet sofort. Sprachausgabe pausiert waehrend der Spracheingabe.
- Play-Store-Offenpunkt: Fuer Release braucht das Projekt weiterhin eine passende Privacy-Policy/Data-Safety-Angabe und eine bewusste Entscheidung, wie lange der app-embedded OpenRouter-Key noch toleriert wird.

## VLM-Profile
- Profile werden aus `app/src/main/assets/vlm-profiles.json` geladen.
- Jedes Profil definiert Modell, Token-Limit sowie `system_prompt` und `overview_prompt`.
- Fuer GPT-5-Modelle wird `reasoning_effort` genutzt (mapping auf `reasoning.effort`) und `temperature` bleibt leer.
- Die Auswahl erfolgt im separaten VLM-Profile-Screen (aus den Settings heraus) und wird als `vlmProfileId` gespeichert.
