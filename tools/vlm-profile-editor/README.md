# VLM Profile Editor (Local)

This is a **developer-only** editor for `vlm-profiles.json` (the app config). It helps avoid manual JSON errors by providing a structured form + validation.

## Start (Chrome)

### Recommended (best UX: open + save-back)
Run a tiny local server:

```bash
cd tools/vlm-profile-editor
python -m http.server 5173
```

Then open in Chrome:

- http://localhost:5173

Now **Open JSON…** and pick `app/src/main/assets/vlm-profiles.json`.  
With a local server, Chrome can usually use the File System Access API, so **Save** will overwrite the file directly.

### Fallback (works even without a server)
Open `tools/vlm-profile-editor/index.html` directly in Chrome and use:
- **Open JSON…** (file input fallback)
- **Download** to export the edited JSON
- Replace the file in the repo manually

## What you can edit

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

## Validation

The editor has built-in validation and there is also a repo validator:

```bash
python tools/validate_vlm_profiles.py
```

or validate a specific file:

```bash
python tools/validate_vlm_profiles.py path/to/vlm-profiles.json
```

## Notes

- **Legacy key warning:** `auto_scan.enabled` is not supported. Use `auto_scan.enabled_by_default`.
- This tool is intended for repository maintenance only; app users cannot edit profiles in the app (by design).
