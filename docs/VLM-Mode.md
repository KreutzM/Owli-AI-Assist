# VLM-Mode (On-Demand)

## Ziel
- On-demand Szenenbeschreibung ueber OpenRouter (Bild + Text).
- Kein Dauerbetrieb, nur per Button/Frage.
- Antwort wird als JSON geparst und via UI/TTS ausgegeben.

## Ablauf
1. UI-Button "VLM" fordert Snapshot an (letzter preprocessierter Frame als JPEG).
2. OpenRouter Chat-Completions Request mit Bild als base64 data URL.
3. Antwort als JSON parsen.
4. UI zeigt die Antwort; TTS spricht `tts_one_liner` + `action_suggestion`.
5. Follow-up "Frage stellen" nutzt dieselbe Session-History.

## JSON-Schema (Antwort)
Pflichtfelder:
- `tts_one_liner` (String, kurz, sprechbar)
- `obstacles` (Array[String])
- `landmarks` (Array[String])
- `readable_text` (String, etwas laenger)
- `action_suggestion` (String, kurz)

Optional:
- `overall_confidence` (String, low|medium|high)

## Safety-Regeln
- Niemals "Weg frei" sagen, wenn `obstacles` nicht leer sind oder `overall_confidence` nicht `high` ist.
- Keine absoluten Freigaben; immer vorsichtig formulieren.
- Bei unklarer Sicht lieber warnen als freigeben.

## Hinweise
- API-Key kommt aus `local.properties` via `OPENROUTER_API_KEY` (BuildConfig).
- Modell-ID und Prompts werden aus `app/src/main/assets/vlm-config.json` geladen.
- Fehlerfaelle (Timeout, Parse-Fehler, kein Snapshot) muessen UI-seitig angezeigt werden.
- Aktuell laeuft der VLM im Raw-Debug-Mode: Antwort wird als Freitext angezeigt, JSON-Parsing ist voruebergehend deaktiviert.

## VLM-Profile
- Profile definieren Modell, Temperatur und Token-Limit.
- Standardprofile:
  - "safe": konservativ (niedrige Temperatur, mehr Tokens).
  - "fast": schneller/guenstiger (hoeherer Temperaturwert, weniger Tokens).
- Die Auswahl erfolgt im Settings-Screen und wird in den App-Settings gespeichert.
