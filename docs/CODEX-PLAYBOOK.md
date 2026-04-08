# Codex Playbook (Team)

Dieses Dokument beschreibt, wie Codex im Teamkontext genutzt wird, konsistent mit `AGENTS.md` und `.codex/config.toml`.

## Grundregeln
- Planung passiert im Chat; Codex bearbeitet pro Run genau ein kleines Thema.
- Jeder Run startet auf einem thematisch benannten Branch, nicht auf `main`.
- Kleine, sinnvolle Inkremente; haeufig committen.
- Nach jedem Commit: mindestens `:app:testDebugUnitTest` gruen.
- Lint nur suppressen mit Begruendung.
- Keine Device-/Emulator-Tasks ohne explizite Anweisung.

## Empfohlene Codex-Session Defaults
- Modell: `gpt-5.4`
- Reasoning: `medium` (nur bei komplexen Refactors `high`/`xhigh`)
- Repo-Defaults: `.codex/config.toml`
- Approvals: so einstellen, dass Codeaenderungen sichtbar/reviewbar bleiben.

## Startprompt (Baseline)
Nutze Vorlagen aus `docs/Prompts-Codex-CLI.md`.

## Erwarteter Run-Ablauf
1. Thema und Ziel im Chat festlegen.
2. Codex liest den relevanten Code und die Doku.
3. Codex setzt eine kleine, reviewbare Aenderung um.
4. Codex verifiziert nur die kleinsten relevanten Checks.
5. Codex erstellt kleine Commits und beendet den Run mit dem `RUN REVIEW`-Paket aus `AGENTS.md`.

## Stop Conditions (wann Codex abbrechen und Rueckfrage stellen soll)
- Dependency-Upgrade/Neue Library waere noetig
- Paket-/Namespace-Rename waere noetig
- Aenderung beruehrt Sicherheits-/Privacy-Aspekte
- Lint/Test-Fixes erfordern grosse Refactors statt minimaler Anpassungen

## Review-Checkliste fuer Menschen
- Diff ist klein und verstaendlich
- Tests decken neue Logik ab
- Keine Secrets/Keys in Code/Doku/Logs
- Keine Device-Tasks in Scripts/Docs eingeschleust
- `RUN REVIEW` ist vollstaendig und die Commit-Reihenfolge ist sinnvoll
