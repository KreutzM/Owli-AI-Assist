# Codex Playbook (Team)

Dieses Dokument beschreibt, wie Codex-CLI im Teamkontext genutzt wird – konsistent mit `AGENTS.md`.

## Grundregeln
- Kleine, sinnvolle Inkremente; häufig committen.
- Nach jedem Commit: mindestens `:app:testDebugUnitTest` grün.
- Lint nur suppressen mit Begründung.
- Keine Device-/Emulator-Tasks ohne explizite Anweisung.

## Empfohlene Codex-Session Defaults
- Modell: `gpt-5.2-codex`
- Reasoning: `medium` (nur bei komplexen Refactors `high`/`xhigh`)
- Approvals: so einstellen, dass Codeänderungen sichtbar/reviewbar bleiben.

## Startprompt (Baseline)
Nutze Vorlagen aus `docs/Prompts-Codex-CLI.md`.

## Stop Conditions (wann Codex abbrechen und Rückfrage stellen soll)
- Dependency-Upgrade/Neue Library wäre nötig
- Paket-/Namespace-Rename wäre nötig
- Änderung berührt Sicherheits-/Privacy-Aspekte
- Lint/Test-Fixes erfordern große Refactors statt minimaler Anpassungen

## Review-Checkliste für Menschen
- Diff ist klein & verständlich
- Tests decken neue Logik ab
- Keine Secrets/Keys in Code/Doku/Logs
- Keine Device-Tasks in Scripts/Docs eingeschleust
