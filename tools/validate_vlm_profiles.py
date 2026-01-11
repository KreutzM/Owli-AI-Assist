#!/usr/bin/env python3
"""
validate_vlm_profiles.py

Validates app/src/main/assets/vlm-profiles.json (or a path passed as first argument).

This validator is dependency-free and intended for CI / pre-commit checks.

Rules (high-level):
- Root: { defaults?, default_profile_id, profiles[] }
- default_profile_id must exist and match a profile.id
- profiles[].id must be unique
- Validate types and basic ranges for known fields
- Detect legacy keys that should not appear (auto_scan.enabled)
"""
import json
import sys
from pathlib import Path


def is_number(value) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def is_non_empty_string(value) -> bool:
    return isinstance(value, str) and value.strip() != ""


def validate_profiles(data):
    errors = []

    if not isinstance(data, dict):
        errors.append("Root must be an object.")
        return errors

    # Root keys sanity (unknown root keys are most likely mistakes)
    allowed_root = {"defaults", "default_profile_id", "profiles"}
    for key in data.keys():
        if key not in allowed_root:
            errors.append(f"Unknown root key: '{key}'.")

    default_id = data.get("default_profile_id")
    if not is_non_empty_string(default_id):
        errors.append("default_profile_id must be a non-empty string.")

    profiles = data.get("profiles")
    if not isinstance(profiles, list):
        errors.append("profiles must be an array.")
        return errors

    # defaults (optional)
    defaults = data.get("defaults")
    if defaults is not None:
        if not isinstance(defaults, dict):
            errors.append("defaults must be an object.")
        else:
            provider = defaults.get("provider")
            if provider is not None and not is_non_empty_string(provider):
                errors.append("defaults.provider must be a non-empty string.")

            for key in ("system_prompt", "overview_prompt"):
                v = defaults.get(key)
                if v is not None and not isinstance(v, str):
                    errors.append(f"defaults.{key} must be a string.")

            image = defaults.get("image")
            if image is not None:
                if not isinstance(image, dict):
                    errors.append("defaults.image must be an object.")
                else:
                    for k in ("max_side_px", "jpeg_quality"):
                        if k in image and not is_number(image[k]):
                            errors.append(f"defaults.image.{k} must be a number.")

            token_policy = defaults.get("token_policy")
            if token_policy is not None:
                if not isinstance(token_policy, dict):
                    errors.append("defaults.token_policy must be an object.")
                else:
                    if "max_tokens" in token_policy:
                        v = token_policy["max_tokens"]
                        if not is_number(v) or v <= 0:
                            errors.append("defaults.token_policy.max_tokens must be a number > 0.")

            caps = defaults.get("capabilities")
            if caps is not None:
                if not isinstance(caps, dict):
                    errors.append("defaults.capabilities must be an object.")
                else:
                    for k in ("supports_vision", "supports_reasoning", "supports_json"):
                        if k in caps and not isinstance(caps[k], bool):
                            errors.append(f"defaults.capabilities.{k} must be boolean.")

    ids = set()

    def check_pos_num(path, obj, key):
        if key not in obj:
            return
        v = obj[key]
        if not is_number(v) or v <= 0:
            errors.append(f"{path}.{key} must be a number > 0.")

    for index, profile in enumerate(profiles):
        path = f"profiles[{index}]"
        if not isinstance(profile, dict):
            errors.append(f"{path} must be an object.")
            continue

        for key in ("id", "label", "description", "model_id"):
            if not is_non_empty_string(profile.get(key)):
                errors.append(f"{path}.{key} must be a non-empty string.")

        pid = profile.get("id")
        if isinstance(pid, str):
            if pid in ids:
                errors.append(f"{path}.id '{pid}' is duplicated.")
            ids.add(pid)

        if "family" in profile and not is_non_empty_string(profile.get("family")):
            errors.append(f"{path}.family must be a non-empty string.")

        if "provider" in profile and not is_non_empty_string(profile.get("provider")):
            errors.append(f"{path}.provider must be a non-empty string.")

        if "streaming_enabled" in profile and not isinstance(profile.get("streaming_enabled"), bool):
            errors.append(f"{path}.streaming_enabled must be boolean.")

        for key in ("system_prompt", "overview_prompt"):
            if key in profile and not isinstance(profile.get(key), str):
                errors.append(f"{path}.{key} must be a string.")

        if "capabilities" in profile:
            caps = profile.get("capabilities")
            if not isinstance(caps, dict):
                errors.append(f"{path}.capabilities must be an object.")
            else:
                for k in ("supports_vision", "supports_reasoning", "supports_json"):
                    if k in caps and not isinstance(caps[k], bool):
                        errors.append(f"{path}.capabilities.{k} must be boolean.")

        if "parameter_overrides" in profile:
            overrides = profile.get("parameter_overrides")
            if not isinstance(overrides, dict):
                errors.append(f"{path}.parameter_overrides must be an object.")
            else:
                for k in ("temperature", "top_p"):
                    if k in overrides:
                        v = overrides[k]
                        if not is_number(v):
                            errors.append(f"{path}.parameter_overrides.{k} must be a number.")

        if "token_policy" in profile:
            policy = profile.get("token_policy")
            if not isinstance(policy, dict):
                errors.append(f"{path}.token_policy must be an object.")
            else:
                check_pos_num(f"{path}.token_policy", policy, "max_tokens")
                check_pos_num(f"{path}.token_policy", policy, "retry1_max_tokens")
                check_pos_num(f"{path}.token_policy", policy, "retry2_max_tokens")

                if "reasoning_effort" in policy:
                    v = policy["reasoning_effort"]
                    if not is_non_empty_string(v):
                        errors.append(f"{path}.token_policy.reasoning_effort must be a non-empty string.")

                if "reasoning_exclude" in policy and not isinstance(policy.get("reasoning_exclude"), bool):
                    errors.append(f"{path}.token_policy.reasoning_exclude must be boolean.")

                # ordering hints
                mt = policy.get("max_tokens")
                r1 = policy.get("retry1_max_tokens")
                r2 = policy.get("retry2_max_tokens")
                if is_number(mt) and is_number(r1) and r1 < mt:
                    errors.append(f"{path}.token_policy.retry1_max_tokens should be >= max_tokens.")
                if is_number(r1) and is_number(r2) and r2 < r1:
                    errors.append(f"{path}.token_policy.retry2_max_tokens should be >= retry1_max_tokens.")

        if "auto_scan" in profile:
            auto_scan = profile.get("auto_scan")
            if not isinstance(auto_scan, dict):
                errors.append(f"{path}.auto_scan must be an object.")
            else:
                # Catch legacy key from previous tool bug
                if "enabled" in auto_scan:
                    errors.append(f"{path}.auto_scan.enabled is not supported; use enabled_by_default.")
                if "enabled_by_default" in auto_scan and not isinstance(auto_scan.get("enabled_by_default"), bool):
                    errors.append(f"{path}.auto_scan.enabled_by_default must be boolean.")
                if "interval_ms" in auto_scan:
                    v = auto_scan["interval_ms"]
                    if not is_number(v):
                        errors.append(f"{path}.auto_scan.interval_ms must be a number.")
                    elif v < 1000:
                        errors.append(f"{path}.auto_scan.interval_ms must be >= 1000.")
                if "speak_free_every_ms" in auto_scan:
                    v = auto_scan["speak_free_every_ms"]
                    if not is_number(v):
                        errors.append(f"{path}.auto_scan.speak_free_every_ms must be a number.")
                    elif v < 0:
                        errors.append(f"{path}.auto_scan.speak_free_every_ms must be >= 0.")

    if isinstance(default_id, str) and default_id.strip() != "":
        if default_id not in ids:
            errors.append(f"default_profile_id '{default_id}' does not match any profile.id.")

    return errors


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    default_path = repo_root / "app" / "src" / "main" / "assets" / "vlm-profiles.json"
    target_path = Path(sys.argv[1]) if len(sys.argv) > 1 else default_path

    if not target_path.is_file():
        print(f"ERROR: File not found: {target_path}", file=sys.stderr)
        return 2

    try:
        data = json.loads(target_path.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"ERROR: Failed to parse JSON: {exc}", file=sys.stderr)
        return 1

    errors = validate_profiles(data)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        print(f"Validation failed: {len(errors)} issue(s).", file=sys.stderr)
        return 1

    print(f"OK: {target_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
