/* VLM Profile Editor (no build tooling, no deps)
 * Works in Chrome. Save-back uses File System Access API when available; otherwise downloads JSON.
 */
"use strict";

(() => {
  const state = {
    data: null,
    fileHandle: null,
    fileName: null,
    currentIndex: -1,
    lastVoiceHintDismissed: false,
  };

  const $ = (id) => document.getElementById(id);

  const el = {
    openButton: $("openButton"),
    saveButton: $("saveButton"),
    downloadButton: $("downloadButton"),
    fileStatus: $("fileStatus"),
    profileSelect: $("profileSelect"),
    duplicateButton: $("duplicateButton"),
    deleteButton: $("deleteButton"),
    setDefaultButton: $("setDefaultButton"),
    validateButton: $("validateButton"),
    defaultProfileId: $("defaultProfileId"),

    // Defaults
    defaultProvider: $("defaultProvider"),
    defaultMaxTokens: $("defaultMaxTokens"),
    defaultImageMaxSidePx: $("defaultImageMaxSidePx"),
    defaultImageJpegQuality: $("defaultImageJpegQuality"),
    defaultSupportsVision: $("defaultSupportsVision"),
    defaultSupportsReasoning: $("defaultSupportsReasoning"),
    defaultSupportsJson: $("defaultSupportsJson"),
    defaultSystemPrompt: $("defaultSystemPrompt"),
    defaultOverviewPrompt: $("defaultOverviewPrompt"),

    validationSummary: $("validationSummary"),
    validationErrors: $("validationErrors"),

    profileFields: $("profileFields"),
    fileInput: $("fileInput"),

    // Profile basics
    fieldId: $("fieldId"),
    fieldLabel: $("fieldLabel"),
    fieldDescription: $("fieldDescription"),
    fieldProvider: $("fieldProvider"),
    fieldModelId: $("fieldModelId"),
    fieldFamily: $("fieldFamily"),

    // Profile toggles/capabilities
    fieldStreaming: $("fieldStreaming"),
    fieldSupportsReasoning: $("fieldSupportsReasoning"),
    fieldSupportsVision: $("fieldSupportsVision"),
    fieldSupportsJson: $("fieldSupportsJson"),

    // Policy + overrides
    fieldMaxTokens: $("fieldMaxTokens"),
    fieldTemperature: $("fieldTemperature"),
    fieldReasoningEffort: $("fieldReasoningEffort"),
    fieldReasoningExclude: $("fieldReasoningExclude"),
    fieldRetry1MaxTokens: $("fieldRetry1MaxTokens"),
    fieldRetry2MaxTokens: $("fieldRetry2MaxTokens"),
    fieldTopP: $("fieldTopP"),

    // Auto-scan
    fieldAutoScanEnabled: $("fieldAutoScanEnabled"), // maps to enabled_by_default
    fieldAutoScanInterval: $("fieldAutoScanInterval"),
    fieldAutoScanSpeakFree: $("fieldAutoScanSpeakFree"),

    // Prompts
    fieldSystemPrompt: $("fieldSystemPrompt"),
    fieldOverviewPrompt: $("fieldOverviewPrompt"),

    // Layout rows/wrappers
    capabilityRow: $("capabilityRow"),
    tokenPolicyRow: $("tokenPolicyRow"),
    autoScanRow: $("autoScanRow"),
    advancedSection: $("advancedSection"),
    advancedCapabilityRow: $("advancedCapabilityRow"),
    advancedTokenPolicyRow: $("advancedTokenPolicyRow"),
    advancedAutoScanRow: $("advancedAutoScanRow"),

    wrapStreaming: $("wrapStreaming"),
    wrapSupportsReasoning: $("wrapSupportsReasoning"),
    wrapSupportsVision: $("wrapSupportsVision"),
    wrapSupportsJson: $("wrapSupportsJson"),

    wrapMaxTokens: $("wrapMaxTokens"),
    wrapTemperature: $("wrapTemperature"),
    wrapReasoningEffort: $("wrapReasoningEffort"),
    wrapReasoningExclude: $("wrapReasoningExclude"),
    wrapRetry1MaxTokens: $("wrapRetry1MaxTokens"),
    wrapRetry2MaxTokens: $("wrapRetry2MaxTokens"),
    wrapTopP: $("wrapTopP"),

    wrapAutoScanEnabled: $("wrapAutoScanEnabled"),
    wrapAutoScanInterval: $("wrapAutoScanInterval"),
    wrapAutoScanSpeakFree: $("wrapAutoScanSpeakFree"),
  };

  const hasOwn = (obj, key) => Object.prototype.hasOwnProperty.call(obj, key);

  const setStatus = (message) => {
    el.fileStatus.textContent = message || "";
  };

  const setValidation = (errors) => {
    el.validationErrors.innerHTML = "";
    if (!errors || errors.length === 0) {
      el.validationSummary.textContent = "No validation errors.";
      el.validationSummary.classList.remove("warn");
      return;
    }
    el.validationSummary.textContent = `${errors.length} validation issue(s) found.`;
    el.validationSummary.classList.add("warn");
    errors.forEach((err) => {
      const li = document.createElement("li");
      li.textContent = err;
      el.validationErrors.appendChild(li);
    });
  };

  const getProfiles = () => (state.data && Array.isArray(state.data.profiles) ? state.data.profiles : []);

  const getCurrentProfile = () => {
    const profiles = getProfiles();
    if (state.currentIndex < 0 || state.currentIndex >= profiles.length) {
      return null;
    }
    return profiles[state.currentIndex] || null;
  };

  const ensureObject = (parent, key) => {
    if (!parent || typeof parent !== "object") {
      return null;
    }
    if (!hasOwn(parent, key) || !parent[key] || typeof parent[key] !== "object" || Array.isArray(parent[key])) {
      parent[key] = {};
    }
    return parent[key];
  };

  const cleanupEmptyObject = (parent, key) => {
    if (!parent || typeof parent !== "object" || !hasOwn(parent, key)) {
      return;
    }
    const value = parent[key];
    if (!value || typeof value !== "object" || Array.isArray(value)) {
      return;
    }
    if (Object.keys(value).length === 0) {
      delete parent[key];
    }
  };

  const setInputValue = (input, value) => {
    if (!input) return;
    input.value = value === undefined || value === null ? "" : String(value);
  };

  const setCheckboxValue = (input, value) => {
    if (!input) return;
    input.checked = Boolean(value);
  };

  const readOptionalNumber = (input) => {
    const raw = (input && input.value ? input.value : "").trim();
    if (raw === "") return { has: false, value: null };
    const n = Number(raw);
    if (!Number.isFinite(n)) return { has: true, value: null, invalid: true };
    return { has: true, value: n };
  };

  const readOptionalString = (input) => {
    const raw = input && typeof input.value === "string" ? input.value : "";
    if (raw.trim() === "") return { has: false, value: "" };
    return { has: true, value: raw };
  };

  const updateAdvancedVisibility = () => {
    const hasAdvanced =
      el.advancedCapabilityRow.children.length > 0 ||
      el.advancedTokenPolicyRow.children.length > 0 ||
      el.advancedAutoScanRow.children.length > 0;
    el.advancedSection.hidden = !hasAdvanced;
  };

  const placeField = (wrapper, row) => {
    if (!wrapper || !row) return;
    if (wrapper.parentElement !== row) {
      row.appendChild(wrapper);
    }
  };

  const placeOptionalFields = (profile) => {
    if (!profile) return;

    const caps = profile.capabilities || {};
    const policy = profile.token_policy || {};
    const overrides = profile.parameter_overrides || {};
    const autoScan = profile.auto_scan || {};

    // Auto-scan is active if profile.auto_scan exists OR any known key exists (incl. legacy 'enabled')
    const hasAutoScan =
      hasOwn(profile, "auto_scan") ||
      hasOwn(autoScan, "enabled_by_default") ||
      hasOwn(autoScan, "interval_ms") ||
      hasOwn(autoScan, "speak_free_every_ms") ||
      hasOwn(autoScan, "enabled");

    // Capabilities/toggles
    placeField(el.wrapStreaming, hasOwn(profile, "streaming_enabled") ? el.capabilityRow : el.advancedCapabilityRow);
    placeField(
      el.wrapSupportsReasoning,
      hasOwn(caps, "supports_reasoning") ? el.capabilityRow : el.advancedCapabilityRow
    );
    placeField(el.wrapSupportsVision, hasOwn(caps, "supports_vision") ? el.capabilityRow : el.advancedCapabilityRow);
    placeField(el.wrapSupportsJson, hasOwn(caps, "supports_json") ? el.capabilityRow : el.advancedCapabilityRow);

    // Token policy (core)
    placeField(el.wrapMaxTokens, el.tokenPolicyRow);
    placeField(el.wrapReasoningEffort, el.tokenPolicyRow);

    // Token policy (optional)
    placeField(
      el.wrapReasoningExclude,
      hasOwn(policy, "reasoning_exclude") ? el.tokenPolicyRow : el.advancedTokenPolicyRow
    );
    placeField(
      el.wrapRetry1MaxTokens,
      hasOwn(policy, "retry1_max_tokens") ? el.tokenPolicyRow : el.advancedTokenPolicyRow
    );
    placeField(
      el.wrapRetry2MaxTokens,
      hasOwn(policy, "retry2_max_tokens") ? el.tokenPolicyRow : el.advancedTokenPolicyRow
    );

    // Overrides
    placeField(el.wrapTemperature, hasOwn(overrides, "temperature") ? el.tokenPolicyRow : el.advancedTokenPolicyRow);
    placeField(el.wrapTopP, hasOwn(overrides, "top_p") ? el.tokenPolicyRow : el.advancedTokenPolicyRow);

    // Auto-scan
    placeField(el.wrapAutoScanEnabled, hasAutoScan ? el.autoScanRow : el.advancedAutoScanRow);
    placeField(el.wrapAutoScanInterval, hasAutoScan ? el.autoScanRow : el.advancedAutoScanRow);
    placeField(el.wrapAutoScanSpeakFree, hasAutoScan ? el.autoScanRow : el.advancedAutoScanRow);

    updateAdvancedVisibility();
  };

  const renderDefaults = () => {
    const defaults = state.data && state.data.defaults && typeof state.data.defaults === "object" ? state.data.defaults : {};
    setInputValue(el.defaultProvider, defaults.provider);
    const defPolicy = defaults.token_policy || {};
    setInputValue(el.defaultMaxTokens, defPolicy.max_tokens);

    const img = defaults.image || {};
    setInputValue(el.defaultImageMaxSidePx, img.max_side_px);
    setInputValue(el.defaultImageJpegQuality, img.jpeg_quality);

    const caps = defaults.capabilities || {};
    setCheckboxValue(el.defaultSupportsVision, caps.supports_vision);
    setCheckboxValue(el.defaultSupportsReasoning, caps.supports_reasoning);
    setCheckboxValue(el.defaultSupportsJson, caps.supports_json);

    setInputValue(el.defaultSystemPrompt, defaults.system_prompt);
    setInputValue(el.defaultOverviewPrompt, defaults.overview_prompt);
  };

  const renderProfile = (profile) => {
    if (!profile) return;

    setInputValue(el.fieldId, profile.id);
    setInputValue(el.fieldLabel, profile.label);
    setInputValue(el.fieldDescription, profile.description);
    setInputValue(el.fieldProvider, profile.provider);
    setInputValue(el.fieldModelId, profile.model_id);
    setInputValue(el.fieldFamily, profile.family);

    setCheckboxValue(el.fieldStreaming, profile.streaming_enabled);

    const caps = profile.capabilities || {};
    setCheckboxValue(el.fieldSupportsReasoning, caps.supports_reasoning);
    setCheckboxValue(el.fieldSupportsVision, caps.supports_vision);
    setCheckboxValue(el.fieldSupportsJson, caps.supports_json);

    const policy = profile.token_policy || {};
    const overrides = profile.parameter_overrides || {};
    const autoScan = profile.auto_scan || {};

    setInputValue(el.fieldMaxTokens, policy.max_tokens);
    setInputValue(el.fieldReasoningEffort, policy.reasoning_effort);
    setCheckboxValue(el.fieldReasoningExclude, policy.reasoning_exclude);
    setInputValue(el.fieldRetry1MaxTokens, policy.retry1_max_tokens);
    setInputValue(el.fieldRetry2MaxTokens, policy.retry2_max_tokens);

    setInputValue(el.fieldTemperature, overrides.temperature);
    setInputValue(el.fieldTopP, overrides.top_p);

    // Map editor checkbox to enabled_by_default; if legacy key exists, still reflect it.
    const enabledByDefault = hasOwn(autoScan, "enabled_by_default")
      ? autoScan.enabled_by_default
      : hasOwn(autoScan, "enabled")
        ? autoScan.enabled
        : undefined;
    setCheckboxValue(el.fieldAutoScanEnabled, enabledByDefault);
    setInputValue(el.fieldAutoScanInterval, autoScan.interval_ms);
    setInputValue(el.fieldAutoScanSpeakFree, autoScan.speak_free_every_ms);

    setInputValue(el.fieldSystemPrompt, profile.system_prompt);
    setInputValue(el.fieldOverviewPrompt, profile.overview_prompt);

    placeOptionalFields(profile);
    el.profileFields.disabled = false;
  };

  const clearProfileForm = () => {
    [
      el.fieldId, el.fieldLabel, el.fieldDescription, el.fieldProvider, el.fieldModelId, el.fieldFamily,
      el.fieldMaxTokens, el.fieldTemperature, el.fieldReasoningEffort, el.fieldRetry1MaxTokens, el.fieldRetry2MaxTokens,
      el.fieldTopP, el.fieldAutoScanInterval, el.fieldAutoScanSpeakFree, el.fieldSystemPrompt, el.fieldOverviewPrompt
    ].forEach((i) => setInputValue(i, ""));
    [
      el.fieldStreaming, el.fieldSupportsReasoning, el.fieldSupportsVision, el.fieldSupportsJson,
      el.fieldReasoningExclude, el.fieldAutoScanEnabled
    ].forEach((c) => setCheckboxValue(c, false));
    el.profileFields.disabled = true;
  };

  const updateActionState = () => {
    const loaded = !!state.data;
    const profile = getCurrentProfile();
    el.profileSelect.disabled = !loaded;
    el.duplicateButton.disabled = !profile;
    el.deleteButton.disabled = !profile;
    el.setDefaultButton.disabled = !profile;
    el.validateButton.disabled = !loaded;
    el.saveButton.disabled = !loaded;
    el.downloadButton.disabled = !loaded;
  };

  const refreshProfileSelect = (preserveIndex) => {
    const profiles = getProfiles();
    const defaultId = state.data ? state.data.default_profile_id : "";
    const prevIndex = state.currentIndex;

    el.profileSelect.innerHTML = "";
    profiles.forEach((profile, index) => {
      const option = document.createElement("option");
      const isDefault = profile && profile.id && profile.id === defaultId;
      const label = profile && profile.label ? profile.label : "(no label)";
      const id = profile && profile.id ? profile.id : "(no id)";
      option.value = String(index);
      option.textContent = `${label} (${id})${isDefault ? " (default)" : ""}`;
      el.profileSelect.appendChild(option);
    });

    if (preserveIndex && prevIndex >= 0 && prevIndex < profiles.length) {
      state.currentIndex = prevIndex;
    } else {
      state.currentIndex = profiles.length > 0 ? 0 : -1;
    }

    if (state.currentIndex >= 0) {
      el.profileSelect.value = String(state.currentIndex);
      renderProfile(getCurrentProfile());
    } else {
      clearProfileForm();
    }
    el.defaultProfileId.value = defaultId || "";
    renderDefaults();
    updateActionState();
  };

  const stableStringify = (value, indent = 2) => {
    const sort = (v) => {
      if (Array.isArray(v)) return v.map(sort);
      if (v && typeof v === "object") {
        const out = {};
        Object.keys(v).sort().forEach((k) => {
          out[k] = sort(v[k]);
        });
        return out;
      }
      return v;
    };
    return JSON.stringify(sort(value), null, indent) + "\n";
  };

  const downloadJson = (fileName, text) => {
    const blob = new Blob([text], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName || "vlm-profiles.json";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  };

  const validateData = (data) => {
    const errors = [];
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      errors.push("Root must be an object.");
      return errors;
    }

    // Root key sanity
    const allowedRoot = new Set(["defaults", "default_profile_id", "profiles"]);
    Object.keys(data).forEach((k) => {
      if (!allowedRoot.has(k)) {
        errors.push(`Unknown root key: '${k}'.`);
      }
    });

    if (typeof data.default_profile_id !== "string" || data.default_profile_id.trim() === "") {
      errors.push("default_profile_id must be a non-empty string.");
    }

    if (!Array.isArray(data.profiles)) {
      errors.push("profiles must be an array.");
      return errors;
    }

    // Defaults validation (optional)
    if (hasOwn(data, "defaults")) {
      if (!data.defaults || typeof data.defaults !== "object" || Array.isArray(data.defaults)) {
        errors.push("defaults must be an object.");
      } else {
        const d = data.defaults;
        if (hasOwn(d, "provider") && (typeof d.provider !== "string" || d.provider.trim() === "")) {
          errors.push("defaults.provider must be a non-empty string.");
        }
        if (hasOwn(d, "system_prompt") && typeof d.system_prompt !== "string") {
          errors.push("defaults.system_prompt must be a string.");
        }
        if (hasOwn(d, "overview_prompt") && typeof d.overview_prompt !== "string") {
          errors.push("defaults.overview_prompt must be a string.");
        }
        if (hasOwn(d, "image")) {
          if (!d.image || typeof d.image !== "object" || Array.isArray(d.image)) {
            errors.push("defaults.image must be an object.");
          } else {
            ["max_side_px", "jpeg_quality"].forEach((k) => {
              if (hasOwn(d.image, k)) {
                const v = d.image[k];
                if (typeof v !== "number" || !Number.isFinite(v)) errors.push(`defaults.image.${k} must be a number.`);
              }
            });
          }
        }
        if (hasOwn(d, "token_policy")) {
          if (!d.token_policy || typeof d.token_policy !== "object" || Array.isArray(d.token_policy)) {
            errors.push("defaults.token_policy must be an object.");
          } else if (hasOwn(d.token_policy, "max_tokens")) {
            const v = d.token_policy.max_tokens;
            if (typeof v !== "number" || !Number.isFinite(v) || v <= 0) {
              errors.push("defaults.token_policy.max_tokens must be a number > 0.");
            }
          }
        }
        if (hasOwn(d, "capabilities")) {
          if (!d.capabilities || typeof d.capabilities !== "object" || Array.isArray(d.capabilities)) {
            errors.push("defaults.capabilities must be an object.");
          } else {
            ["supports_reasoning", "supports_vision", "supports_json"].forEach((k) => {
              if (hasOwn(d.capabilities, k) && typeof d.capabilities[k] !== "boolean") {
                errors.push(`defaults.capabilities.${k} must be boolean.`);
              }
            });
          }
        }
      }
    }

    const ids = new Set();

    data.profiles.forEach((profile, index) => {
      const path = `profiles[${index}]`;
      if (!profile || typeof profile !== "object" || Array.isArray(profile)) {
        errors.push(`${path} must be an object.`);
        return;
      }

      ["id", "label", "description", "model_id"].forEach((k) => {
        if (typeof profile[k] !== "string" || profile[k].trim() === "") {
          errors.push(`${path}.${k} must be a non-empty string.`);
        }
      });

      if (typeof profile.id === "string") {
        if (ids.has(profile.id)) errors.push(`${path}.id '${profile.id}' is duplicated.`);
        ids.add(profile.id);
      }

      if (hasOwn(profile, "family") && (typeof profile.family !== "string" || profile.family.trim() === "")) {
        errors.push(`${path}.family must be a non-empty string.`);
      }

      if (hasOwn(profile, "provider") && (typeof profile.provider !== "string" || profile.provider.trim() === "")) {
        errors.push(`${path}.provider must be a non-empty string.`);
      }

      if (hasOwn(profile, "streaming_enabled") && typeof profile.streaming_enabled !== "boolean") {
        errors.push(`${path}.streaming_enabled must be boolean.`);
      }

      if (hasOwn(profile, "system_prompt") && typeof profile.system_prompt !== "string") {
        errors.push(`${path}.system_prompt must be a string.`);
      }
      if (hasOwn(profile, "overview_prompt") && typeof profile.overview_prompt !== "string") {
        errors.push(`${path}.overview_prompt must be a string.`);
      }

      if (hasOwn(profile, "capabilities")) {
        if (!profile.capabilities || typeof profile.capabilities !== "object" || Array.isArray(profile.capabilities)) {
          errors.push(`${path}.capabilities must be an object.`);
        } else {
          ["supports_reasoning", "supports_vision", "supports_json"].forEach((k) => {
            if (hasOwn(profile.capabilities, k) && typeof profile.capabilities[k] !== "boolean") {
              errors.push(`${path}.capabilities.${k} must be boolean.`);
            }
          });
        }
      }

      if (hasOwn(profile, "parameter_overrides")) {
        if (
          !profile.parameter_overrides ||
          typeof profile.parameter_overrides !== "object" ||
          Array.isArray(profile.parameter_overrides)
        ) {
          errors.push(`${path}.parameter_overrides must be an object.`);
        } else {
          ["temperature", "top_p"].forEach((k) => {
            if (hasOwn(profile.parameter_overrides, k)) {
              const v = profile.parameter_overrides[k];
              if (typeof v !== "number" || !Number.isFinite(v)) errors.push(`${path}.parameter_overrides.${k} must be a number.`);
            }
          });
        }
      }

      if (hasOwn(profile, "token_policy")) {
        if (!profile.token_policy || typeof profile.token_policy !== "object" || Array.isArray(profile.token_policy)) {
          errors.push(`${path}.token_policy must be an object.`);
        } else {
          const tp = profile.token_policy;
          const checkPosNum = (k) => {
            if (!hasOwn(tp, k)) return;
            const v = tp[k];
            if (typeof v !== "number" || !Number.isFinite(v) || v <= 0) errors.push(`${path}.token_policy.${k} must be a number > 0.`);
          };
          checkPosNum("max_tokens");
          checkPosNum("retry1_max_tokens");
          checkPosNum("retry2_max_tokens");

          if (hasOwn(tp, "reasoning_effort")) {
            const v = tp.reasoning_effort;
            if (typeof v !== "string" || v.trim() === "") errors.push(`${path}.token_policy.reasoning_effort must be a non-empty string.`);
          }
          if (hasOwn(tp, "reasoning_exclude") && typeof tp.reasoning_exclude !== "boolean") {
            errors.push(`${path}.token_policy.reasoning_exclude must be boolean.`);
          }

          // basic ordering hint
          if (typeof tp.max_tokens === "number" && typeof tp.retry1_max_tokens === "number" && tp.retry1_max_tokens < tp.max_tokens) {
            errors.push(`${path}.token_policy.retry1_max_tokens should be >= max_tokens.`);
          }
          if (typeof tp.retry1_max_tokens === "number" && typeof tp.retry2_max_tokens === "number" && tp.retry2_max_tokens < tp.retry1_max_tokens) {
            errors.push(`${path}.token_policy.retry2_max_tokens should be >= retry1_max_tokens.`);
          }
        }
      }

      if (hasOwn(profile, "auto_scan")) {
        if (!profile.auto_scan || typeof profile.auto_scan !== "object" || Array.isArray(profile.auto_scan)) {
          errors.push(`${path}.auto_scan must be an object.`);
        } else {
          const as = profile.auto_scan;

          // Flag legacy key explicitly (this was the earlier bug).
          if (hasOwn(as, "enabled")) {
            errors.push(`${path}.auto_scan.enabled is not supported; use enabled_by_default.`);
          }

          if (hasOwn(as, "enabled_by_default") && typeof as.enabled_by_default !== "boolean") {
            errors.push(`${path}.auto_scan.enabled_by_default must be boolean.`);
          }
          const checkMin = (k, min) => {
            if (!hasOwn(as, k)) return;
            const v = as[k];
            if (typeof v !== "number" || !Number.isFinite(v)) errors.push(`${path}.auto_scan.${k} must be a number.`);
            else if (v < min) errors.push(`${path}.auto_scan.${k} must be >= ${min}.`);
          };
          checkMin("interval_ms", 1000);
          checkMin("speak_free_every_ms", 0);
        }
      }
    });

    if (typeof data.default_profile_id === "string" && data.default_profile_id.trim() !== "") {
      if (!ids.has(data.default_profile_id)) {
        errors.push(`default_profile_id '${data.default_profile_id}' does not match any profile.id.`);
      }
    }

    return errors;
  };

  const updateValidation = () => {
    if (!state.data) {
      setValidation([]);
      return;
    }
    setValidation(validateData(state.data));
  };

  const generateCopyId = (baseId) => {
    const profiles = getProfiles();
    const existing = new Set(profiles.map((p) => (p && p.id ? p.id : "")));
    let idx = 1;
    let candidate = `${baseId}-copy-${idx}`;
    while (existing.has(candidate)) {
      idx += 1;
      candidate = `${baseId}-copy-${idx}`;
    }
    return candidate;
  };

  const loadJsonText = (text, fileName, fileHandle) => {
    let parsed;
    try {
      parsed = JSON.parse(text);
    } catch {
      setStatus("Failed to parse JSON.");
      return;
    }
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      setStatus("JSON root must be an object.");
      return;
    }
    if (!Array.isArray(parsed.profiles)) parsed.profiles = [];
    state.data = parsed;
    state.fileHandle = fileHandle || null;
    state.fileName = fileName || "vlm-profiles.json";

    refreshProfileSelect(false);
    updateValidation();

    setStatus(`${state.fileName} loaded (${getProfiles().length} profiles)`);
  };

  const openFileViaPicker = async () => {
    if (!window.showOpenFilePicker) return false;
    try {
      const [handle] = await window.showOpenFilePicker({
        types: [{ description: "JSON", accept: { "application/json": [".json"] } }],
        multiple: false,
      });
      const file = await handle.getFile();
      const text = await file.text();
      loadJsonText(text, file.name, handle);
      return true;
    } catch {
      return false;
    }
  };

  const openFileViaInput = () => {
    el.fileInput.value = "";
    el.fileInput.click();
  };

  const openFile = async () => {
    // File System Access API requires secure context; fallback to file input.
    const ok = await openFileViaPicker();
    if (!ok) openFileViaInput();
  };

  const saveToHandle = async (text) => {
    if (!state.fileHandle || !state.fileHandle.createWritable) return false;
    try {
      const writable = await state.fileHandle.createWritable();
      await writable.write(text);
      await writable.close();
      setStatus(`Saved ${state.fileName}`);
      return true;
    } catch {
      return false;
    }
  };

  const getOutputJsonText = () => stableStringify(state.data);

  // ---------- Defaults input handlers ----------
  const onDefaultsChanged = () => {
    if (!state.data) return;
    const d = ensureObject(state.data, "defaults");

    // provider
    const prov = readOptionalString(el.defaultProvider);
    if (prov.has) d.provider = prov.value;
    else delete d.provider;

    // prompts
    const sp = readOptionalString(el.defaultSystemPrompt);
    if (sp.has) d.system_prompt = sp.value;
    else delete d.system_prompt;

    const op = readOptionalString(el.defaultOverviewPrompt);
    if (op.has) d.overview_prompt = op.value;
    else delete d.overview_prompt;

    // image
    const img = ensureObject(d, "image");
    const mx = readOptionalNumber(el.defaultImageMaxSidePx);
    if (mx.has && !mx.invalid) img.max_side_px = mx.value;
    else delete img.max_side_px;

    const jq = readOptionalNumber(el.defaultImageJpegQuality);
    if (jq.has && !jq.invalid) img.jpeg_quality = jq.value;
    else delete img.jpeg_quality;
    cleanupEmptyObject(d, "image");

    // token_policy.max_tokens
    const tp = ensureObject(d, "token_policy");
    const mt = readOptionalNumber(el.defaultMaxTokens);
    if (mt.has && !mt.invalid) tp.max_tokens = mt.value;
    else delete tp.max_tokens;
    cleanupEmptyObject(d, "token_policy");

    // capabilities
    const caps = ensureObject(d, "capabilities");
    const setCap = (key, input) => {
      if (input.checked) caps[key] = true;
      else delete caps[key];
    };
    setCap("supports_vision", el.defaultSupportsVision);
    setCap("supports_reasoning", el.defaultSupportsReasoning);
    setCap("supports_json", el.defaultSupportsJson);
    cleanupEmptyObject(d, "capabilities");

    cleanupEmptyObject(state.data, "defaults");
    updateValidation();
  };

  // ---------- Profile input handlers ----------
  const onProfileBasicsChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    const idv = readOptionalString(el.fieldId);
    if (idv.has) profile.id = idv.value; // id required; keep if present
    const lb = readOptionalString(el.fieldLabel);
    if (lb.has) profile.label = lb.value;
    const desc = readOptionalString(el.fieldDescription);
    if (desc.has) profile.description = desc.value;

    const prov = readOptionalString(el.fieldProvider);
    if (prov.has) profile.provider = prov.value;
    else delete profile.provider;

    const mid = readOptionalString(el.fieldModelId);
    if (mid.has) profile.model_id = mid.value;

    const fam = readOptionalString(el.fieldFamily);
    if (fam.has) profile.family = fam.value;
    else delete profile.family;

    refreshProfileSelect(true);
    updateValidation();
  };

  const onProfileTogglesChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    if (el.fieldStreaming.checked) profile.streaming_enabled = true;
    else delete profile.streaming_enabled;

    const caps = ensureObject(profile, "capabilities");
    const setCap = (key, input) => {
      if (input.checked) caps[key] = true;
      else delete caps[key];
    };
    setCap("supports_reasoning", el.fieldSupportsReasoning);
    setCap("supports_vision", el.fieldSupportsVision);
    setCap("supports_json", el.fieldSupportsJson);
    cleanupEmptyObject(profile, "capabilities");

    placeOptionalFields(profile);
    updateValidation();
  };

  const onPolicyChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    const tp = ensureObject(profile, "token_policy");

    const maxT = readOptionalNumber(el.fieldMaxTokens);
    if (maxT.has && !maxT.invalid) tp.max_tokens = maxT.value;
    else delete tp.max_tokens;

    const re = readOptionalString(el.fieldReasoningEffort);
    if (re.has) tp.reasoning_effort = re.value;
    else delete tp.reasoning_effort;

    // reasoning_exclude: only store if true
    if (el.fieldReasoningExclude.checked) tp.reasoning_exclude = true;
    else delete tp.reasoning_exclude;

    const r1 = readOptionalNumber(el.fieldRetry1MaxTokens);
    if (r1.has && !r1.invalid) tp.retry1_max_tokens = r1.value;
    else delete tp.retry1_max_tokens;

    const r2 = readOptionalNumber(el.fieldRetry2MaxTokens);
    if (r2.has && !r2.invalid) tp.retry2_max_tokens = r2.value;
    else delete tp.retry2_max_tokens;

    cleanupEmptyObject(profile, "token_policy");
    placeOptionalFields(profile);
    updateValidation();
  };

  const onOverridesChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    const overrides = ensureObject(profile, "parameter_overrides");

    const t = readOptionalNumber(el.fieldTemperature);
    if (t.has && !t.invalid) overrides.temperature = t.value;
    else delete overrides.temperature;

    const tp = readOptionalNumber(el.fieldTopP);
    if (tp.has && !tp.invalid) overrides.top_p = tp.value;
    else delete overrides.top_p;

    cleanupEmptyObject(profile, "parameter_overrides");
    placeOptionalFields(profile);
    updateValidation();
  };

  const onAutoScanChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    const as = ensureObject(profile, "auto_scan");

    // Remove legacy key if present
    if (hasOwn(as, "enabled")) delete as.enabled;

    // keep explicit bool if auto_scan exists
    as.enabled_by_default = Boolean(el.fieldAutoScanEnabled.checked);

    const iv = readOptionalNumber(el.fieldAutoScanInterval);
    if (iv.has && !iv.invalid) as.interval_ms = iv.value;
    else delete as.interval_ms;

    const sf = readOptionalNumber(el.fieldAutoScanSpeakFree);
    if (sf.has && !sf.invalid) as.speak_free_every_ms = sf.value;
    else delete as.speak_free_every_ms;

    cleanupEmptyObject(profile, "auto_scan");
    placeOptionalFields(profile);
    updateValidation();
  };

  const onPromptsChanged = () => {
    const profile = getCurrentProfile();
    if (!profile) return;

    const sp = readOptionalString(el.fieldSystemPrompt);
    if (sp.has) profile.system_prompt = sp.value;
    else delete profile.system_prompt;

    const op = readOptionalString(el.fieldOverviewPrompt);
    if (op.has) profile.overview_prompt = op.value;
    else delete profile.overview_prompt;

    updateValidation();
  };

  // ---------- Wire UI ----------
  el.openButton.addEventListener("click", () => openFile());

  el.fileInput.addEventListener("change", async (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    const text = await file.text();
    loadJsonText(text, file.name, null);
  });

  el.profileSelect.addEventListener("change", () => {
    const index = Number(el.profileSelect.value);
    state.currentIndex = Number.isFinite(index) ? index : -1;
    const profile = getCurrentProfile();
    if (profile) renderProfile(profile);
    else clearProfileForm();
    updateActionState();
  });

  el.duplicateButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    if (!profile || !state.data) return;
    const copy = JSON.parse(JSON.stringify(profile));
    copy.id = generateCopyId(profile.id || "profile");
    state.data.profiles.push(copy);
    refreshProfileSelect(false);
    updateValidation();
  });

  el.deleteButton.addEventListener("click", () => {
    if (!state.data) return;
    const profile = getCurrentProfile();
    if (!profile) return;
    const ok = window.confirm(`Delete profile '${profile.id || "(unknown)"}'?`);
    if (!ok) return;
    state.data.profiles.splice(state.currentIndex, 1);
    refreshProfileSelect(false);
    updateValidation();
  });

  el.setDefaultButton.addEventListener("click", () => {
    if (!state.data) return;
    const profile = getCurrentProfile();
    if (!profile || typeof profile.id !== "string" || profile.id.trim() === "") return;
    state.data.default_profile_id = profile.id;
    refreshProfileSelect(true);
    updateValidation();
  });

  el.validateButton.addEventListener("click", () => updateValidation());

  el.saveButton.addEventListener("click", async () => {
    if (!state.data) return;
    const text = getOutputJsonText();
    const saved = await saveToHandle(text);
    if (!saved) {
      downloadJson(state.fileName || "vlm-profiles.json", text);
      setStatus("Downloaded JSON (Save-back not available in this context).");
    }
  });

  el.downloadButton.addEventListener("click", () => {
    if (!state.data) return;
    downloadJson(state.fileName || "vlm-profiles.json", getOutputJsonText());
  });

  // Defaults listeners
  [
    el.defaultProvider, el.defaultMaxTokens, el.defaultImageMaxSidePx, el.defaultImageJpegQuality,
    el.defaultSystemPrompt, el.defaultOverviewPrompt
  ].forEach((input) => input.addEventListener("input", onDefaultsChanged));
  [el.defaultSupportsVision, el.defaultSupportsReasoning, el.defaultSupportsJson].forEach((c) =>
    c.addEventListener("change", onDefaultsChanged)
  );

  // Profile listeners
  [el.fieldId, el.fieldLabel, el.fieldDescription, el.fieldProvider, el.fieldModelId, el.fieldFamily].forEach((i) =>
    i.addEventListener("input", onProfileBasicsChanged)
  );

  [el.fieldStreaming, el.fieldSupportsReasoning, el.fieldSupportsVision, el.fieldSupportsJson].forEach((c) =>
    c.addEventListener("change", onProfileTogglesChanged)
  );

  [el.fieldMaxTokens, el.fieldReasoningEffort, el.fieldRetry1MaxTokens, el.fieldRetry2MaxTokens].forEach((i) =>
    i.addEventListener("input", onPolicyChanged)
  );
  el.fieldReasoningExclude.addEventListener("change", onPolicyChanged);

  [el.fieldTemperature, el.fieldTopP].forEach((i) => i.addEventListener("input", onOverridesChanged));

  el.fieldAutoScanEnabled.addEventListener("change", onAutoScanChanged);
  [el.fieldAutoScanInterval, el.fieldAutoScanSpeakFree].forEach((i) => i.addEventListener("input", onAutoScanChanged));

  [el.fieldSystemPrompt, el.fieldOverviewPrompt].forEach((i) => i.addEventListener("input", onPromptsChanged));

  // Initial UI state
  updateActionState();
  clearProfileForm();
  setValidation([]);
  setStatus("Open a vlm-profiles.json to begin.");
})();
