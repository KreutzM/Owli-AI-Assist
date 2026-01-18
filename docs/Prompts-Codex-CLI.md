# Prompts-Codex-CLI – Vorlagen für ChatGPT5.1-Codex-max

Diese Datei sammelt **Prompt-Bausteine und Muster**, mit denen Codex-CLI (ChatGPT5.1-Codex-max) möglichst effizient für dieses Projekt genutzt werden kann.

Alle Prompts beziehen sich auf:

* `System-Architektur.md`
* `System-Spezifikation.md`
* `Coding-Guidelines.md`

Bitte diese Dateien bei komplexeren Aufgaben erwähnen oder als Kontext mitsenden.

---

## 1. Allgemeine Empfehlungen für Prompts

* **Konkret sein**: Immer genau angeben, welche Datei, Klasse oder Funktion geändert werden soll.
* **Kontext geben**: Kurz angeben, welchen Teil der Architektur / Spezifikation die Änderung betrifft.
* **Ausgabeformat festlegen**: Z. B. „Gib nur den kompletten Kotlin-Code der Datei zurück, ohne Erklärungen.“
* **Schrittweise arbeiten**: Lieber mehrere kleine, fokussierte Prompts als einen riesigen.

---

## 2. Prompt-Vorlage: Neue Klasse implementieren

**Zweck:** Implementierung einer neuen Klasse gemäß Architektur und Spezifikation.

**Muster:**

> Du bist ChatGPT5.1-Codex-max und arbeitest an einer Android-App.
> Bitte halte dich an die Architektur in `System-Architektur.md`, die Anforderungen in `System-Spezifikation.md` und die Regeln in `Coding-Guidelines.md`.
>
> Implementiere die Klasse `<Klassenname>` in der Datei `<Pfad/Dateiname.kt>` im Paket `<Paketname>`.
>
> Anforderungen:
>
> * [Stichpunkt 1]
> * [Stichpunkt 2]
> * [Stichpunkt 3]
>
> Gib nur den vollständigen Kotlin-Code für diese Datei aus, ohne zusätzliche Erklärungen.

**Beispiel:**

> Implementiere `DefaultVisionPipeline` in `pipeline/DefaultVisionPipeline.kt` im Paket `com.owlitech.owli.assist.pipeline`.
> Anforderungen:
>
> * Implementiert das Interface `VisionPipeline`.
> * Nutzt `CameraFrameSource`, `Preprocessor`, `Detector` und `SceneAnalyzer`.
> * Verarbeitet Frames sequentiell auf einem Hintergrund-Dispatcher.
> * Emittiert `SceneState`-Objekte über einen `StateFlow` oder `SharedFlow`.

---

## 3. Prompt-Vorlage: Bestehende Datei verfeinern / vervollständigen

**Zweck:** Teilskelett oder alte Version verbessern.

**Muster:**

> Hier ist der aktuelle Inhalt der Datei `<Pfad/Dateiname.kt>`:
>
> ```kotlin
> // aktueller Inhalt
> ```
>
> Bitte vervollständige oder verbessere diese Datei gemäß `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Spezifische Anforderungen:
>
> * [Anforderung 1]
> * [Anforderung 2]
>
> Ändere keine Paketdeklaration. Gib nur den vollständigen, aktualisierten Kotlin-Code dieser Datei zurück.

---

## 4. Prompt-Vorlage: Unit-Tests erzeugen

**Zweck:** Tests für eine bestimmte Klasse oder Funktion generieren.

**Muster:**

> Du bist Test-Autor für diese App.
> Halte dich an `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Erzeuge Unit-Tests für die Klasse `<Klassenname>` in `<Pfad/Dateiname.kt>`.
>
> Hier ist der Code der Klasse:
>
> ```kotlin
> // Code der zu testenden Klasse
> ```
>
> Anforderungen an die Tests:
>
> * Nutze JUnit4/JUnit5 (je nach Projektsetup) und Kotlin.
> * Teste insbesondere folgende Fälle:
>
>   * [Testfall 1]
>   * [Testfall 2]
>   * [Testfall 3]
> * Tests sollen deterministisch und unabhängig voneinander sein.
>
> Gib nur den vollständigen Code der Testdatei zurück (inklusive Paket und Importen).

