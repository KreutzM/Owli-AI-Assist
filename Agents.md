# AGENTS.md – Owli-AI Assist (Android)

Diese Datei ist die **Single Source of Truth** für Codex-CLI-Arbeit in diesem Repo: Setup, Regeln, Checks, Doku-Pflege.

## Projekt-Identität (fix)
- **Produkt/Brand (sichtbar):** Owli-AI
- **App (diese Repo-Instanz):** Owli-AI Assist
- **applicationId / namespace (Gradle):** `com.owlitech.owli.assist`
- **Code-Root-Package:** `com.owlitech.owli.assist.*`
- Historische Begriffe (BikeBuddy/BikeAssist/BlindView) können im Repo vorkommen und sollen bei passenden Refactors **konsistent auf Owli-AI / Owli-AI Assist** umgestellt werden – **wenn** der Nutzer es beauftragt.

---

## 1) Arbeitsmodus

### Autonomie (wenig Nachfragen)
- Standardannahme: Der Nutzer möchte, dass Codex **30+ Minuten selbständig** arbeiten kann.
- Stelle nur Rückfragen, wenn du **blockiert** bist (z.B. fehlende Datei, uneindeutiger Zielwert, gefährlicher Befehl außerhalb Workspace).
- Triff bei kleinen Detailentscheidungen sinnvolle Defaults und dokumentiere sie kurz in der Batch-Zusammenfassung.

### Kleine, sichere Änderungen
- Arbeite in **kleinen Batches** (max. ~200 Zeilen Diff pro Batch, wenn möglich).
- Nach jedem Batch:
  1) `git diff` zeigen
  2) Build/Test-Check ausführen (siehe „Checks“)
  3) **Docs aktualisieren** (README/CHANGELOG/docs). Hinweis: - Docs niemals mit BOM oder CRLF neu schreiben; UTF-8 ohne BOM + LF beibehalten.
  4) Kurze Zusammenfassung (3–7 Bulletpoints)

### Keine unnötige Format-/Churn-Änderungen
- Kein großflächiges Reformatting „nebenbei“.
- Bei Rename/Move: **`git mv`** bevorzugen (History erhalten).

---

## 2) Windows 10 (PowerShell) – verbindliche Konventionen

### Gradle Wrapper
- Verwende unter PowerShell **immer** `gradlew.bat` (nicht `./gradlew`).
- Beispiele:
  - `gradlew.bat test`
  - `gradlew.bat assembleDebug`

### Pfade & Shell
- Verwende Windows-Pfade robust (keine Bash-Only Syntax).
- Bevorzuge `git grep` statt `grep`.

---

## 3) Setup & Checks (genau so ausführen) (genau so ausführen)

> Codex soll zuerst feststellen, ob es in **WSL/Linux** oder **Windows (PowerShell)** läuft und dann den passenden Wrapper nutzen.

### Minimal (immer nach Code-Änderungen)
- WSL/Linux/macOS:
  - `./gradlew test`
  - `./gradlew assembleDebug`
- Windows/PowerShell:
  - `gradlew.bat test`
  - `gradlew.bat assembleDebug`

### Optional (wenn Build/CI/Release betroffen)
- WSL/Linux/macOS: `./gradlew lintDebug`
- Windows: `gradlew.bat lintDebug`

---

## 4) Doku-Pflege ist Pflicht (nach JEDEM Code-Change)

Nach **jeder** Code-Änderung (Refactor, Bugfix, Feature) muss Codex **in derselben Änderung** prüfen und bei Bedarf aktualisieren:
- `README.md`
- `CHANGELOG.md`
- `docs/*.md` (alle relevanten Dateien)

Regeln:
- Doku muss **konsistent** sein (Namen, Packages, Screens, Setup-Commands, Feature-Beschreibung).
- Wenn Terminologie umgestellt wird (BikeBuddy/BikeAssist → Owli-AI), muss die Doku das widerspiegeln.
- Keine „leeren“ Changelog-Einträge: nur wenn sich Nutzer-/Dev-relevantes Verhalten, Setup oder Architektur ändert.

---

## 5) Security / Secrets (hart)
- **Keine Secrets** in Repo-Dateien (auch nicht „nur testweise“).
- `local.properties` darf **niemals getrackt** sein.
- Wenn ein Secret (z.B. `OPENROUTER_API_KEY`) im Repo war: Nutzer auf **Key-Rotation** hinweisen.

---

## 6) Was Codex ohne Rückfrage darf
- Lesen/Analysieren von Dateien im Workspace
- Ausführen von:
  - `git status`, `git diff`, `git grep`
  - Gradle Checks aus Abschnitt „Setup & Checks“
  - einfache Diagnose-Commands (`pwd`, `ls`)

## 7) Was Codex nur nach expliziter Freigabe darf
- `git commit`, `git push`, `git rebase/merge`, Branch-Wechsel
- Löschen größerer Bereiche / riskante Moves außerhalb klarer Pfade
- Neue Dependencies / Plugin-Upgrades / Kotlin-/AGP-/Gradle-Versionssprünge
- Architektur-Umbauten (z.B. Multi-Module Split) – nur wenn Nutzer das beauftragt

---

## 8) Android-spezifische Leitplanken
- Keine teure Arbeit auf dem Main Thread (Camera/ML/TTS müssen lifecycle-sicher bleiben).
- Compose/UI bleibt „dünn“; Pipeline/ML/IO bleibt in separaten Schichten.
- `applicationId` ist stabil (Play-Store Identität); UI-Name wird über `app_name`/Store gepflegt.
- Beim Package-Rename immer prüfen:
  - `AndroidManifest.xml`
  - Theme-Namen/Refs
  - `BuildConfig`-Imports
  - Tests (`ExampleInstrumentedTest` packageName)

---

## 9) Typischer Codex-Flow (empfohlen)
1) **Inventory**: relevante Dateien/Touchpoints nennen, Plan in 5–10 Bulletpoints.
2) Batchweise implementieren.
3) Nach jedem Batch: diff + Checks.
4) Zum Schluss: Rest-Suche (`git grep`) nach alten Namen/Packages und Doku-Update.

---

## 10) Repo-Dateien, die Codex als „Leitdokumente“ behandeln soll
- `README.md`
- `CHANGELOG.md`
- `docs/*.md`
- Diese `AGENTS.md`

Codex soll bei Widersprüchen **den Nutzer fragen** oder einen klaren Vorschlag machen, welche Quelle „gewinnt“.

