#!/usr/bin/env python3
import json
import sys
from pathlib import Path


def is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def validate_profiles(data):
    errors = []

    if not isinstance(data, dict):
        errors.append("Root must be an object.")
        return errors

    default_id = data.get("default_profile_id")
    if not isinstance(default_id, str):
        errors.append("default_profile_id must be a string.")
    elif default_id.strip() == "":
        errors.append("default_profile_id must not be empty.")

    profiles = data.get("profiles")
    if not isinstance(profiles, list):
        errors.append("profiles must be an array.")
        return errors

    ids = set()
    for index, profile in enumerate(profiles):
        path = f"profiles[{index}]"
        if not isinstance(profile, dict):
            errors.append(f"{path} must be an object.")
            continue

        for key in ("id", "label", "description", "model_id"):
            value = profile.get(key)
            if not isinstance(value, str):
                errors.append(f"{path}.{key} must be a string.")
            elif value.strip() == "":
                errors.append(f"{path}.{key} must not be empty.")

        profile_id = profile.get("id")
        if isinstance(profile_id, str):
            if profile_id in ids:
                errors.append(f"{path}.id '{profile_id}' is duplicated.")
            else:
                ids.add(profile_id)

        if "streaming_enabled" in profile and not isinstance(profile["streaming_enabled"], bool):
            errors.append(f"{path}.streaming_enabled must be boolean.")

        if "capabilities" in profile:
            caps = profile["capabilities"]
            if not isinstance(caps, dict):
                errors.append(f"{path}.capabilities must be an object.")
            else:
                for key in ("supports_reasoning", "supports_vision", "supports_json"):
                    if key in caps and not isinstance(caps[key], bool):
                        errors.append(f"{path}.capabilities.{key} must be boolean.")

        if "parameter_overrides" in profile:
            overrides = profile["parameter_overrides"]
            if not isinstance(overrides, dict):
                errors.append(f"{path}.parameter_overrides must be an object.")
            else:
                for key in ("temperature", "top_p"):
                    if key in overrides and not is_number(overrides[key]):
                        errors.append(f"{path}.parameter_overrides.{key} must be a number.")

        if "token_policy" in profile:
            policy = profile["token_policy"]
            if not isinstance(policy, dict):
                errors.append(f"{path}.token_policy must be an object.")
            else:
                if "max_tokens" in policy:
                    value = policy["max_tokens"]
                    if not is_number(value):
                        errors.append(f"{path}.token_policy.max_tokens must be a number.")
                    elif value <= 0:
                        errors.append(f"{path}.token_policy.max_tokens must be > 0.")
                if "reasoning_effort" in policy:
                    value = policy["reasoning_effort"]
                    if not isinstance(value, str):
                        errors.append(f"{path}.token_policy.reasoning_effort must be a string.")
                    elif value.strip() == "":
                        errors.append(f"{path}.token_policy.reasoning_effort must not be empty.")

        if "auto_scan" in profile:
            auto_scan = profile["auto_scan"]
            if not isinstance(auto_scan, dict):
                errors.append(f"{path}.auto_scan must be an object.")
            else:
                if "enabled" in auto_scan and not isinstance(auto_scan["enabled"], bool):
                    errors.append(f"{path}.auto_scan.enabled must be boolean.")
                if "interval_ms" in auto_scan:
                    value = auto_scan["interval_ms"]
                    if not is_number(value):
                        errors.append(f"{path}.auto_scan.interval_ms must be a number.")
                    elif value < 1000:
                        errors.append(f"{path}.auto_scan.interval_ms must be >= 1000.")

    if isinstance(default_id, str) and default_id.strip() != "":
        if default_id not in ids:
            errors.append(
                f"default_profile_id '{default_id}' does not match any profile.id."
            )

    return errors


def main():
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
