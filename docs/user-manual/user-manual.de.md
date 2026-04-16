# Owli-AI Assist - Benutzerhandbuch (Deutsch)

Stand: 2026-04-16

## 1. Ueberblick

Owli-AI Assist ist eine barrierearme Assistenz-App. Im VLM-Modus richtest du die Kamera aus, nimmst bei Bedarf ein Bild auf und bekommst danach eine Antwort als Text und optional als Sprachausgabe.

## 2. Voraussetzungen

- Android-Smartphone mit Kamera
- Internetverbindung (fuer VLM/LLM)
- Optional: Sprachausgabe (TTS) aktiviert

> Datenschutz: Bilder und Texte werden nur nach deiner ausdruecklichen Aktion an einen KI-Dienst uebertragen, zum Beispiel nach `Neue Szene` oder beim Senden einer Folgefrage.

## 3. Schnellstart

1. App oeffnen.
2. Kamera-Berechtigung erlauben.
3. Kamera in der Live-Vorschau ausrichten.
4. `Neue Szene` tippen, um ein Standbild aufzunehmen.
5. Frage per Text stellen und mit `Senden` abschicken.
6. Alternativ auf das Mikrofon tippen, um Sprache einzufuegen, oder lange druecken, um Sprache direkt zu senden.
7. Mit `Reset` den aktuellen Chat schliessen und zur Live-Vorschau zurueckkehren.

## 4. Hauptansicht

- Live-Vorschau: hilft beim Ausrichten vor der Aufnahme.
- Standbild: wird nach `Neue Szene` angezeigt.
- App-Menue oben rechts: oeffnet Einstellungen, Datenschutzerklaerung, Hilfe und Ueber.
- In-Chat-Aktionen: ueber `Weitere Aktionen` kannst du die letzte Antwort erneut sprechen oder ein weiteres Bild fuer den aktuellen Chat anhaengen.
- Anhaenge: zusaetzliche Bilder fuer den aktuellen Chat verwalten.
- Auto: falls das aktive VLM-Profil Autoscan unterstuetzt, kannst du periodische `Neue Szene`-Anfragen ein- und ausschalten.

## 5. Einstellungen

- VLM-Profil auswaehlen. Die verfuegbaren Profile haengen vom aktiven Transportmodus ab.
- VLM-Transport verwalten:
  - `Owli-Backend` ist der normale Produktionspfad.
  - `Eigener OpenRouter-Key` nutzt direkten BYOK-Betrieb mit deinem lokal verschluesselt gespeicherten Key.
  - In Debug-/Entwicklungs-Builds kann zusaetzlich ein eingebetteter App-Key verfuegbar sein.
- Im Bereich `Key verwalten` kannst du:
  - einen eigenen OpenRouter-Key manuell speichern,
  - ihn per QR-Code importieren,
  - den gespeicherten Key loeschen,
  - den aktiven Transport umschalten,
  - OpenRouter-Key-Infos fuer direkte OpenRouter-Modi abrufen.
- Sprache auf System / Deutsch / Englisch setzen.
- TTS, Sprechtempo, Tonhoehe und optional Streaming-TTS konfigurieren.

## 6. Barrierefreiheit

- Die Steuerelemente folgen einer stabilen Reihenfolge fuer TalkBack.
- Mit Wischgesten kannst du zwischen den Bedienelementen wechseln.
- Die Beschriftungen sind fuer Screenreader ausgelegt.

## 7. Fehlerbehebung

- Keine Antwort: Internetverbindung pruefen.
- Keine Spracheingabe: Verfuegbarkeit der Spracherkennung auf dem Geraet pruefen.
- Kamera funktioniert nicht: Kamera-Berechtigung in Android pruefen.
- Abgehackte Sprachausgabe: Streaming-TTS deaktivieren oder Sprechtempo reduzieren.
- `Auto` fehlt: Das aktuell ausgewaehlte VLM-Profil unterstuetzt keinen Autoscan.
- Zusatzbild bei Folgefrage funktioniert nicht: Zusatzbilder sind aktuell nur im direkten OpenRouter-BYOK-Modus verfuegbar, nicht im Owli-Backend-Modus.

## 8. Feedback

Feedback hilft besonders bei der Frage, welche Aktionen haeufig genutzt werden und wo die Bedienung noch unklar ist.
