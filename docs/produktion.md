# Produktionsfreigabe-Review (Google Play)

Stand: 2026-04-16

Zweck: technisches Repo-Review zur Frage, ob die App in ihrem aktuellen Zustand fuer eine Google-Play-Produktionsfreigabe bereit ist.

Kurzurteil: nein, noch nicht freigabereif.

Die Android-App ist fuer eine Einreichung schon relativ nah an den formalen Play-Basisanforderungen, aber der produktive Betriebs- und Datenschutzpfad ist noch nicht sauber genug abgesichert. Der staerkste Befund liegt nicht im UI oder Manifest, sondern in der Kopplung an das Backend und im noch unvollstaendigen Missbrauchs- und Privacy-Modell.

## Bewertungsgrundlage

Dieses Review basiert auf:

- dem aktuellen App-Repo
- dem lokal vorliegenden Backend-Repo `D:\Codex\OwliAI-BackEnd`
- offiziellen Google-Quellen, Stand 2026-04-16

Massgebliche Google-Quellen:

- Target API requirements for Google Play apps:
  https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
- User Data / Personal and Sensitive User Data:
  https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
- Developer Program Policy:
  https://support.google.com/googleplay/android-developer/answer/16543315?hl=en
- Data safety form guidance:
  https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- Android Developers: Declare your app's data use:
  https://developer.android.com/privacy-and-security/declare-data-use
- Android Developers: About Android App Bundles:
  https://developer.android.com/appbundle
- Android Developers: Core app quality guidelines:
  https://developer.android.com/docs/quality-guidelines/core-app-quality

## Relevante Google-Play-Mindestanforderungen

Fuer diese App sind vor allem diese Punkte relevant:

- Neue Apps und Updates muessen seit 2025-08-31 mindestens Android 15 / API 35 targeten.
- Google Play verlangt eine Data-Safety-Angabe fuer veroeffentlichte Apps, auch auf Test-Tracks.
- Kamera-Bilder und sonstige sensible Nutzungsdaten fallen unter Personal and Sensitive User Data und muessen transparent, sicher und nur zweckgebunden verarbeitet werden.
- Wenn Zugriff, Sammlung oder Weitergabe sensibler Daten ausserhalb der vernuenftigen Nutzererwartung geschieht, ist eine prominente In-App-Disclosure erforderlich.
- Neue Apps muessen als Android App Bundle ausgeliefert werden.
- Die App muss die allgemeinen Core-Quality-Erwartungen fuer Stabilitaet, WebView-Haertung, Security und Play-Policies erfuellen.

## Was bereits freigabenahe ist

### Formale Android-/Play-Basis

- `targetSdk = 36`, `compileSdk = 36`, `minSdk = 26` in `app/build.gradle.kts`
  Bewertung: erfuellt die aktuelle Target-API-Vorgabe klar.
- Manifest enthaelt nur die klar erwartbaren Permissions `CAMERA` und `INTERNET`.
  Bewertung: kein offensichtlicher Policy-Risikofall wie Hintergrundstandort, SMS, Kontakte, Accessibility, Install-Pakete oder All-Files-Access.
- `android:allowBackup="false"` im Manifest.
  Bewertung: reduziert Restore-/Datenmigrationsrisiken.
- Release-Build shippt keinen eingebetteten OpenRouter-Key.
  Bewertung: wichtiger Produktionsfortschritt gegenueber frueheren Zwischenstaenden.
- Die App verwendet den normalen Android-App-Plugin-Pfad und ist bundling-faehig.
  Bewertung: die technische Basis fuer AAB-Auslieferung ist vorhanden.

### Privacy- und Nutzertransparenz-Basis

- In-App-Privacy-Seite vorhanden, plus Website-Link zur vollstaendigen Datenschutzerklaerung.
- Privacy-Dokumentation unterscheidet Backend-Modus und Direct-BYOK-Modus bereits sauber.
- Kamera-Zugriff wird als Runtime-Permission angefordert.
- Es gibt keine Evidenz fuer heimliche Hintergrundaufnahme oder kontinuierliches Hochladen ohne Nutzeraktion.

