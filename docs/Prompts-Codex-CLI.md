# Prompts - Codex CLI (GPT-5.4)

Diese Datei liefert Prompt-Vorlagen fuer Codex, die zu `AGENTS.md` und `.codex/config.toml` passen.

Ziel: Arbeiten in kleinen, sinnvollen Inkrementen mit schnellen Checks, kleinen Commits und einem klaren Chat-Review nach jedem Run.

---

## 0) Standard-Setup (Windows / PowerShell)

- Wrapper: `gradlew.bat`
- Keine Bash-only Syntax, keine langen `&&`-Ketten, keine Pipes.
- Keine Device-/Emulator-Tasks automatisch (`connectedAndroidTest`, `connectedCheck`, `installDebug` etc.).
- Default-Modell: `gpt-5.4`
- Default-Reasoning: `medium`
- Projekt-Defaults: `.codex/config.toml`

**Fast checks (Default):**
- `./gradlew.bat :app:testDebugUnitTest`
- (wenn relevant) `./gradlew.bat :app:lintDebug`
- (wenn UI/Resources/Manifest/Gradle betroffen) `./gradlew.bat :app:assembleDebug`

---

## 1) Template: Small Thematic Run (1-3 Commits)

```text
You are in an Android/Kotlin repo on Windows (PowerShell).
Follow AGENTS.md strictly.
Use the project defaults from .codex/config.toml.

Task:
<describe the change briefly>

Rules:
- Work on a thematically named branch.
- Work in small, meaningful increments; commit frequently.
- Each commit must pass: ./gradlew.bat :app:testDebugUnitTest
- Run ./gradlew.bat :app:lintDebug when Android components or public APIs are touched.
- Do NOT run any device/emulator tasks.
- End the run with the exact RUN REVIEW packet from AGENTS.md.

Start:
1) Inspect the relevant files and summarize the intended edit surface.
2) Implement the first small commit, run the smallest relevant checks, then commit.
3) Continue only if the next commit stays focused and reviewable.
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
- End with the RUN REVIEW packet from AGENTS.md.

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
1) Identify 3-5 high-value functions/classes to test.
2) Add deterministic tests (no network, no sleeps).
3) Keep each test addition as a small commit with a clear message.
4) Run: ./gradlew.bat :app:testDebugUnitTest after each commit.
5) End with the RUN REVIEW packet from AGENTS.md.
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
- End with the RUN REVIEW packet from AGENTS.md.
```

---

## 5) Template: Documentation / Workflow Update

```text
Goal: Update docs to match the current code and workflow.

Rules:
- Do not invent features.
- Keep docs concise and aligned to the actual packages and APIs.
- Avoid mechanical reflows.
- If code behavior is ambiguous, add a short NOTE and point to the relevant source file.
- Keep the diff tight and prefer 1-2 documentation commits.
- End with the RUN REVIEW packet from AGENTS.md.
```
