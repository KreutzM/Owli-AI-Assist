# System-Architektur

## Architekturueberblick

Die App ist um einen VLM-first-Flow herum aufgebaut:

- `ui/`: Compose-Screens, Navigation, Kamera-Preview und Benutzerfluss
- `vlm/`: Transportumschaltung, Backend-/OpenRouter-Clients, Profile, Session- und Parsinglogik
- `settings/`: persistente Einstellungen, Transportwahl, Key-Management und QR-Import
- `audio/`: TTS und Streaming-TTS-Ausgabe
- `util/`: Logging und zentrale App-Links

## Hauptpfade

1. `MainActivity` initialisiert die App und bindet Navigation, Settings und Haupt-ViewModel zusammen.
2. `ui/MainViewModel` orchestriert Snapshot-Erfassung, Anfragefluss, Follow-up-Zustaende und TTS-Ansteuerung.
3. `vlm/SwitchingVlmClient` waehlt zur Laufzeit zwischen `OwliBackendVlmClient`, `OpenRouterVlmClient` und dem Debug-Fallback.
4. `vlm/VlmProfilesRepository` laedt Profile bevorzugt remote und faellt kontrolliert auf Cache, Registry und Legacy-Asset zurueck.
5. `settings/SettingsRepository` und `OpenRouterUserKeyStore` trennen nicht-geheime App-Settings von geheimem BYOK-Material.

## Daten- und Sicherheitsgrenzen

- Live-Kameradaten bleiben lokal, bis eine explizite Nutzeraktion eine Anfrage ausloest.
- Im Backend-Modus gehen Snapshot und Folgefragen an das Owli-Backend; ein direkter OpenRouter-Key wird dabei nicht aus der App verwendet.
- Im BYOK-Modus gehen Snapshot, Folgefrage und optionale Zusatzbilder direkt an OpenRouter.
- Nutzer-Keys werden verschluesselt im Android-Keystore-Kontext gespeichert, nicht in DataStore.

## Kanonische Referenzen

- Produkt- und Verhaltensumfang: `docs/System-Spezifikation.md`
- Workflow, Build und Runtime-Fallbacks: `docs/DEVELOPMENT.md`
- VLM-Transport, Profile und Registry-Migration: `docs/VLM.md`
- Release-Iststand und manuelle Freigabechecks: `docs/RELEASE-READINESS-CHECKLIST.md`
