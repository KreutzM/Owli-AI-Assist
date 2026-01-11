# VLM Profile Editor (Local)

This is a local, static editor for `vlm-profiles.json`.

## Quick Start

1. Open `tools/vlm-profile-editor/index.html` in Chrome.
2. Click **Open JSON** and select `app/src/main/assets/vlm-profiles.json`.
3. Edit profiles, use **Validate** to check for issues, and **Save** (if available) or **Download** to export a pretty-printed JSON file.

## Notes

- This tool is intended for repository maintenance, not for app users.
- Some browsers require a local server for full file access. If file access is blocked, use the download workflow and replace the JSON file manually.
- Save uses the File System Access API when available; otherwise it falls back to a download.
