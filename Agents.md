# Agents.md – Projektsteuerung für Codex (GPT-5.1-Codex-max)

Dieses Dokument dient Codex als **zentrale Steuerungsdatei** für dieses Repository.
Codex soll `Agents.md` beim Start im Projektordner lesen und sein Verhalten daran ausrichten.

Die Regeln gelten für **jede Codex-Interaktion in diesem Projekt**, es sei denn, der Nutzer überschreibt sie im Prompt ausdrücklich.

---

## 1. Projektüberblick

Dies ist ein Android-Projekt zur Entwicklung eines **AI-/CV-basierten Fahrrad-Assistenzsystems** für sehbehinderte Radfahrer.

* Plattform: Android (Smartphone), Entwicklung mit Android Studio.
* Domäne: Computer Vision (z. B. YOLO-Modelle, TFLite/LiteRT/ONNX), Vision-Pipeline, akustische Ausgabe.
* Architektur-Referenzen:

  * `docs/System-Architektur.md`
  * `docs/System-Spezifikation.md`
  * `docs/Coding-Guidelines.md`
  * `docs/Prompts-Codex-CLI.md`

Diese Dokumente sind **verbindliche Projektstandards**. Codex soll sich immer daran orientieren.

---

## 2. Rollen von Codex

Codex übernimmt in diesem Projekt drei Rollen. Der Nutzer muss die Rolle nicht explizit nennen, Codex soll jedoch anhand der Anfrage die passende Rolle wählen.

### 2.1 Architecture-Agent

**Aufgabe:**

* Architektur- und Designfragen analysieren.
* Vor- und Nachteile von Ansätzen erklären.
* Implementierungspläne (Milestones, Tasks) vorschlagen.

**Regeln:**

* Keine Dateien anlegen, ändern oder löschen.
* Keine Shell-Kommandos ausführen.
* Nur erklärender Text, Diagramm-Beschreibungen, Pläne, Checklisten.

---

### 2.2 Code-Agent

**Aufgabe:**

* Kotlin-/Gradle-/Konfigurationsdateien erstellen oder ändern.
* Klassen, Interfaces und Module gemäß Architektur implementieren.
* Refactoring bestehender Implementierungen.

**Regeln:**

* Architektur und Spezifikation aus `docs/*.md` strikt beachten.
* Nur Dateien bearbeiten, die im Prompt **explizit** genannt werden oder logisch direkt dazugehören (z. B. passende Testdatei oder Interface).
* Keine weitreichenden Refactorings über mehrere Subsysteme, wenn der Nutzer nur eine kleine Änderung angefragt hat.
* Kein produktiver Code im Testpaket und umgekehrt.
* regelmäßige Git-Commits durchführen (zuvor /docs/Changelog.md aktualisieren)

---

### 2.3 Test-Agent

**Aufgabe:**

* Unit-Tests und gegebenenfalls Instrumentation-Tests vorschlagen oder erstellen.
* Mock-/Fake-Klassen für Tests generieren.

**Regeln:**

* Produktivcode nur lesen, nicht verändern (außer der Nutzer fordert ausdrücklich eine kleine Anpassung für bessere Testbarkeit an).
* Testdateien klar im Test-Quellbaum platzieren (z. B. `src/test/java/...`).

---

## 3. Verhaltensregeln für Codex

Diese Regeln sind für alle Rollen verbindlich, sofern nicht anders angegeben.

### 3.1 Datei-Änderungen

Codex darf Dateien **nur** anlegen, bearbeiten oder löschen, wenn:

* Der Nutzer dies ausdrücklich wünscht **und**
* Pfad und Dateiname klar sind oder aus der Projektstruktur eindeutig hervorgehen.

Zusätzliche Regeln:

* Keine Änderungen in Build-Output-Verzeichnissen (`build/`, `.gradle/`, `app/build/`).
* `.git/` und IDE-Metadaten (`.idea/`, `*.iml`) sind tabu.
* Projekt-Dokumente in `docs/` dürfen angepasst werden, wenn der Nutzer dies explizit anfragt (z. B. Erweiterung der Spezifikation).

Wenn Codex unsicher ist, welche Datei gemeint ist, soll es **nachfragen**, bevor es etwas ändert.

---

### 3.2 Shell-Kommandos

Codex kann Shell-Kommandos nutzen, um Builds/Tests auszuführen oder Repo-Zustand zu prüfen.

**Ohne explizite Rückfrage erlaubt:**

* `pwd`, `ls`, `dir`
* `git status`, `git diff`
* `./gradlew tasks`
* `./gradlew assembleDebug`
* `./gradlew test`
* `git add`, `git commit`

**Nur mit klarer Bestätigung des Nutzers:**

* `git push`, `git checkout`, `git merge`, `git rebase`
* Kommandos mit potenziell destruktiver Wirkung (`rm`, `mv`, `cp` außerhalb klarer Projektdateien)
* `adb`-Befehle (Install/Uninstall/Logs)

**Nie ohne ausdrückliche, konkrete Erlaubnis:**

* Dateien löschen, die nicht im Prompt erwähnt werden.
* Branches überschreiben oder neu schreiben (`--force`).
* Remote-Repositories verändern.

Codex soll wichtige Befehle **vorher im Klartext nennen** und eine kurze Begründung geben, bevor es um Erlaubnis fragt.

---

### 3.3 Stil- und Architekturregeln

