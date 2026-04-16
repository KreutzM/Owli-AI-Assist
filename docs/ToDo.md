# ToDo & Roadmap

Diese Datei ist der priorisierte Plan fuer die aktuell noch **berechtigten** Review-Befunde in App und Backend.
Bitte bei Aenderungen:
- Items klein halten, klar formulieren, mit Akzeptanzkriterium.
- Erledigte Punkte regelmaessig bereinigen oder nach `docs/ChangeLog.md` uebernehmen.
- Bei App- und Backend-Aenderungen die gekoppelte Gegenseite mitpruefen.

Legende:
- `[P0]` releaseblockierend / vor produktiver Freigabe klaeren
- `[P1]` wichtig / zeitnah nachziehen
- `[P2]` sinnvoll / danach abarbeiten
- `[x]` erledigt

---

## P0) Produktionspfad App <-> Backend finalisieren

- [ ] `wrangler.jsonc` fuer `staging` und `prod` auf einen bewusst freigegebenen Zielzustand bringen.
  Akzeptanzkriterium: `FEATURE_SCENE_DESCRIBE_ENABLED`, `FEATURE_FOLLOWUP_ENABLED` und `OPENROUTER_ENABLED` sind fuer den geplanten Betriebsmodus explizit entschieden und dokumentiert.
- [ ] Release-Freigabeprozess fuer Backend-Managed sauber dokumentieren.
  Akzeptanzkriterium: Es gibt einen kurzen Operator-Runbook-Abschnitt fuer `session/bootstrap`, `scene/describe`, `scene/followup`, SSE und `GET /api/v1/profiles`.
- [ ] App und Backend nicht nur implizit ueber `https://api.owli-ai.com` koppeln, sondern den produktiven Zielpfad explizit beschreiben.
  Akzeptanzkriterium: App-Doku, Backend-Doku und Release-Checkliste nennen denselben produktiven Transportpfad ohne Widerspruch.

## P0) Echte Attestation integrieren

- [ ] Attestation in der App erzeugen und beim Bootstrap mitsenden.
  Akzeptanzkriterium: `POST /api/v1/session/bootstrap` enthaelt im produktiven Pfad ein echtes Attestation-Token.
- [ ] Placeholder-Verifier im Backend durch echten Verifier ersetzen.
  Akzeptanzkriterium: `staging` und `prod` akzeptieren Bootstrap nur noch mit erfolgreicher echter Verifikation.
- [ ] Fehler- und Fallback-Verhalten fuer Attestation sauber dokumentieren.
  Akzeptanzkriterium: Doku beschreibt erlaubte Dev-Fallbacks getrennt von produktivem Verhalten.

## P0) Scene-Token datenschutzsauber umbauen

- [ ] `sceneToken` ohne lesbaren `sceneText` neu designen.
  Akzeptanzkriterium: Follow-up funktioniert ohne im Token offengelegten Szenentext, z. B. ueber opaque Handle oder verschluesselten Kontext.
- [ ] Backend-Follow-up auf das neue Kontextmodell umstellen.
  Akzeptanzkriterium: `scene/followup` nutzt keinen im Klartext dekodierbaren Szeneninhalt mehr.
- [ ] Privacy-Doku und Data-Safety-Angaben an das neue Tokenmodell anpassen.
  Akzeptanzkriterium: App-, Website- und Backend-Doku beschreiben denselben Datenfluss.

---

## P1) Backend-Qualitaet und Absicherung hochziehen

- [ ] Backend-Test-Suite fuer Token-, Validation-, Rate-Limit- und Profilpfade aufbauen.
  Akzeptanzkriterium: Mindestens Session-Token, Scene-Token, Bootstrap-/Describe-/Follow-up-Validierung, Registry-Projection und zentrale Fehlerpfade sind automatisiert getestet.
- [ ] CI fuer das Backend einfuehren.
  Akzeptanzkriterium: Ein Workflow prueft mindestens `npm run check` und die neue Test-Suite bei Pull Requests.
- [ ] Selbstgebaute Tokenlogik entweder haerten oder bewusst absichern.
  Akzeptanzkriterium: Entweder Umstieg auf etablierte JOSE/JWT-Library oder dokumentierte Entscheidung plus gezielte Negativtests fuer Signatur-, Expiry- und Parsing-Fehler.

## P1) Environment-Konfiguration der App enthaerten

- [ ] Backend-API-Base-URL und Profil-Endpoint nicht mehr hart im Code verdrahten.
  Akzeptanzkriterium: dev/staging/prod sind ueber Build-Konfiguration oder klaren App-Config-Pfad unterscheidbar.
- [ ] Release- und Testpfade mit derselben Konfiguration pruefbar machen.
  Akzeptanzkriterium: Release-Smoke-Tests koennen gegen einen nicht-produktiven Zielhost gefahren werden, ohne Source-Code zu patchen.

## P1) Backend-Doku auf Ist-Stand ziehen

- [ ] Veraltete Phase-0-/Scaffold-Reste in Backend-Doku und Metadaten entfernen.
  Akzeptanzkriterium: `package.json`, `README.md` und Schluesseldocs beschreiben den aktuellen Backend-Stand.
- [ ] Widersprueche bei Profil-Endpoint und App-Remote-Loading bereinigen.
  Akzeptanzkriterium: Backend-Doku beschreibt `/api/v1/profiles` und das app-seitige Remote-Loading konsistent.
- [ ] Repo-unsaubere absolute Windows-Pfade aus Markdown-Links entfernen.
  Akzeptanzkriterium: Backend-Doku verwendet nur portable Repo-Pfade.

---

## P2) App-Sicherheits- und UX-Raender nachziehen

- [ ] QR-Key-Import ohne Default-PIN `1597` neu fassen.
  Akzeptanzkriterium: Keine implizite Default-PIN mehr oder mindestens deutlich als schwache Transporthuerde gekennzeichnet und nicht als Schutz suggeriert.
- [ ] Lokale Hilfe-WebView weiter haerten.
  Akzeptanzkriterium: Es ist geprueft und dokumentiert, ob `allowFileAccess` wirklich noetig ist; falls nicht, ist es deaktiviert.
- [ ] Permission-, Offline- und Backend-Fehler-UX produktnaeher machen.
  Akzeptanzkriterium: Kamera-, Netzwerk- und Backend-Fehler haben kurze, accessibility-taugliche Nutzertexte und klaren Retry-Pfad.

## P2) Doku-Kanon weiter sauber halten

- [x] App-Systemdoku (`docs/System-Spezifikation.md`, `docs/System-Architektur.md`, `docs/VLM.md`) ist wieder auf aktuellem Stand.
- [ ] App- und Backend-Doku bei Architektur- oder Release-Pfadaenderungen kuenftig gemeinsam aktualisieren.
  Akzeptanzkriterium: Kein bekannter Widerspruch mehr zwischen App-Repo, Backend-Repo und Privacy-/Release-Doku.
