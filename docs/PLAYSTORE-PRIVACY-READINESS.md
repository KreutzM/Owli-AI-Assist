# Play Store Privacy Readiness

Stand: 2026-04-16

Kurzer interner Abgleich zwischen aktuellem App-Verhalten und offenen Punkten fuer Play Store / Privacy Preparation.

## Aktueller App-Stand

- Kamera:
  - Die App benoetigt Kamera-Zugriff fuer Live-Vorschau und Snapshot-Erfassung.
  - Die Live-Vorschau laeuft lokal auf dem Geraet.
- Cloud/VLM:
  - Die App nutzt in Produktion standardmaessig das Owli-Backend fuer VLM-Anfragen.
  - Direkter OpenRouter-Verkehr passiert nur im explizit ausgewaehlten BYOK-Modus mit dem lokal gespeicherten Nutzer-Key.
  - Daten verlassen das Geraet nur nach expliziter Nutzeraktion, insbesondere `Neue Szene` oder einer Folgefrage.
  - Backend-Modus: Die App sendet ein Snapshot-Bild fuer `scene/describe` und danach Textfragen fuer `scene/followup` an `api.owli-ai.com`.
  - Direct-BYOK-Modus: Die App kann Snapshot-Bild, optionale weitere Bild-Anhaenge und Fragetext direkt an OpenRouter senden.
  - VLM-Antworten kommen als Text zurueck und werden in der App angezeigt; optional liest die App sie per TTS vor.
- Lokal gespeicherte Daten:
  - Spracheinstellung, TTS-Optionen, Transportmodus und VLM-Profilwahl werden lokal via DataStore gespeichert.
  - Ein optionaler Nutzer-Key fuer Direct-BYOK wird getrennt davon lokal verschluesselt ueber Android Keystore/AES-GCM gespeichert.
  - Oeffentliche Profilmetadaten aus `GET /api/v1/profiles` werden lokal gecached; sie enthalten keine Nutzer-Keys.
  - Der aktuelle Chat-Zustand und Bild-Anhaenge sind Session-Daten und nicht als dauerhafte Export-/Sync-Funktion dokumentiert.
- Release defaults:
  - `android:allowBackup="false"`; Android-Backup und Device-Transfer-Restore sind fuer die shipped App deaktiviert.
  - Release-Builds nutzen R8/Minify und Resource-Shrinking.
  - Release-Builds shippen keinen eingebetteten OpenRouter-Provider-Key.

## Wichtige Caveats

- Der eingebettete OpenRouter-Key ist nur noch ein Debug-/Entwicklungs-Fallback und kein Produktionspfad fuer Release-Builds.
- Die App arbeitet aktuell im Raw-Text-Modus fuer VLM-Antworten; strukturierte JSON-Nutzerantworten sind nicht aktiv.
- Backend-Streaming liefert fruehe Textdeltas; bei fruehem Streamfehler vor dem ersten sinnvollen Delta kann die App lokal auf den nicht-streamenden Backend-Pfad zurueckfallen.
- Backend-Profile kommen bevorzugt remote vom Owli-Backend, fallen aber bei Bedarf auf Cache und lokale Registry/Assets zurueck.
- Die Repo-Doku beschreibt den aktuellen technischen Stand, ersetzt aber keine rechtlich gepruefte Privacy Policy oder Data-Safety-Erklaerung.

## Offene Punkte vor Store-Einreichung

- Privacy Policy:
  - Eine oeffentliche, rechtlich gepruefte Privacy Policy muss Backend-Modus, Direct-BYOK-Modus und die jeweiligen Datenempfaenger korrekt beschreiben.
- Google Play Data Safety:
  - Kamera-Bilder, optionale weitere Bild-Anhaenge und Nutzertext fuer VLM-Anfragen muessen gegen den tatsaechlichen Backend-/BYOK-Flow geprueft und korrekt deklariert werden.
  - Lokal gespeicherte Einstellungen, verschluesselte Nutzer-Keys und gecachte oeffentliche Profilmetadaten muessen gegen die finale Data-Safety-Angabe abgeglichen werden.
- Anbieter-/Vertragspruefung:
  - Die Nutzung des Owli-Backends und des optionalen Direct-BYOK/OpenRouter-Pfads sollte fuer Release organisatorisch und datenschutzseitig bewusst freigegeben sein.
- Backend-/Repo-Abgleich:
  - Die Backend-Dokumentation und die oeffentliche Privacy Policy muessen dieselben Datenfluesse, Empfaenger und Aufbewahrungsannahmen beschreiben wie die App.
- UI/Store copy:
  - Falls fuer Store-Review oder Nutzertransparenz noetig, sollte die App eine kurze, sichtbare Privacy-Hinweislinie fuer Backend-Modus und Direct-BYOK getrennt benennen.

## Nicht behaupten

- Nicht behaupten, dass die App vollstaendig offline arbeitet.
- Nicht behaupten, dass Debug-`BuildConfig`-Werte sichere Secret-Speicherung sind.
- Nicht behaupten, dass Backend-Modus und Direct-BYOK denselben Datenempfaenger oder dieselben Verantwortlichkeiten haben.
- Nicht behaupten, dass Android-Backup lokale App-Daten fuer Release sichert oder migriert.
