# Play Store Privacy Readiness

Stand: 2026-04-08

Kurzer interner Abgleich zwischen aktuellem App-Verhalten und offenen Punkten fuer Play Store / Privacy Preparation.

## Aktueller App-Stand

- Kamera:
  - Die App benoetigt Kamera-Zugriff fuer Live-Vorschau und Snapshot-Erfassung.
  - Die Live-Vorschau laeuft lokal auf dem Geraet.
- Cloud/VLM:
  - Die App nutzt OpenRouter fuer VLM-Anfragen.
  - Daten verlassen das Geraet nur nach expliziter Nutzeraktion, insbesondere `Neue Szene` oder einer Folgefrage.
  - In diesen Pfaden kann die App das Snapshot-Bild, optionale weitere Bild-Anhaenge und den Fragetext an OpenRouter senden.
  - VLM-Antworten kommen als Text zurueck und werden in der App angezeigt; optional liest die App sie per TTS vor.
- Lokal gespeicherte Daten:
  - Spracheinstellung, TTS-Optionen und VLM-Profilwahl werden lokal via DataStore gespeichert.
  - Der aktuelle Chat-Zustand und Bild-Anhaenge sind Session-Daten und nicht als dauerhafte Export-/Sync-Funktion dokumentiert.
- Release defaults:
  - `android:allowBackup="false"`; Android-Backup und Device-Transfer-Restore sind fuer die shipped App deaktiviert.
  - Release-Builds nutzen R8/Minify und Resource-Shrinking.

## Wichtige Caveats

- Der aktuelle OpenRouter-Key ist ein app-embedded Client-Key via `BuildConfig`.
- Das ist fuer den aktuellen Zwischenstand funktional, aber keine sichere Secret-Speicherung.
- Die App arbeitet aktuell im Raw-Text-Modus fuer VLM-Antworten; strukturierte JSON-Nutzerantworten sind nicht aktiv.
- Die Repo-Doku beschreibt den aktuellen technischen Stand, ersetzt aber keine rechtlich gepruefte Privacy Policy oder Data-Safety-Erklaerung.

## Offene Punkte vor Store-Einreichung

- Privacy Policy:
  - Eine oeffentliche, rechtlich gepruefte Privacy Policy muss die realen Cloud-Datenfluesse und den VLM-Anbieter korrekt abdecken.
- Google Play Data Safety:
  - Kamera-Bilder, optionale weitere Bild-Anhaenge und Nutzertext fuer VLM-Anfragen muessen gegen den tatsaechlichen App-Flow geprueft und korrekt deklariert werden.
  - Lokal gespeicherte Einstellungen via DataStore muessen gegen die finale Data-Safety-Angabe abgeglichen werden.
- Anbieter-/Vertragspruefung:
  - Die Nutzung von OpenRouter als externer KI-Dienst sollte fuer Release organisatorisch und datenschutzseitig bewusst freigegeben sein.
- Secret model:
  - Es braucht eine bewusste Release-Entscheidung, ob der app-embedded Provider-Key noch akzeptiert wird oder vor Store-Rollout durch ein Backend-/Token-Modell ersetzt werden muss.
- UI/Store copy:
  - Falls fuer Store-Review oder Nutzertransparenz noetig, sollte die App eine kurze, sichtbare Privacy-Hinweislinie fuer Cloud-VLM-Anfragen erhalten.

## Nicht behaupten

- Nicht behaupten, dass die App vollstaendig offline arbeitet.
- Nicht behaupten, dass eingebettete `BuildConfig`-Werte sichere Secret-Speicherung sind.
- Nicht behaupten, dass Android-Backup lokale App-Daten fuer Release sichert oder migriert.
