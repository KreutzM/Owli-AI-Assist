#!/usr/bin/env python3
"""
validate_vlm_profiles.py

Validates either:

- app/src/main/assets/vlm-profiles.json
- app/src/main/assets/vlm-profile-registry.json

The script auto-detects the schema from the JSON root and can also validate a custom
path passed as the first argument.
"""
import json
import sys
from pathlib import Path


REGISTRY_SCHEMA_VERSION = "vlm_profile_registry/v1"


def is_number(value) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def is_non_empty_string(value) -> bool:
    return isinstance(value, str) and value.strip() != ""


def validate_known_fields(path, obj, allowed_keys, errors):
    for key in obj.keys():
        if key not in allowed_keys:
            errors.append(f"Unknown key at {path}: '{key}'.")


def check_positive_number(path, obj, key, errors):
    if key not in obj:
        return
    value = obj[key]
    if not is_number(value) or value <= 0:
        errors.append(f"{path}.{key} must be a number > 0.")


def validate_capabilities(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(
        path,
        obj,
        {"supports_vision", "supports_reasoning", "supports_json"},
        errors
    )
    for key in ("supports_vision", "supports_reasoning", "supports_json"):
        if key in obj and not isinstance(obj[key], bool):
            errors.append(f"{path}.{key} must be boolean.")


def validate_image(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(path, obj, {"max_side_px", "jpeg_quality", "detail"}, errors)
    for key in ("max_side_px", "jpeg_quality"):
        if key in obj and not is_number(obj[key]):
            errors.append(f"{path}.{key} must be a number.")
    if "detail" in obj and not is_non_empty_string(obj["detail"]):
        errors.append(f"{path}.detail must be a non-empty string.")


def validate_token_policy(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(
        path,
        obj,
        {"max_tokens", "reasoning_effort", "reasoning_exclude", "retry1_max_tokens", "retry2_max_tokens"},
        errors
    )
    check_positive_number(path, obj, "max_tokens", errors)
    check_positive_number(path, obj, "retry1_max_tokens", errors)
    check_positive_number(path, obj, "retry2_max_tokens", errors)
    if "reasoning_effort" in obj and not is_non_empty_string(obj["reasoning_effort"]):
        errors.append(f"{path}.reasoning_effort must be a non-empty string.")
    if "reasoning_exclude" in obj and not isinstance(obj["reasoning_exclude"], bool):
        errors.append(f"{path}.reasoning_exclude must be boolean.")
    max_tokens = obj.get("max_tokens")
    retry1 = obj.get("retry1_max_tokens")
    retry2 = obj.get("retry2_max_tokens")
    if is_number(max_tokens) and is_number(retry1) and retry1 < max_tokens:
        errors.append(f"{path}.retry1_max_tokens should be >= max_tokens.")
    if is_number(retry1) and is_number(retry2) and retry2 < retry1:
        errors.append(f"{path}.retry2_max_tokens should be >= retry1_max_tokens.")


def validate_parameter_overrides(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(path, obj, {"temperature", "top_p", "reasoning_effort"}, errors)
    for key in ("temperature", "top_p"):
        if key in obj and not is_number(obj[key]):
            errors.append(f"{path}.{key} must be a number.")
    if "reasoning_effort" in obj and not is_non_empty_string(obj["reasoning_effort"]):
        errors.append(f"{path}.reasoning_effort must be a non-empty string.")


def validate_auto_scan(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(
        path,
        obj,
        {"enabled_by_default", "interval_ms", "speak_free_every_ms"},
        errors
    )
    if "enabled" in obj:
        errors.append(f"{path}.enabled is not supported; use enabled_by_default.")
    if "enabled_by_default" in obj and not isinstance(obj["enabled_by_default"], bool):
        errors.append(f"{path}.enabled_by_default must be boolean.")
    if "interval_ms" in obj:
        value = obj["interval_ms"]
        if not is_number(value):
            errors.append(f"{path}.interval_ms must be a number.")
        elif value < 1000:
            errors.append(f"{path}.interval_ms must be >= 1000.")
    if "speak_free_every_ms" in obj:
        value = obj["speak_free_every_ms"]
        if not is_number(value):
            errors.append(f"{path}.speak_free_every_ms must be a number.")
        elif value < 0:
            errors.append(f"{path}.speak_free_every_ms must be >= 0.")


def validate_legacy_profiles(data):
    errors = []

    if not isinstance(data, dict):
        errors.append("Root must be an object.")
        return errors

    validate_known_fields(
        "root",
        data,
        {"defaults", "default_profile_id", "profiles"},
        errors
    )

    default_id = data.get("default_profile_id")
    if not is_non_empty_string(default_id):
        errors.append("default_profile_id must be a non-empty string.")

    profiles = data.get("profiles")
    if not isinstance(profiles, list):
        errors.append("profiles must be an array.")
        return errors

    defaults = data.get("defaults")
    if defaults is not None:
        if not isinstance(defaults, dict):
            errors.append("defaults must be an object.")
        else:
            validate_known_fields(
                "defaults",
                defaults,
                {
                    "provider", "family", "system_prompt", "overview_prompt", "image",
                    "token_policy", "parameter_overrides", "capabilities", "streaming_enabled"
                },
                errors
            )
            if "provider" in defaults and not is_non_empty_string(defaults["provider"]):
                errors.append("defaults.provider must be a non-empty string.")
            if "family" in defaults and not is_non_empty_string(defaults["family"]):
                errors.append("defaults.family must be a non-empty string.")
            for key in ("system_prompt", "overview_prompt"):
                if key in defaults and not isinstance(defaults[key], str):
                    errors.append(f"defaults.{key} must be a string.")
            if "streaming_enabled" in defaults and not isinstance(defaults["streaming_enabled"], bool):
                errors.append("defaults.streaming_enabled must be boolean.")
            if "image" in defaults:
                validate_image("defaults.image", defaults["image"], errors)
            if "token_policy" in defaults:
                validate_token_policy("defaults.token_policy", defaults["token_policy"], errors)
            if "parameter_overrides" in defaults:
                validate_parameter_overrides("defaults.parameter_overrides", defaults["parameter_overrides"], errors)
            if "capabilities" in defaults:
                validate_capabilities("defaults.capabilities", defaults["capabilities"], errors)

    ids = set()
    for index, profile in enumerate(profiles):
        path = f"profiles[{index}]"
        if not isinstance(profile, dict):
            errors.append(f"{path} must be an object.")
            continue
        validate_known_fields(
            path,
            profile,
            {
                "id", "label", "description", "model_id", "model", "provider", "family",
                "system_prompt", "overview_prompt", "image", "token_policy", "parameter_overrides",
                "capabilities", "streaming_enabled", "auto_scan", "temperature", "max_tokens"
            },
            errors
        )
        for key in ("id", "label", "description", "model_id"):
            if not is_non_empty_string(profile.get(key)):
                errors.append(f"{path}.{key} must be a non-empty string.")
        profile_id = profile.get("id")
        if isinstance(profile_id, str):
            if profile_id in ids:
                errors.append(f"{path}.id '{profile_id}' is duplicated.")
            ids.add(profile_id)
        if "family" in profile and not is_non_empty_string(profile["family"]):
            errors.append(f"{path}.family must be a non-empty string.")
        if "provider" in profile and not is_non_empty_string(profile["provider"]):
            errors.append(f"{path}.provider must be a non-empty string.")
        if "streaming_enabled" in profile and not isinstance(profile["streaming_enabled"], bool):
            errors.append(f"{path}.streaming_enabled must be boolean.")
        for key in ("system_prompt", "overview_prompt"):
            if key in profile and not isinstance(profile[key], str):
                errors.append(f"{path}.{key} must be a string.")
        if "capabilities" in profile:
            validate_capabilities(f"{path}.capabilities", profile["capabilities"], errors)
        if "parameter_overrides" in profile:
            validate_parameter_overrides(f"{path}.parameter_overrides", profile["parameter_overrides"], errors)
        if "token_policy" in profile:
            validate_token_policy(f"{path}.token_policy", profile["token_policy"], errors)
        if "auto_scan" in profile:
            validate_auto_scan(f"{path}.auto_scan", profile["auto_scan"], errors)

    if isinstance(default_id, str) and default_id.strip() and default_id not in ids:
        errors.append(f"default_profile_id '{default_id}' does not match any profile.id.")

    return errors


def validate_ui_metadata(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(path, obj, {"group", "sort_order", "badges", "hidden"}, errors)
    if "group" in obj and not is_non_empty_string(obj["group"]):
        errors.append(f"{path}.group must be a non-empty string.")
    if "sort_order" in obj and not is_number(obj["sort_order"]):
        errors.append(f"{path}.sort_order must be a number.")
    if "hidden" in obj and not isinstance(obj["hidden"], bool):
        errors.append(f"{path}.hidden must be boolean.")
    if "badges" in obj:
        badges = obj["badges"]
        if not isinstance(badges, list):
            errors.append(f"{path}.badges must be an array.")
        else:
            for index, badge in enumerate(badges):
                if not is_non_empty_string(badge):
                    errors.append(f"{path}.badges[{index}] must be a non-empty string.")


def validate_backend_config(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(
        path,
        obj,
        {"profile_id", "supports_streaming", "supports_followup", "supports_followup_images", "notes"},
        errors
    )
    if not is_non_empty_string(obj.get("profile_id")):
        errors.append(f"{path}.profile_id must be a non-empty string.")
    for key in ("supports_streaming", "supports_followup", "supports_followup_images"):
        if key in obj and not isinstance(obj[key], bool):
            errors.append(f"{path}.{key} must be boolean.")
    if "notes" in obj and not is_non_empty_string(obj["notes"]):
        errors.append(f"{path}.notes must be a non-empty string.")


def validate_byok_config(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(
        path,
        obj,
        {
            "provider", "model_id", "family", "streaming_enabled", "system_prompt",
            "overview_prompt", "capabilities", "image", "token_policy",
            "parameter_overrides", "auto_scan"
        },
        errors
    )
    for key in ("provider", "model_id"):
        if not is_non_empty_string(obj.get(key)):
            errors.append(f"{path}.{key} must be a non-empty string.")
    if "family" in obj and not is_non_empty_string(obj["family"]):
        errors.append(f"{path}.family must be a non-empty string.")
    if "streaming_enabled" in obj and not isinstance(obj["streaming_enabled"], bool):
        errors.append(f"{path}.streaming_enabled must be boolean.")
    for key in ("system_prompt", "overview_prompt"):
        if key in obj and not isinstance(obj[key], str):
            errors.append(f"{path}.{key} must be a string.")
    if "capabilities" in obj:
        validate_capabilities(f"{path}.capabilities", obj["capabilities"], errors)
    if "image" in obj:
        validate_image(f"{path}.image", obj["image"], errors)
    if "token_policy" in obj:
        validate_token_policy(f"{path}.token_policy", obj["token_policy"], errors)
    if "parameter_overrides" in obj:
        validate_parameter_overrides(f"{path}.parameter_overrides", obj["parameter_overrides"], errors)
    if "auto_scan" in obj:
        validate_auto_scan(f"{path}.auto_scan", obj["auto_scan"], errors)


def validate_debug_config(path, obj, errors):
    if not isinstance(obj, dict):
        errors.append(f"{path} must be an object.")
        return
    validate_known_fields(path, obj, {"embedded_key_allowed"}, errors)
    if "embedded_key_allowed" in obj and not isinstance(obj["embedded_key_allowed"], bool):
        errors.append(f"{path}.embedded_key_allowed must be boolean.")


def validate_registry_profiles(data):
    errors = []
    if not isinstance(data, dict):
        errors.append("Root must be an object.")
        return errors

    validate_known_fields(
        "root",
        data,
        {"schema_version", "default_profile_id", "profiles"},
        errors
    )

    if data.get("schema_version") != REGISTRY_SCHEMA_VERSION:
        errors.append(f"schema_version must be '{REGISTRY_SCHEMA_VERSION}'.")

    default_id = data.get("default_profile_id")
    if not is_non_empty_string(default_id):
        errors.append("default_profile_id must be a non-empty string.")

    profiles = data.get("profiles")
    if not isinstance(profiles, list):
        errors.append("profiles must be an array.")
        return errors

    ids = set()
    valid_availability = {"backend", "byok", "both"}

    for index, profile in enumerate(profiles):
        path = f"profiles[{index}]"
        if not isinstance(profile, dict):
            errors.append(f"{path} must be an object.")
            continue
        validate_known_fields(
            path,
            profile,
            {"id", "label", "description", "availability", "ui", "backend", "byok", "debug"},
            errors
        )
        for key in ("id", "label", "description", "availability"):
            if not is_non_empty_string(profile.get(key)):
                errors.append(f"{path}.{key} must be a non-empty string.")
        profile_id = profile.get("id")
        if isinstance(profile_id, str):
            if profile_id in ids:
                errors.append(f"{path}.id '{profile_id}' is duplicated.")
            ids.add(profile_id)
        availability = profile.get("availability")
        if isinstance(availability, str) and availability not in valid_availability:
            errors.append(f"{path}.availability must be one of backend, byok, both.")

        if "ui" in profile:
            validate_ui_metadata(f"{path}.ui", profile["ui"], errors)
        if "debug" in profile:
            validate_debug_config(f"{path}.debug", profile["debug"], errors)

        backend = profile.get("backend")
        byok = profile.get("byok")
        if availability in ("backend", "both") and backend is None:
            errors.append(f"{path}.backend is required for availability '{availability}'.")
        if availability in ("byok", "both") and byok is None:
            errors.append(f"{path}.byok is required for availability '{availability}'.")
        if availability == "backend" and byok is not None:
            errors.append(f"{path}.byok must be omitted when availability is 'backend'.")
        if availability == "byok" and backend is not None:
            errors.append(f"{path}.backend must be omitted when availability is 'byok'.")

        if backend is not None:
            validate_backend_config(f"{path}.backend", backend, errors)
        if byok is not None:
            validate_byok_config(f"{path}.byok", byok, errors)

    if isinstance(default_id, str) and default_id.strip() and default_id not in ids:
        errors.append(f"default_profile_id '{default_id}' does not match any profile.id.")

    return errors


def detect_schema(data):
    if isinstance(data, dict) and data.get("schema_version") == REGISTRY_SCHEMA_VERSION:
        return "registry"
    return "legacy"


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

    schema_kind = detect_schema(data)
    errors = validate_registry_profiles(data) if schema_kind == "registry" else validate_legacy_profiles(data)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        print(f"Validation failed ({schema_kind}): {len(errors)} issue(s).", file=sys.stderr)
        return 1

    print(f"OK ({schema_kind}): {target_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
