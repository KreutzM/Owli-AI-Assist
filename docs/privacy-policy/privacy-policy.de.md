# Owli-AI Assist - Datenschutzerklaerung

Stand: 2026-04-16

## 1. Zweck dieser App

Owli-AI Assist ist eine barrierearme Assistenz-App fuer Bildbeschreibungen und Folgefragen zu einer erfassten Szene.

## 2. Wann Daten das Geraet verlassen

Bilder und Texte werden nur nach deiner ausdruecklichen Aktion uebertragen, zum Beispiel nach `Neue Szene` oder beim Senden einer Folgefrage.

## 3. Produktionsmodus: Owli-Backend

Im normalen Produktionsmodus nutzt die App das Owli-Backend.

- Fuer `Neue Szene` sendet die App ein aufgenommenes Bild an `https://api.owli-ai.com`.
- Fuer Folgefragen sendet die App den Frage-Text an das Owli-Backend.
- Die App verwendet in diesem Modus keinen direkten OpenRouter-Nutzer-Key auf dem Geraet.

## 4. Optionaler Direktmodus: eigener OpenRouter-Key (BYOK)

Wenn du in den Einstellungen bewusst den direkten OpenRouter-BYOK-Modus aktivierst, sendet die App Anfragen direkt an OpenRouter.

- In diesem Modus koennen das Bild der Szene, optionale weitere Bild-Anhaenge und dein Fragetext direkt an OpenRouter gesendet werden.
- Dieser Modus wird nur genutzt, wenn du ihn selbst auswaehlst und ein eigener Key gespeichert ist.

## 5. Lokal gespeicherte Daten

Die App speichert lokal auf dem Geraet:

- Spracheinstellungen
- TTS-Einstellungen
- den gewaehlten VLM-Transportmodus
- das gewaehlte VLM-Profil
- einen optionalen eigenen OpenRouter-Key nur getrennt verschluesselt im Android-Keystore-Kontext
- einen Cache oeffentlicher Profilmetadaten vom Owli-Backend

Der optionale eigene OpenRouter-Key wird nicht im Klartext in normalen Einstellungen gespeichert.

## 6. Was lokal bleibt

Die Live-Kameravorschau laeuft lokal auf dem Geraet. Daten werden erst nach einer ausdruecklichen Nutzeraktion fuer eine VLM-Anfrage uebertragen.

## 7. Profil- und Betriebsdaten

Die App kann oeffentliche Profilinformationen von `https://api.owli-ai.com/api/v1/profiles` laden und lokal cachen, damit die Profilauswahl auch bei temporaeren Backend-Problemen robust bleibt. Dieser Cache enthaelt keine Nutzer-Keys.

## 8. Debug- und Entwicklungsmodus

Ein eingebetteter App-Key ist nur fuer Debug- oder Entwicklungs-Builds vorgesehen und kein normaler Produktionspfad fuer Release-Builds.

## 9. Kontakt

Bei Fragen zum App-Verhalten oder zu Datenschutz-Hinweisen: `feedback@owli-ai.com`

Die vollstaendige Datenschutzerklaerung auf der Website findest du hier: [owli-ai.com/privacy/assist](https://www.owli-ai.com/privacy/assist/)
