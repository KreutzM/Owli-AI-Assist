# Owli‑AI Assist – Benutzerhandbuch (Deutsch)

Stand: 2026-01-24

## 1. Kurzüberblick

Owli‑AI Assist ist eine barrierearme Assistenz‑App. Im **VLM‑Modus** beschreibst du eine Szene „on demand“: Du richtest die Kamera aus, nimmst ein Bild auf und erhältst eine Antwort (Text + optional Sprachausgabe).

Der **Offline‑Detector** ist experimentell und standardmäßig **ausgeblendet**. Er kann in den Einstellungen aktiviert werden.

## 2. Voraussetzungen

- Android‑Smartphone mit Kamera
- Internetverbindung (für VLM/LLM)
- Optional: Sprachausgabe (TTS) aktiviert

> **Datenschutz:** Bilder und Texte werden für den VLM‑Modus an einen KI‑Dienst übertragen (z. B. über OpenRouter), damit die Antwort erzeugt werden kann.

## 3. Schnellstart

1. App öffnen → du befindest dich im **VLM‑Modus**.
2. **Kamera‑Vorschau** erscheint (Live‑Bild).
3. Richte die Kamera aus.
4. Tippe **„Neue Szene“** → die App nimmt ein **Standbild** auf.
5. Stelle deine Frage (Text oder Spracheingabe) und tippe **Senden**.
6. Zum erneuten Zielen: tippe **„Reset“** → zurück zur Live‑Vorschau.

## 4. Hauptbildschirm (VLM)

### 4.1 Kamera‑Vorschau und Standbild

- **Vorschau (Live):** zum Anvisieren vor der Aufnahme.
- **Standbild:** nach „Neue Szene“ wird das aufgenommene Bild angezeigt.

### 4.2 „Neue Szene“ und „Reset“

- **Neue Szene:** setzt den Chat‑Kontext zurück, nimmt ein neues Bild auf und zeigt das Standbild an.
- **Reset:** setzt den Kontext zurück und zeigt wieder die Live‑Vorschau.

### 4.3 Eingabezeile (unten)

Die Eingabezeile besteht aus:

- **Textfeld** (links): Tippe deine Frage/Anweisung.
- **Senden** (mittig/rechts): Sendet deine Nachricht zusammen mit dem aktuellen Bild (und ggf. Anhängen).
- **Mikrofon** (rechts): Spracheingabe.

**Spracheingabe:**
- **Tippen:** Diktieren und Text ins Eingabefeld übernehmen.
- **Gedrückt halten:** Diktieren und danach **sofort senden** (wenn diese Funktion in der UI angeboten wird).

### 4.4 Weitere Aktionen (Drei‑Punkt‑Menü)

Unter **„Weitere Aktionen“** findest du:

- **Wiederhole letzte Antwort:** spricht die letzte LLM‑Antwort erneut.
- **Bild hinzufügen:** nimmt ein weiteres Bild auf und hängt es an den aktuellen Chat an.

### 4.5 Anhänge (mehrere Bilder)

Wenn du mehrere Bilder anhängst (z. B. Detailaufnahme), wird neben der Eingabe ein **Anhang‑Zähler** angezeigt (z. B. „2“).

- Tippe den Zähler, um Anhänge zu verwalten.
- Du kannst einzelne Bilder entfernen.

**Wann ist „Bild hinzufügen“ nützlich?**
- Wenn die KI nach einem Detail fragt (z. B. „Bitte näher ran“).
- Wenn du mehrere Blickwinkel brauchst.

## 5. Einstellungen (VLM)

### 5.1 Profil auswählen

Die App kann unterschiedliche VLM‑Profile anbieten (z. B. „kurz“, „detailliert“). Wähle ein Profil, das zu deinem Use‑Case passt.

### 5.2 Sprache und Sprachausgabe

- **Sprache:** System / Deutsch / Englisch
- **Sprachausgabe (TTS):** Ein/Aus, Sprechtempo, Tonhöhe
- Optional: **Streaming‑TTS**, falls verfügbar (Antwort wird während des Empfangs gesprochen).

### 5.3 Developer / Experimental

- **Offline Detector aktivieren (Experimentell):** blendet den Offline‑Detector‑Modus und dessen Einstellungen ein.
- Hinweis: Dieser Modus ist für Entwickler gedacht und kann die Bedienung komplexer machen.

## 6. Offline Detector (experimentell)

Wenn aktiviert, steht ein separater Modus zur Verfügung, der kontinuierlich Objekte erkennt und Audio‑Hinweise geben kann. Dieser Modus ist **nicht** Teil des Beta‑Flows und kann mehr Optionen enthalten.

## 7. Barrierefreiheit‑Tipps (TalkBack)

- Die App ist auf eine klare Reihenfolge der Steuerelemente ausgelegt.
- Nutze „Wischen“ um Buttons und Eingabefelder anzuspringen.
- Wenn dir ein Button unklar ist, prüfe die Ansage (die App verwendet sprechende Labels).

## 8. Fehlerbehebung

- **Keine Antwort:** Prüfe Internetverbindung.
- **Keine Spracheingabe:** Prüfe, ob Spracheingabe auf deinem Gerät verfügbar ist.
- **Kamera funktioniert nicht:** Prüfe Kamera‑Berechtigung in Android‑Einstellungen.
- **Antwort klingt abgehackt:** Streaming‑TTS deaktivieren oder Sprechtempo reduzieren.

## 9. Feedback

Feedback ist besonders wertvoll: Welche Aktionen nutzt du am häufigsten? Wo ist die Bedienung unklar?

---
*Dieses Handbuch beschreibt den aktuellen Beta‑Stand. Je nach Build können Menüs/Funktionen leicht abweichen.*
