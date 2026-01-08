# Prompts-Codex-CLI â€“ Vorlagen fÃ¼r ChatGPT5.1-Codex-max

Diese Datei sammelt **Prompt-Bausteine und Muster**, mit denen Codex-CLI (ChatGPT5.1-Codex-max) mÃ¶glichst effizient fÃ¼r dieses Projekt genutzt werden kann.

Alle Prompts beziehen sich auf:

* `System-Architektur.md`
* `System-Spezifikation.md`
* `Coding-Guidelines.md`

Bitte diese Dateien bei komplexeren Aufgaben erwÃ¤hnen oder als Kontext mitsenden.

---

## 1. Allgemeine Empfehlungen fÃ¼r Prompts

* **Konkret sein**: Immer genau angeben, welche Datei, Klasse oder Funktion geÃ¤ndert werden soll.
* **Kontext geben**: Kurz angeben, welchen Teil der Architektur / Spezifikation die Ã„nderung betrifft.
* **Ausgabeformat festlegen**: Z. B. â€žGib nur den kompletten Kotlin-Code der Datei zurÃ¼ck, ohne ErklÃ¤rungen.â€œ
* **Schrittweise arbeiten**: Lieber mehrere kleine, fokussierte Prompts als einen riesigen.

---

## 2. Prompt-Vorlage: Neue Klasse implementieren

**Zweck:** Implementierung einer neuen Klasse gemÃ¤ÃŸ Architektur und Spezifikation.

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
> Gib nur den vollstÃ¤ndigen Kotlin-Code fÃ¼r diese Datei aus, ohne zusÃ¤tzliche ErklÃ¤rungen.

**Beispiel:**

> Implementiere `DefaultVisionPipeline` in `pipeline/DefaultVisionPipeline.kt` im Paket `com.owlitech.owli.assist.pipeline`.
> Anforderungen:
>
> * Implementiert das Interface `VisionPipeline`.
> * Nutzt `CameraFrameSource`, `Preprocessor`, `Detector` und `SceneAnalyzer`.
> * Verarbeitet Frames sequentiell auf einem Hintergrund-Dispatcher.
> * Emittiert `SceneState`-Objekte Ã¼ber einen `StateFlow` oder `SharedFlow`.

---

## 3. Prompt-Vorlage: Bestehende Datei verfeinern / vervollstÃ¤ndigen

**Zweck:** Teilskelett oder alte Version verbessern.

**Muster:**

> Hier ist der aktuelle Inhalt der Datei `<Pfad/Dateiname.kt>`:
>
> ```kotlin
> // aktueller Inhalt
> ```
>
> Bitte vervollstÃ¤ndige oder verbessere diese Datei gemÃ¤ÃŸ `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Spezifische Anforderungen:
>
> * [Anforderung 1]
> * [Anforderung 2]
>
> Ã„ndere keine Paketdeklaration. Gib nur den vollstÃ¤ndigen, aktualisierten Kotlin-Code dieser Datei zurÃ¼ck.

---

## 4. Prompt-Vorlage: Unit-Tests erzeugen

**Zweck:** Tests fÃ¼r eine bestimmte Klasse oder Funktion generieren.

**Muster:**

> Du bist Test-Autor fÃ¼r diese App.
> Halte dich an `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Erzeuge Unit-Tests fÃ¼r die Klasse `<Klassenname>` in `<Pfad/Dateiname.kt>`.
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
> * Teste insbesondere folgende FÃ¤lle:
>
>   * [Testfall 1]
>   * [Testfall 2]
>   * [Testfall 3]
> * Tests sollen deterministisch und unabhÃ¤ngig voneinander sein.
>
> Gib nur den vollstÃ¤ndigen Code der Testdatei zurÃ¼ck (inklusive Paket und Importen).

**Beispiel-TestfÃ¤lle fÃ¼r `SceneAnalyzer`:**

* Wenn eine Person mit hoher Konfidenz zentral unten im Bild erkannt wird â†’ `HazardLevel.DANGER`, `primaryMessage` nicht null.
* Wenn nur Objekte oben im Bild sind â†’ `HazardLevel.WARNING` oder `NONE`, je nach Logik.

---

## 5. Prompt-Vorlage: Refactoring nach Architekturregeln

