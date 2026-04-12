# VLM Profile Editor (Local)

This folder now contains two **developer-only** static editors:

- `index.html` for the current runtime file `vlm-profiles.json`
- `registry.html` for the new canonical `vlm-profile-registry.json`

Both are local tools intended for repo maintenance.

## Start (Chrome)

### Recommended (best UX: open + save-back)
Run a tiny local server:

```bash
cd tools/vlm-profile-editor
python -m http.server 5173
```

Then open in Chrome:

- http://localhost:5173

Now **Open JSON…** and pick one of:

- `app/src/main/assets/vlm-profiles.json`
- `app/src/main/assets/vlm-profile-registry.json` via `registry.html`

With a local server, Chrome can usually use the File System Access API, so **Save** will overwrite the file directly.

### Fallback (works even without a server)
Open one of these files directly in Chrome and use:

- `tools/vlm-profile-editor/index.html`
- `tools/vlm-profile-editor/registry.html`

Then use:
- **Open JSON…** (file input fallback)
- **Download** to export the edited JSON
- Replace the file in the repo manually

## What you can edit

### Legacy runtime editor (`index.html`)

- **Defaults** (applies unless overridden per profile)
  - `defaults.provider`, `defaults.token_policy.max_tokens`
  - `defaults.image.max_side_px`, `defaults.image.jpeg_quality`
  - `defaults.capabilities.*`
  - `defaults.system_prompt`, `defaults.overview_prompt`

- **Profiles**
  - Core: `id`, `label`, `description`, `model_id`, optional `family`
  - Capabilities: `capabilities.supports_*`
  - Token policy: `max_tokens`, `reasoning_effort`, `reasoning_exclude`, `retry1_max_tokens`, `retry2_max_tokens`
  - Overrides: `parameter_overrides.temperature`, `parameter_overrides.top_p`
  - Auto-scan: `enabled_by_default`, `interval_ms`, `speak_free_every_ms`
  - Prompts: `system_prompt`, `overview_prompt`

### Registry editor (`registry.html`)

- Root:
  - `schema_version`
  - `default_profile_id`
- Per profile:
  - Core: `id`, `label`, `description`, `availability`
  - UI: `ui.group`, `ui.sort_order`, `ui.badges`, `ui.hidden`
  - Backend: `backend.profile_id`, streaming/follow-up capability flags, `notes`
  - BYOK: provider/model/family, prompts, capabilities, image, token policy, parameter overrides, optional auto-scan
  - Debug: `debug.embedded_key_allowed`

## Validation

Both editors have built-in validation and there is also a repo validator:

```bash
python tools/validate_vlm_profiles.py
```

or validate a specific file:

```bash
python tools/validate_vlm_profiles.py path/to/vlm-profiles.json
python tools/validate_vlm_profiles.py path/to/vlm-profile-registry.json
```

## Notes

- **Legacy key warning:** `auto_scan.enabled` is not supported. Use `auto_scan.enabled_by_default`.
- `vlm-profile-registry.json` is the new canonical registry foundation, but the Android runtime still reads `vlm-profiles.json` in this phase.
- These tools are intended for repository maintenance only; app users cannot edit profiles in the app.
