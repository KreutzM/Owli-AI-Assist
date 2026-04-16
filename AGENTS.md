# AGENTS.md

> This file contains project-wide instructions for Codex/CLI agents. Keep these rules short, specific, and actionable.

## Goals
- Deliver small, correct, reviewable improvements.
- Keep Android app behavior stable unless explicitly requested.
- Prefer measurable progress over large refactors.

## Model & reasoning defaults
- **Default model:** `gpt-5.4`.
- **Default reasoning/effort:** **medium**.
- Use **high / xhigh** only for:
  - multi-step refactors spanning multiple modules,
  - subtle concurrency/lifecycle bugs,
  - performance profiling/tuning,
  - complex architecture changes.

## Work style
- Planning happens in chat; Codex executes one **small thematic run** at a time.
- Start each run on a **thematically named branch**. Do not work on `main` directly.
- Work in **small, meaningful increments**.
- Prefer **short feedback loops**: implement -> verify -> commit.
- Avoid speculative rewrites.
- Inspect the relevant code and docs first, then implement the smallest defensible change.

### Branch closure policy (required)
- Every completed run branch must end in one explicit state:
  - **merged** to `main`
  - **superseded** by a newer branch or commit
  - **kept for later** by explicit decision
  - **deleted** as obsolete
- Do not leave old branches in an ambiguous state.
- If a branch is superseded, record the replacement commit or branch in the final `RUN REVIEW`.
- Before deleting or ignoring an unmerged branch, classify it explicitly as:
  - already contained **in substance**
  - still relevant and pending
  - obsolete and safe to delete

### Commit policy (required)
- Make progress via **frequent, small commits**.
- Each commit must be:
  - **Buildable** (at least `assembleDebug` passes),
  - **Focused** (one logical change),
  - **Documented** (clear message + brief rationale in body if non-obvious).
- When a change touches public behavior, settings, or architecture, update docs **in the same commit**.

**Commit message format**
- `area: short summary`
  - Examples: `pipeline: gate IMU derotation by quality`, `ui: show stabilized input preview toggle`, `docs: update stabilization notes`.

### End-of-run output (required)
- End every Codex run with a compact review packet that can be reviewed in chat or pasted into a PR.
- Do not wait for the user to request it explicitly; every run must include this packet by default.
- If a user prompt requests a different review format for that run, follow the user prompt.
- Do not claim the repo is "clean" unless:
  - the working tree is clean
  - `main` is in sync with `origin/main`
  - no still-relevant local branches remain unreviewed or unclassified
- Use this exact structure:

```text
RUN REVIEW
Branch: <branch-name>
Remote push: <yes/no>
Compare/PR URL: <url-or-n/a>

Scope summary:

* <1 sentence>

Commits:

1. <sha> <subject>
2. <sha> <subject>

Files changed:

* <path>  <short purpose>
* <path>  <short purpose>

Checks run:

* <command>  <pass/fail/not run>
* <command>  <pass/fail/not run>

Behavior impact:

* <none / describe>

Risks / review focus:

* <item 1>
* <item 2>

Manual follow-up:

* <item or "none">

Open questions:

* <item or "none">
```

## Command execution (Windows / PowerShell)
- Prefer **single commands** over long chains; avoid `|`, `&&`, bash-only syntax.
- Use `git grep` instead of `grep`.
- Use the Gradle wrapper on Windows: **`gradlew.bat`**.
- Quote paths when needed and keep them repo-relative.

## Verification checklist (do this in each increment)
1. Review changes:
   - `git status`
   - `git diff`
2. Run **fast checks (default)**:
   - `./gradlew.bat :app:testDebugUnitTest`
   - If you touched resources/manifest/Compose UI, or changed Gradle config: `./gradlew.bat :app:assembleDebug`
3. Run **static checks when relevant**:
   - If you touched Android components (manifest, resources), threading/lifecycle code, or introduced new APIs: `./gradlew.bat :app:lintDebug`
   - Keep lint output clean; only suppress with a short comment explaining why.
4. Run **full checks** when:
   - changes span multiple areas, refactors are non-trivial, or before merging:
   - `./gradlew.bat :app:check` (or, if too slow: `:app:test` + `:app:lintDebug` + `:app:assembleDebug`).
5. If any check fails, fix before committing.

> **Never** run device/emulator tests automatically (e.g. `connectedDebugAndroidTest`, `installDebug`) unless explicitly instructed.

## Testing policy
- Prefer **JVM unit tests** (`app/src/test`) for:
  - domain logic (analyzer, hazard rules),
  - math/geometry/mapping, stabilization heuristics, trackers,
  - parsers/formatters (VLM, TTS chunking),
  - settings/defaults.
- Only add **instrumented tests** (`app/src/androidTest`) when unavoidable and explicitly requested.
- For bugfixes: add a **regression test** (fails before, passes after).
- For new features/behavior changes: add **1-3 focused tests** for the critical logic and edge cases.
- Tests must be deterministic:
  - no real network, no `Thread.sleep`, use fakes/clocks.

## Android-specific guidance
- Respect lifecycle: start/stop camera and sensors safely.
- Avoid work on the main thread; use coroutines/dispatchers appropriately.
- Be explicit about threading when interacting with CameraX/TFLite.
- If you change:
  - permissions,
  - manifests,
  - Gradle config,
  - model assets paths,
  - settings defaults,
  then update documentation.

## Docs policy
- Update docs **only when behavior, setup, settings, or architecture changes**.
- Avoid mechanical reflows or large-format-only diffs.
- Do not add secrets to docs/logs.

## Safety & secrets
- Never commit API keys, tokens, or credentials.
- `local.properties` may contain secrets; do not copy its contents into source.
- Treat `BuildConfig` values as app-shipped material, not secret storage. Only use them for local development when sourced from untracked config and when they are safe to ship or strip before release.

## Repo hygiene
- Do not rename packages/namespaces unless explicitly requested.
- Do not introduce or upgrade dependencies without explicit instruction.
- Avoid touching generated files and IDE configs (`.idea/`, `build/`, etc.) unless requested.

## When uncertain
- Make the smallest reasonable change, leave a TODO with context, and commit.
- Prefer inspection, targeted tests, and narrow verification over guessing. Add minimal temporary instrumentation only when truly necessary to unblock a safe change, and remove it before finishing unless explicitly requested.