**Zweck:** Code, der gewachsen ist, nachtrÃ¤glich anpassen.

**Muster:**

> Hier ist der aktuelle Code von `<Pfad/Dateiname.kt>`:
>
> ```kotlin
> // aktueller Code
> ```
>
> Bitte refaktoriere diesen Code im Sinne von `System-Architektur.md` und `Coding-Guidelines.md`:
>
> * Trenne UI-Logik von GeschÃ¤ftslogik.
> * Entferne ML-spezifische Details aus der UI-Schicht.
> * Stelle sicher, dass keine Inferenz auf dem Main-Thread lÃ¤uft.
>
> Gib nur den vollstÃ¤ndigen, refaktorierten Kotlin-Code der Datei zurÃ¼ck.

---

## 6. Prompt-Vorlage: Diskussion / Designentscheidung

**Zweck:** Nicht direkt Code, sondern Architekturfragen / Design.

**Muster:**

> Du kennst die Inhalte aus `System-Architektur.md`, `System-Spezifikation.md` und `Coding-Guidelines.md`.
>
> Diskutiere verschiedene AnsÃ¤tze fÃ¼r:
>
> * [Thema, z. B. â€žAmpelerkennungâ€œ oder â€žDepth-Integrationâ€œ]
>
> Vergleiche Vor- und Nachteile und schlage einen konkreten Ansatz fÃ¼r dieses Projekt vor.
> Antworte strukturiert mit kurzen, prÃ¤gnanten Bullet-Points.

---

## 7. Prompt-Vorlage: Konfiguration und Build-Skripte

**Zweck:** Gradle-Dateien oder Manifest-EintrÃ¤ge generieren.

**Muster:**

> Erzeuge eine vollstÃ¤ndige `app/build.gradle.kts` fÃ¼r dieses Projekt.
>
> Rahmenbedingungen:
>
> * Kotlin, Jetpack Compose, CameraX.
> * AbhÃ¤ngigkeiten fÃ¼r TFLite/LiteRT (oder ONNX, falls angegeben).
> * MinSdk und TargetSdk gemÃ¤ÃŸ Ã¼blichen aktuellen Werten.
> * Halte dich an die Struktur aus `System-Architektur.md`.
>
> Gib nur die vollstÃ¤ndige Datei `app/build.gradle.kts` zurÃ¼ck.

---

## 8. Prompt-Vorlage: Debug-/Diagnose-Funktionen

**Zweck:** Hilfsklassen fÃ¼r Logging, FPS-Messung etc.

**Muster:**

> Implementiere eine Utility-Klasse `AppLogger` im Paket `com.owlitech.owli.assist.util`.
>
> Anforderungen:
>
> * Wrapper um `Log.d/e`.
> * Statische Methoden fÃ¼r Debug/Info/Error.
> * SpÃ¤ter austauschbar gegen eine andere Logging-Library.
>
> Halte dich an `Coding-Guidelines.md`. Gib nur den Kotlin-Code der Datei aus.

---

## 9. Prompt-Vorlage: Code-ErklÃ¤rung / Review

**Zweck:** Generierten oder bestehenden Code besser verstehen.

**Muster:**

> ErklÃ¤re mir den folgenden Kotlin-Code im Kontext der Architektur in `System-Architektur.md`:
>
> ```kotlin
> // Code
> ```
>
> Gehe insbesondere ein auf:
>
> * Wie fÃ¼gt sich dieser Code in die VisionPipeline ein?
> * Welche Threads/Dispatcher werden genutzt?
> * Gibt es erkennbare Probleme oder VerbesserungsmÃ¶glichkeiten?

---

## 10. Empfehlung: Eigene Snippets ergÃ¤nzen

Diese Datei soll im Projektverlauf erweitert werden:

* FÃ¼ge konkrete, bewÃ¤hrte Prompts hinzu, die gut funktioniert haben.
* Markiere, welche Prompts eher fÃ¼r **Implementierung**, welche fÃ¼r **Refactoring** und welche fÃ¼r **Testgenerierung** gedacht sind.

So entsteht ein wachsender â€žPrompt-Werkzeugkastenâ€œ, mit dem Codex-CLI effizient auf das Projekt ausgerichtet werden kann.
