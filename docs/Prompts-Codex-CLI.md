# Prompts – Codex CLI (gpt-5.2-codex)

Diese Datei liefert **Prompt-Vorlagen** für Codex-CLI, die zu `AGENTS.md` passen.

Ziel: Arbeiten in **kleinen, sinnvollen Inkrementen** (2 Menschen + 1 Codex-Agent) mit **schnellen Checks** und ohne unnötige Device-Läufe.

---

## 0) Standard-Setup (Windows / PowerShell)

- Wrapper: `gradlew.bat`
- Keine Bash-only Syntax, keine langen `&&`-Ketten, keine Pipes.
- Keine Device-/Emulator-Tasks automatisch (`connectedAndroidTest`, `connectedCheck`, `installDebug` etc.).

**Fast checks (Default):**
- `./gradlew.bat :app:testDebugUnitTest`
- (wenn relevant) `./gradlew.bat :app:lintDebug`
- (wenn UI/Resources/Manifest/Gradle betroffen) `./gradlew.bat :app:assembleDebug`

---

## 1) Template: Small Change (1–2 Commits)

```text
You are in an Android/Kotlin repo on Windows (PowerShell).
Follow AGENTS.md strictly.

Task:
<describe the change briefly>

Rules:
- Work in small, meaningful increments; commit frequently.
- Each commit must pass: ./gradlew.bat :app:testDebugUnitTest
- Run ./gradlew.bat :app:lintDebug when Android components or public APIs are touched.
- Do NOT run any device/emulator tasks.

Start:
1) Summarize where in the code you will change things (files/packages).
2) Propose the first 1–2 small commits (titles + short content).
3) Implement commit #1, run checks, then commit.
```

---

## 2) Template: Bugfix with Regression Test

```text
You are in an Android/Kotlin repo on Windows (PowerShell).
Follow AGENTS.md strictly.

Bug report:
<describe bug + expected behavior>

Requirements:
- Add a regression test in app/src/test that fails before the fix and passes after.
- Keep the fix minimal and focused.
- Run: ./gradlew.bat :app:testDebugUnitTest (and lintDebug if relevant).
- No device/emulator tasks.

Process:
1) Create failing test (commit).
2) Apply minimal fix (commit).
3) Re-run checks; ensure green.
```

---

## 3) Template: Add Tests for Untested Logic

```text
Goal: Increase unit test coverage for pure logic (JVM tests only).

Scope:
- Prefer domain/processing/blindview/motion logic.
- No new dependencies unless explicitly required.

Work:
1) Identify 3–5 high-value functions/classes to test.
2) Add deterministic tests (no network, no sleeps).
3) Keep each test addition as a small commit with a clear message.
4) Run: ./gradlew.bat :app:testDebugUnitTest after each commit.
```

---

## 4) Template: Lint Baseline / Cleanup

```text
Goal: Establish/clean Android Lint baseline without large refactors.

Rules:
- Fix issues where reasonable.
- If suppressing, add a short justification comment.
- Keep diffs small; avoid formatting-only churn.
- Run: ./gradlew.bat :app:lintDebug and ./gradlew.bat :app:testDebugUnitTest
- No device tasks.
```

---

## 5) Template: Documentation Update (Ist-Zustand)

```text
Goal: Update docs to match the current code (Ist-Zustand).

Rules:
- Do not invent features.
- Keep docs concise and aligned to the actual packages and APIs.
- Avoid mechanical reflows.
- If code behavior is ambiguous, add a short NOTE and point to the relevant source file.
```