**Beispiel-Testfälle für `SceneAnalyzer`:**

* Wenn eine Person mit hoher Konfidenz zentral unten im Bild erkannt wird → `HazardLevel.DANGER`, `primaryMessage` nicht null.
* Wenn nur Objekte oben im Bild sind → `HazardLevel.WARNING` oder `NONE`, je nach Logik.

---

## 5. Prompt-Vorlage: Refactoring nach Architekturregeln

**Zweck:** Code, der gewachsen ist, nachträglich anpassen.

**Muster:**

> Hier ist der aktuelle Code von `<Pfad/Dateiname.kt>`:
>
> ```kotlin
> // aktueller Code
> ```
>
> Bitte refaktoriere diesen Code im Sinne von `System-Architektur.md` und `Coding-Guidelines.md`:
>
> * Trenne UI-Logik von Geschäftslogik.
> * Entferne ML-spezifische Details aus der UI-Schicht.
> * Stelle sicher, dass keine Inferenz auf dem Main-Thread läuft.
>
> Gib nur den vollständigen, refaktorierten Kotlin-Code der Datei zurück.

---

## 6. Prompt-Vorlage: Diskussion / Designentscheidung

**Zweck:** Nicht direkt Code, sondern Architekturfragen / Design.

**Muster:**

> Du kennst die Inhalte aus `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Diskutiere verschiedene Ansätze für:
>
> * [Thema, z. B. „Ampelerkennung“ oder „Depth-Integration“]
>
> Vergleiche Vor- und Nachteile und schlage einen konkreten Ansatz für dieses Projekt vor.
> Antworte strukturiert mit kurzen, prägnanten Bullet-Points.

---

## 7. Prompt-Vorlage: Konfiguration und Build-Skripte

**Zweck:** Gradle-Dateien oder Manifest-Einträge generieren.

**Muster:**

> Erzeuge eine vollständige `app/build.gradle.kts` für dieses Projekt.
>
> Rahmenbedingungen:
>
> * Kotlin, Jetpack Compose, CameraX.
> * Abhängigkeiten für TFLite/LiteRT (oder ONNX, falls angegeben).
> * MinSdk und TargetSdk gemäß üblichen aktuellen Werten.
> * Halte dich an die Struktur aus `System-Architektur.md`.
>
> Gib nur die vollständige Datei `app/build.gradle.kts` zurück.

---

## 8. Prompt-Vorlage: Debug-/Diagnose-Funktionen

**Zweck:** Hilfsklassen für Logging, FPS-Messung etc.

**Muster:**

> Implementiere eine Utility-Klasse `AppLogger` im Paket `com.owlitech.owli.assist.util`.
>
> Anforderungen:
>
> * Wrapper um `Log.d/e`.
> * Statische Methoden für Debug/Info/Error.
> * Später austauschbar gegen eine andere Logging-Library.
>
> Halte dich an `Coding-Guidelines.md`. Gib nur den Kotlin-Code der Datei aus.

---

## 9. Prompt-Vorlage: Code-Erklärung / Review

**Zweck:** Generierten oder bestehenden Code besser verstehen.

**Muster:**

> Erkläre mir den folgenden Kotlin-Code im Kontext der Architektur in `System-Architektur.md`:
>
> ```kotlin
> // Code
> ```
>
> Gehe insbesondere ein auf:
>
> * Wie fügt sich dieser Code in die VisionPipeline ein?
> * Welche Threads/Dispatcher werden genutzt?
> * Gibt es erkennbare Probleme oder Verbesserungsmöglichkeiten?

---

## 10. Empfehlung: Eigene Snippets ergänzen

Diese Datei soll im Projektverlauf erweitert werden:

* Füge konkrete, bewährte Prompts hinzu, die gut funktioniert haben.
* Markiere, welche Prompts eher für **Implementierung**, welche für **Refactoring** und welche für **Testgenerierung** gedacht sind.

So entsteht ein wachsender „Prompt-Werkzeugkasten“, mit dem Codex-CLI effizient auf das Projekt ausgerichtet werden kann.