Inference aus Code und Doku: Eine zusaetzliche prominente Disclosure fuer Kamera-/Netzwerkzugriff scheint derzeit nicht zwingend, weil die Datenuebertragung an eine klar nutzerinitiierte Aktion gekoppelt ist (`Neue Szene`, Folgefrage) und nicht im Hintergrund erfolgt. Das bleibt aber nur dann tragfaehig, wenn Store-Listing, Datenschutzerklaerung und App-UX dieselbe Erwartung klar transportieren.

## Was ausserhalb des Repos nicht nachweisbar ist

Diese Punkte kann das Repo allein nicht belegen. Ohne sie ist eine echte Produktionsfreigabe trotzdem nicht belastbar:

- Play-Console-Data-Safety-Form korrekt und vollstaendig ausgefuellt
- App Content, Content Rating und Store Listing korrekt gepflegt
- Release-Signing / Play App Signing produktiv eingerichtet
- rechtlich gepruefte Privacy Policy mit finalem Verantwortlichen und finalen Empfaengern
- operative Backend-Freigabe fuer `staging` und `prod`
- reale Device-Smoke-Tests des Release-Builds

Fazit fuer diesen Block: selbst bei sauberem Code waere damit nur eine technische Vorpruefung moeglich, keine vollstaendige Google-Freigabebestaetigung.

## Release-Blocker

### 1. Produktionspfad App <-> Backend ist noch nicht sauber freigezogen

Die App arbeitet im Produktionsdefault gegen `https://api.owli-ai.com`, aber im Backend-Repo sind in `wrangler.jsonc` fuer `staging` und `prod` die Kernfeatures weiter deaktiviert:

- `FEATURE_SCENE_DESCRIBE_ENABLED=false`
- `FEATURE_FOLLOWUP_ENABLED=false`
- `OPENROUTER_ENABLED=false`

Bewertung: Solange der produktive End-to-End-Pfad im Repo so aussieht, ist keine belastbare Produktionsfreigabe ableitbar. Entweder ist der produktive Zustand ausserhalb des Repos konfiguriert, oder das Repo beschreibt den Release-Stand nicht korrekt. Beides ist fuer Freigabe und Audit zu schwach.

### 2. Echte App-/Geraete-Attestation fehlt weiterhin

Im Backend ist `src/lib/attestation.ts` noch ein Placeholder-Verifier. In der App wird beim Bootstrap aktuell kein echtes Attestation-Token mitgesendet.

Bewertung: Das ist fuer einen produktiven, oeffentlich erreichbaren AI-Proxy ein echter Freigabeblocker. Google Play verlangt nicht pauschal Play Integrity fuer jede App, aber fuer dieses Bedrohungsmodell ist der Missbrauchsschutz derzeit zu schwach, um den Produktionspfad als gehärtet zu bewerten.

### 3. `sceneToken` traegt Szenentext im lesbaren Payload

Im Backend enthaelt der signierte `sceneToken` weiterhin `sceneText`. Das ist funktional, aber nicht privacy-sauber, weil signiert nicht gleich verschluesselt ist.

Bewertung: Kein formaler Play-Console-Autoreject-Punkt an sich, aber fuer eine Produktionsfreigabe mit Kamera- und Szenendaten ein klarer Datenschutz- und Architekturblocker.

### 4. Backend-Qualitaet ist fuer Produktionsfreigabe nicht ausreichend nachgewiesen

Im Backend-Repo ist keine sichtbare Test-Suite und keine GitHub-Actions-CI fuer zentrale Token-, Validation-, Rate-Limit- und Streaming-Logik vorhanden.

Bewertung: Fuer einen produktiven API-Proxy mit Token- und Bildverarbeitung ist das zu duenn. Ohne reproduzierbare Backend-Checks bleibt das Betriebsrisiko fuer eine Play-Produktionsfreigabe zu hoch.

### 5. Data-Safety- und Privacy-Nachweise sind noch nicht final

Im Repo gibt es bereits gute Vorarbeiten, aber keine Evidenz, dass die finale Play-Console-Data-Safety-Angabe, die Website-Privacy-Policy und die reale Backend-Praxis abschliessend abgeglichen wurden.

Bewertung: Das ist direkt freigaberelevant, weil Google bei Widerspruechen zwischen App-Verhalten und Deklaration einschreiten kann.

## Wichtige Should-fix-Punkte

### 1. Harte Backend-URL in der App

Die App verdrahtet den Backend-Zielhost direkt auf `api.owli-ai.com`.

