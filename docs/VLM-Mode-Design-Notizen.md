# VLM Mode Design Notes (On-Demand)

Hinweis: MVP ist implementiert; aktuelle Details siehe `docs/VLM-Mode.md`.

## Scope
- Historische Design-Notizen zum VLM-MVP.
- On-demand VLM request using the latest camera snapshot.
- No continuous VLM inference.

## Extension Points
- Snapshot capture: best after preprocessing in `DefaultVisionPipeline` where a rotated/scaled `Bitmap` already exists.
- VLM session state: keep a pure domain model, but store the live state in `MainViewModel`.
- UI overlay + controls: Compose layer in `MainActivity` (new composable) should read VLM state.
- TTS output: route through `AudioFeedbackEngine` or a dedicated VLM audio helper in `audio`.
- Speech input: Android-specific controller in UI (or a new `speech` package) that emits intents to the ViewModel.

## Relevant Files + Planned Changes
- `app/src/main/java/com/example/bikeassist/pipeline/DefaultVisionPipeline.kt`
  - Add a thread-safe "latest frame" cache for the last processed `Bitmap` (optionally downscaled).
  - Expose a snapshot provider or method to read the cached frame without blocking the pipeline.
- `app/src/main/java/com/example/bikeassist/pipeline/VisionPipeline.kt`
  - Consider adding a minimal optional interface for snapshot access (or extend `VisionPipelineHandle`).
- `app/src/main/java/com/example/bikeassist/pipeline/VisionPipelineModule.kt`
  - Wire the snapshot provider into the handle so UI can request a frame without touching CameraX.
- `app/src/main/java/com/example/bikeassist/ui/MainViewModel.kt`
  - Add `StateFlow<VlmSessionState>` and a `requestVlm` method that pulls the snapshot and runs VLM off the main thread.
- `app/src/main/java/com/example/bikeassist/domain/VlmSessionState.kt` (new)
  - Pure model with status, lastQuery, lastResult, timestamps, and error message.
- `app/src/main/java/com/example/bikeassist/audio/AudioFeedbackEngine.kt`
  - Add a VLM response speech method or a dedicated VLM audio class with its own cooldown.
- `app/src/main/java/com/example/bikebuddy/MainActivity.kt`
  - Add UI controls (button or voice trigger) and a VLM overlay showing status/result.
  - Connect to `MainViewModel` VLM state and use `AudioFeedbackEngine` for spoken results.
- `app/src/main/java/com/example/bikeassist/settings/AppSettings.kt`
  - Add VLM mode flags/settings only if needed (keep minimal for now).
- `app/src/main/java/com/example/bikeassist/settings/SettingsRepository.kt`
  - Persist VLM settings if introduced (optional).
- `app/src/main/java/com/example/bikeassist/` (new package `vlm` or `ml/vlm`)
  - VLM engine interface and implementation (on-device); called from ViewModel or a coordinator.

## Observations / Inconsistencies
- `AppMode` is passed in settings and `VisionPipelineModule`, but not used by `DefaultVisionPipeline` or updated in `AudioFeedbackEngine`.
- `AudioFeedbackEngine` mode is fixed at construction time; no update when `AppSettings.appMode` changes.