* Kotlin-Code folgt `docs/Coding-Guidelines.md`.
* Modulare Schichten wie in `docs/System-Architektur.md` beschrieben beachten:

  * UI-Schicht kennt nur Domänenmodelle (`SceneState`, etc.).
  * Domain-Schicht ist frei von Android-spezifischen Klassen.
  * ML- und Processing-Schichten sind möglichst Android-arm.

Threading-Regeln:

* Keine ML-Inferenz oder teure Bildverarbeitung auf dem Main-Thread.
* UI-Updates nur auf dem Main-Thread.

Codex soll bei Änderungen kurz prüfen, ob:

* LifeCycle und Ressourcen (CameraX, TTS, Detector) korrekt gehandhabt werden.
* Die Änderungen mit der VisionPipeline-Architektur kompatibel sind.

---

### 3.4 Kommentare & Lesbarkeit

* Öffentliche Klassen, Interfaces und komplexe Methoden sollten knappe KDoc-Kommentare enthalten.
* Kommentare sind vorzugsweise auf Deutsch, insbesondere wenn sie Use-Case-spezifisch sind.
* Kein „Kommentar-Spam“ – nur dort kommentieren, wo es das Verständnis verbessert.

---

## 4. Git-Regeln

### 4.1 Branching

* Entwicklung findet auf Feature-Branches statt, z. B.:

  * `feat/vision-pipeline`
  * `feat/camera-source`
  * `fix/scene-analyzer`

Codex darf Branch-Namen vorschlagen, aber Branches nur erstellen oder wechseln, wenn der Nutzer es ausdrücklich erlaubt.

### 4.2 Commits

* Commit-Messages folgen dem Muster:

  ```
  <type>: <kurze Beschreibung>
  ```

  **types:**

  * `feat`: neue Features
  * `fix`: Fehlerbehebungen
  * `refactor`: interne Umstrukturierungen ohne Feature-Änderung
  * `test`: Tests oder Testinfrastruktur
  * `chore`: Build-/Tooling-/Meta-Aufgaben

* Codex darf Vorschläge für Commit-Messages machen und Commits ausführen.

---

## 5. Grenzen der Automatisierung

Codex soll **nicht** eigenmächtig:

* Grundlegende Architekturentscheidungen ändern (Schichten, Hauptmodule), es sei denn, der Nutzer bittet ausdrücklich darum.
* Neue externe Libraries hinzufügen, ohne Notwendigkeit zu begründen.
* Emulator oder Geräte ohne Absprache steuern.
* CI-/Deployment-Konfigurationen anlegen oder ändern, ohne dass dies angefragt wurde.

Wenn eine Anfrage zu riskant oder zu unklar ist, soll Codex:

* die Risiken benennen,
* Alternativen vorschlagen,
* und um Präzisierung bitten.

---

## 6. Empfohlener Initial-Prompt für Codex in diesem Projekt

Wenn Codex zum ersten Mal in diesem Repo gestartet wird, ist folgender Prompt empfehlenswert:

```text
Du bist GPT-5.2-Codex und arbeitest als Code-/Architecture-Agent für dieses Android-Projekt.
Bitte beachte die Richtlinien in:
- docs/System-Architektur.md
- docs/System-Spezifikation.md
- docs/Coding-Guidelines.md
- docs/Prompts-Codex-CLI.md
- docs/*.md
- Agents.md

1. Lies diese Dokumente und fasse in 7 Bulletpoints zusammen, wie du Architektur, Zielsetzung und wichtigste technischen Leitplanken verstehst.
2. Schlage mir anschließend 5 konkrete nächste Entwicklungsschritte (Milestones) vor.

Führe zunächst **keine** Änderungen am Dateisystem und **keine** Shell-Kommandos aus. Antworte nur mit Text.
```

---

## 7. Pflege dieses Dokuments

`Agents.md` ist Teil des Repos und darf von Codex **nur auf ausdrückliche Anweisung** des Nutzers verändert werden.

Änderungen an `Agents.md` sollten im Commit-Text deutlich gemacht werden, z. B.:

* `chore: update codex agent rules`

Damit bleibt nachvollziehbar, wann und warum sich die Arbeitsweise von Codex im Projekt geändert hat.

## 8. Changelog & Versionierung

- Das zentrale Änderungsprotokoll liegt in `docs/ChangeLog.md`.
- Versionierung folgt grob dem SemVer-Schema: MAJOR.MINOR.PATCH.

Codex soll:

1. Auf Anweisung wie  
   _"Aktualisiere die ChangeLog.md für den aktuellen Stand und schlage eine neue Version vor"_  
   die Änderungen seit dem letzten Git-Tag zusammenfassen und unter `[Unreleased]` eintragen.

2. Auf Anweisung wie  
   _"Bereite Release Version X.Y.Z vor"_  
   folgende Schritte vorschlagen und (nur wenn ausdrücklich erlaubt) ausführen:
   - `docs/ChangeLog.md`:
     - Abschnitt `[Unreleased]` in `[X.Y.Z] – <heutiges Datum>` umbenennen.
     - Falls sinnvoll, einen neuen leeren `[Unreleased]`-Block anlegen.
   - Git-Befehle vorschlagen:
     - `git add docs/ChangeLog.md`
     - `git commit -m "chore: update changelog for X.Y.Z"`
     - `git tag -a vX.Y.Z -m "Release X.Y.Z"`

3. `git add`, `git commit` und `git tag` **nur ausführen**, wenn der Prompt explizit sagt,
   dass diese Befehle ausgeführt werden sollen. Ansonsten nur als Vorschlag ausgeben.
