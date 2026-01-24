# AGENTS.md

> This file contains project-wide instructions for Codex/CLI agents. Keep these rules short, specific, and actionable.

## Goals
- Deliver small, correct, reviewable improvements.
- Keep Android app behavior stable unless explicitly requested.
- Prefer measurable progress over large refactors.

## Model & reasoning defaults
- **Default model:** `gpt-5.2-codex`.
- **Default reasoning/effort:** **medium**.
- Use **high / xhigh** only for:
  - multi-step refactors spanning multiple modules,
  - subtle concurrency/lifecycle bugs,
  - performance profiling/tuning,
  - complex architecture changes.

## Work style
- Work in **small, meaningful increments**.
- Prefer **short feedback loops**: implement → verify → commit.
- Avoid speculative rewrites.

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
- For new features/behavior changes: add **1–3 focused tests** for the critical logic and edge cases.
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
- `BuildConfig` constants are acceptable **only** if sourced from local, untracked config.

## Repo hygiene
- Do not rename packages/namespaces unless explicitly requested.
- Do not introduce or upgrade dependencies without explicit instruction.
- Avoid touching generated files and IDE configs (`.idea/`, `build/`, etc.) unless requested.

## When uncertain
- Make the smallest reasonable change, leave a TODO with context, and commit.
- Prefer adding diagnostics/logging over guessing.