Bewertung: kein unmittelbarer Policy-Verstoss, aber schlecht fuer Staging, kontrollierte Rollouts und Release-Verifikation. Vor breiter Auslieferung sollte der Environment-Pfad sauber konfigurierbar sein.

### 2. QR-Key-Import mit Default-PIN `1597`

Der QR-Import akzeptiert weiter eine bekannte Default-PIN.

Bewertung: kein Play-Policy-Hard-Blocker, aber fuer Produktionshaertung zu schwach und missverstaendlich. Das sollte vor breiter Freigabe entfernt oder deutlich als schwache Transporthuerde markiert werden.

### 3. WebView-Haertung pruefen

Die Hilfe-/Privacy-WebView laeuft mit `allowFileAccess = true`, JavaScript ist aber deaktiviert.

Bewertung: nicht akut kritisch, aber gemaess Core-App-Quality und Security-Haertung ein sinnvoller Prüfpunkt vor Production.

### 4. Repo-Doku ist nicht ueberall auf demselben Produktionsstand

Das App-Repo ist deutlich besser geworden, aber `README.md` enthaelt noch aeltere Aussagen zum eingebetteten OpenRouter-Key und zum reinen OpenRouter-Datenfluss. Das Backend-README ist ebenfalls teilweise veraltet.

Bewertung: kein direkter Play-Policy-Blocker, aber ein Review- und Wartungsrisiko. Fuer eine Freigabe sollte der dokumentierte Produktionspfad an einer Stelle eindeutig stimmen.

## Gesamtbewertung nach Bereich

### Android / Manifest / Target SDK

Status: gruen

Begruendung:

- aktuelle Target-API-Anforderung erfuellt
- keine offensichtlichen verbotenen oder hochriskanten Permissions
- Runtime-Permission fuer Kamera vorhanden

### App-Privacy-Transparenz

Status: gelb

Begruendung:

- Privacy-Policy und In-App-Hinweise sind vorhanden
- finale Data-Safety- und Empfaenger-Deklaration aber noch nicht als abgeschlossen nachweisbar

### Produktionssicherheit / Abuse Resistance

Status: rot

Begruendung:

- keine echte Attestation
- produktiver Backend-Pfad im Repo nicht sauber aktiviert
- privacy-unsauberes `sceneToken`-Design

### Backend-Betriebsreife

Status: rot

Begruendung:

- Feature-Flags fuer `staging`/`prod` im Repo deaktiviert
- keine sichtbare Backend-Test-Suite und keine CI

### Google-Play-Einreichungsreife insgesamt

Status: rot

Begruendung:

- formale Android-Basis passt
- aber produktiver Serverpfad, Datenschutznachweis und Missbrauchsschutz sind noch nicht releasefest genug

## Entscheidung

Mit dem aktuellen Repo-Stand wuerde ich keine Produktionsfreigabe fuer Google Play empfehlen.

Eine interne Test- oder geschlossene Vorabfreigabe waere eher vertretbar, wenn:

- das Backend operativ separat abgesichert ist,
- reale Release-Smoke-Tests durchgefuehrt wurden,
- und die finale Privacy-/Data-Safety-Deklaration manuell sauber nachgezogen wird.

Fuer eine breite Produktionsfreigabe fehlen aber noch mehrere substanzielle Punkte.

## Priorisierte Freigabevoraussetzungen

Vor einer Google-Play-Produktionsfreigabe sollten mindestens diese Punkte erledigt sein:

1. Produktionspfad App <-> Backend sauber finalisieren und dokumentieren.
2. Echte Attestation integrieren und im Backend enforcebar machen.
3. `sceneToken` ohne lesbaren Szenentext neu designen.
4. Backend-Tests und CI fuer Token-, Validation-, Rate-Limit- und Streaming-Pfade aufsetzen.
5. Finale Data-Safety-Angaben, Privacy Policy und Backend-Praxis gegeneinander abgleichen.
6. Release-Build auf realen Geraeten mit dokumentiertem Smoke-Test pruefen.
7. Environment-Konfiguration der App fuer dev/staging/prod sauber entkoppeln.

## Verwandte interne Dokumente

- `docs/RELEASE-READINESS-CHECKLIST.md`
- `docs/PLAYSTORE-PRIVACY-READINESS.md`
- `docs/ToDo.md`
- `docs/System-Spezifikation.md`
- `docs/System-Architektur.md`
