# VLM Profile Registry Foundation

Dieses Dokument beschreibt die neue kanonische, transport-aware Registry-Struktur im App-Repo.
Sie ist in diesem Stand noch **nicht** die aktive Runtime-Quelle der Android-App. Die Runtime
laedt weiter `app/src/main/assets/vlm-profiles.json`.

Die neue Grundlage dient drei Zwecken:

1. Ein kanonisches Produktmodell fuer VLM-Profile definieren.
2. Die lokale Pflege im App-Repo ueber Validator und Editor vorbereiten.
3. Einen klaren Handoff-Punkt schaffen, den das Backend-Repo spaeter uebernehmen kann.

## Status in dieser Phase

- Neu: `app/src/main/assets/vlm-profile-registry.json`
- Neu: transport-aware Validierung im Repo-Validator
- Neu: statischer Registry-Editor unter `tools/vlm-profile-editor/registry.html`
- Unveraendert: Android-Runtime nutzt weiter `vlm-profiles.json`
- Nicht Teil dieser Phase: Remote-Profile, Backend-Endpunkte, Runtime-Umschaltung

## Schema

Root:

```json
{
  "schema_version": "vlm_profile_registry/v1",
  "default_profile_id": "gpt52-scene-brief",
  "profiles": [ ... ]
}
```

### Root-Felder

- `schema_version`
  - Versionsmarker fuer die kanonische Registry.
- `default_profile_id`
  - Kanonischer Default auf Registry-Ebene.
  - Dieser Wert ist in diesem Stand noch nicht automatisch der Android-Runtime-Default.
- `profiles`
  - Liste kanonischer Produktprofile.

### Profil-Felder

Jedes Profil beschreibt die Produktidentitaet getrennt von transport-spezifischen Details:

- `id`
  - Stabile kanonische ID.
- `label`
  - Anzeigename fuer Tools und spaetere UI-Abbildungen.
- `description`
  - Kurzer Zweck des Profils.
- `availability`
  - Einer von `backend`, `byok`, `both`.
- `ui`
  - Optionale reine Darstellungsmetadaten.
- `backend`
  - Backend-spezifische Zuordnung und Faehigkeiten.
- `byok`
  - Direkte OpenRouter/BYOK-Konfiguration.
- `debug`
  - Debug-/Dev-Fallback-Metadaten, z.B. ob embedded-key Fallback erlaubt ist.

## Transport-Blöcke

### `backend`

Der `backend`-Block ist bewusst schmal. Er beschreibt keine OpenRouter-Modelldetails,
weil diese spaeter serverseitig kontrolliert werden sollen.

Typische Felder:

- `profile_id`
  - Serverseitige oder kanonische Backend-ID.
- `supports_streaming`
- `supports_followup`
- `supports_followup_images`
- `notes`
  - Optional fuer Migrations-/Ops-Hinweise.

### `byok`

Der `byok`-Block enthaelt den heutigen app-lokalen Direct-OpenRouter-Zuschnitt:

- `provider`
- `model_id`
- `family`
- `streaming_enabled`
- `system_prompt`
- `overview_prompt`
- `capabilities`
- `image`
- `token_policy`
- `parameter_overrides`
- optional `auto_scan`

Das ist absichtlich nah an `vlm-profiles.json`, damit die spaetere Migration nachvollziehbar bleibt.

### `debug`

Der `debug`-Block markiert, ob ein Profil einen eingebetteten App-Key ueberhaupt als
Debug-/Dev-Fallback verwenden darf.

Aktuell:

- `embedded_key_allowed`

Wichtig:

- Embedded-Key ist **nicht** Teil des Produktionsmodells.
- Dieser Block dokumentiert nur den verbleibenden Debug-Fallback.

## Warum zwei Dateien parallel existieren

Aktuell existieren absichtlich beide Dateien:

- `vlm-profiles.json`
  - Runtime-kompatibles App-Asset fuer die heutige Android-App.
- `vlm-profile-registry.json`
  - Neue kanonische Registry fuer Tooling, Migration und spaetere Backend-Uebernahme.

So bleibt das App-Verhalten stabil, waehrend die neue Registry bereits reviewbar und pflegbar ist.

## Erwartete spaetere Schritte im Backend-Repo

Diese Phase implementiert sie **nicht**, bereitet sie aber vor:

1. Das Backend-Repo uebernimmt dieselbe Registry-Grundstruktur oder ein kompatibles Derivat.
2. Backend-seitige Profile bekommen eine serverseitige Auslieferungsquelle.
3. Die Android-App bekommt spaeter einen klaren Loader fuer servergelieferte Registry-Daten.
4. Danach kann `vlm-profiles.json` als reine lokale Fallback-/Dev-Datei verkleinert oder abgeloest werden.

## Editor / Validator

- Validator:
  - `python tools/validate_vlm_profiles.py`
  - Validiert weiterhin das alte Runtime-Asset per Default.
  - Kann jetzt auch die neue Registry-Datei validieren.
- Editor:
  - Legacy-Runtime-Datei: `tools/vlm-profile-editor/index.html`
  - Neue Registry: `tools/vlm-profile-editor/registry.html`

## Nicht Teil dieser Phase

- Keine Backend-Endpunkte
- Keine Remote-Auslieferung
- Keine Android-Runtime-Umschaltung auf die neue Registry
- Keine grosse Profilsystem-Refaktorierung
