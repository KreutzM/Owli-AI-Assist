"use strict";

(() => {
  const state = {
    data: null,
    fileHandle: null,
    fileName: null,
    currentIndex: -1,
  };

  const el = {
    openButton: document.getElementById("openButton"),
    saveButton: document.getElementById("saveButton"),
    downloadButton: document.getElementById("downloadButton"),
    fileStatus: document.getElementById("fileStatus"),
    profileSelect: document.getElementById("profileSelect"),
    duplicateButton: document.getElementById("duplicateButton"),
    deleteButton: document.getElementById("deleteButton"),
    setDefaultButton: document.getElementById("setDefaultButton"),
    validateButton: document.getElementById("validateButton"),
    defaultProfileId: document.getElementById("defaultProfileId"),
    defaultProvider: document.getElementById("defaultProvider"),
    validationSummary: document.getElementById("validationSummary"),
    validationErrors: document.getElementById("validationErrors"),
    profileFields: document.getElementById("profileFields"),
    fileInput: document.getElementById("fileInput"),
    fieldId: document.getElementById("fieldId"),
    fieldLabel: document.getElementById("fieldLabel"),
    fieldDescription: document.getElementById("fieldDescription"),
    fieldProvider: document.getElementById("fieldProvider"),
    fieldModelId: document.getElementById("fieldModelId"),
    fieldStreaming: document.getElementById("fieldStreaming"),
    fieldSupportsReasoning: document.getElementById("fieldSupportsReasoning"),
    fieldSupportsVision: document.getElementById("fieldSupportsVision"),
    fieldSupportsJson: document.getElementById("fieldSupportsJson"),
    fieldMaxTokens: document.getElementById("fieldMaxTokens"),
    fieldTemperature: document.getElementById("fieldTemperature"),
    fieldReasoningEffort: document.getElementById("fieldReasoningEffort"),
    fieldTopP: document.getElementById("fieldTopP"),
    fieldAutoScanEnabled: document.getElementById("fieldAutoScanEnabled"),
    fieldAutoScanInterval: document.getElementById("fieldAutoScanInterval"),
    fieldSystemPrompt: document.getElementById("fieldSystemPrompt"),
    fieldOverviewPrompt: document.getElementById("fieldOverviewPrompt"),
    capabilityRow: document.getElementById("capabilityRow"),
    tokenPolicyRow: document.getElementById("tokenPolicyRow"),
    autoScanRow: document.getElementById("autoScanRow"),
    advancedSection: document.getElementById("advancedSection"),
    advancedCapabilityRow: document.getElementById("advancedCapabilityRow"),
    advancedTokenPolicyRow: document.getElementById("advancedTokenPolicyRow"),
    advancedAutoScanRow: document.getElementById("advancedAutoScanRow"),
    wrapStreaming: document.getElementById("wrapStreaming"),
    wrapSupportsReasoning: document.getElementById("wrapSupportsReasoning"),
    wrapSupportsVision: document.getElementById("wrapSupportsVision"),
    wrapSupportsJson: document.getElementById("wrapSupportsJson"),
    wrapMaxTokens: document.getElementById("wrapMaxTokens"),
    wrapTemperature: document.getElementById("wrapTemperature"),
    wrapReasoningEffort: document.getElementById("wrapReasoningEffort"),
    wrapTopP: document.getElementById("wrapTopP"),
    wrapAutoScanEnabled: document.getElementById("wrapAutoScanEnabled"),
    wrapAutoScanInterval: document.getElementById("wrapAutoScanInterval"),
  };

  const setStatus = (text) => {
    el.fileStatus.textContent = text;
  };

  const setValidation = (messages) => {
    el.validationErrors.innerHTML = "";
    if (!messages || messages.length === 0) {
      el.validationSummary.textContent = "Validation OK";
      el.validationSummary.className = "status ok";
      return;
    }

    el.validationSummary.textContent = `${messages.length} validation issue(s)`;
    el.validationSummary.className = "status error";
    messages.forEach((message) => {
      const item = document.createElement("li");
      item.textContent = message;
      el.validationErrors.append(item);
    });
  };

  const hasOwn = (obj, key) => {
    return Object.prototype.hasOwnProperty.call(obj || {}, key);
  };

  const getProfiles = () => {
    if (!state.data || !Array.isArray(state.data.profiles)) {
      return [];
    }
    return state.data.profiles;
  };

  const getCurrentProfile = () => {
    const profiles = getProfiles();
    if (state.currentIndex < 0 || state.currentIndex >= profiles.length) {
      return null;
    }
    return profiles[state.currentIndex];
  };

  const getDefaultProvider = () => {
    return (state.data && state.data.defaults && state.data.defaults.provider) || "";
  };

  const getProfileLabel = (profile) => {
    const label = profile.label || profile.id || "(missing)";
    const suffix = profile.id ? ` (${profile.id})` : "";
    return `${label}${suffix}`;
  };

  const updateDefaultIndicators = () => {
    const profiles = getProfiles();
    const defaultId = state.data ? state.data.default_profile_id : "";
    const options = Array.from(el.profileSelect.options);
    options.forEach((option, index) => {
      const profile = profiles[index];
      if (!profile) {
        return;
      }
      const isDefault = profile.id && profile.id === defaultId;
      option.textContent = `${getProfileLabel(profile)}${isDefault ? " (default)" : ""}`;
    });
  };

  const refreshProfileSelect = (preserveIndex) => {
    const profiles = getProfiles();
    const defaultId = state.data ? state.data.default_profile_id : "";
    el.profileSelect.innerHTML = "";

    profiles.forEach((profile, index) => {
      const option = document.createElement("option");
      const isDefault = profile.id && profile.id === defaultId;
      option.value = String(index);
      option.textContent = `${getProfileLabel(profile)}${isDefault ? " (default)" : ""}`;
      el.profileSelect.append(option);
    });

    if (profiles.length === 0) {
      state.currentIndex = -1;
      el.profileSelect.disabled = true;
      clearProfileForm();
      updateActionState();
      return;
    }

    el.profileSelect.disabled = false;
    if (!preserveIndex) {
      const defaultIndex = profiles.findIndex((profile) => profile.id === defaultId);
      state.currentIndex = defaultIndex >= 0 ? defaultIndex : 0;
    }

    if (state.currentIndex < 0 || state.currentIndex >= profiles.length) {
      state.currentIndex = 0;
    }

    el.profileSelect.value = String(state.currentIndex);
    renderProfile(profiles[state.currentIndex]);
    updateActionState();
  };

  const setInputValue = (input, value) => {
    input.value = value ?? "";
  };

  const setCheckboxValue = (input, value) => {
    input.checked = Boolean(value);
  };

  const placeField = (wrapper, target) => {
    if (!wrapper || !target) {
      return;
    }
    if (wrapper.parentNode !== target) {
      target.appendChild(wrapper);
    }
  };

  const updateAdvancedVisibility = () => {
    const hasAdvanced =
      el.advancedCapabilityRow.children.length > 0 ||
      el.advancedTokenPolicyRow.children.length > 0 ||
      el.advancedAutoScanRow.children.length > 0;
    el.advancedSection.style.display = hasAdvanced ? "block" : "none";
    if (!hasAdvanced) {
      el.advancedSection.open = false;
    }
  };

  const placeOptionalFields = (profile) => {
    if (!profile) {
      return;
    }
    const capabilities = profile.capabilities || {};
    const overrides = profile.parameter_overrides || {};
    const autoScan = profile.auto_scan || {};
    const hasAutoScan =
      hasOwn(profile, "auto_scan") ||
      hasOwn(autoScan, "enabled") ||
      hasOwn(autoScan, "interval_ms");

    placeField(
      el.wrapStreaming,
      hasOwn(profile, "streaming_enabled") ? el.capabilityRow : el.advancedCapabilityRow
    );
    placeField(
      el.wrapSupportsReasoning,
      hasOwn(capabilities, "supports_reasoning")
        ? el.capabilityRow
        : el.advancedCapabilityRow
    );
    placeField(
      el.wrapSupportsVision,
      hasOwn(capabilities, "supports_vision") ? el.capabilityRow : el.advancedCapabilityRow
    );
    placeField(
      el.wrapSupportsJson,
      hasOwn(capabilities, "supports_json") ? el.capabilityRow : el.advancedCapabilityRow
    );

    placeField(el.wrapMaxTokens, el.tokenPolicyRow);
    placeField(
      el.wrapTemperature,
      hasOwn(overrides, "temperature") ? el.tokenPolicyRow : el.advancedTokenPolicyRow
    );
    placeField(el.wrapReasoningEffort, el.tokenPolicyRow);
    placeField(
      el.wrapTopP,
      hasOwn(overrides, "top_p") ? el.tokenPolicyRow : el.advancedTokenPolicyRow
    );

    placeField(
      el.wrapAutoScanEnabled,
      hasAutoScan ? el.autoScanRow : el.advancedAutoScanRow
    );
    placeField(
      el.wrapAutoScanInterval,
      hasAutoScan ? el.autoScanRow : el.advancedAutoScanRow
    );

    updateAdvancedVisibility();
  };

  const renderProfile = (profile) => {
    if (!profile) {
      return;
    }
    setInputValue(el.fieldId, profile.id);
    setInputValue(el.fieldLabel, profile.label);
    setInputValue(el.fieldDescription, profile.description);
    setInputValue(el.fieldProvider, profile.provider);
    setInputValue(el.fieldModelId, profile.model_id);

    setCheckboxValue(el.fieldStreaming, profile.streaming_enabled);
    setCheckboxValue(
      el.fieldSupportsReasoning,
      profile.capabilities && profile.capabilities.supports_reasoning
    );
    setCheckboxValue(
      el.fieldSupportsVision,
      profile.capabilities && profile.capabilities.supports_vision
    );
    setCheckboxValue(
      el.fieldSupportsJson,
      profile.capabilities && profile.capabilities.supports_json
    );

    const tokenPolicy = profile.token_policy || {};
    const overrides = profile.parameter_overrides || {};
    const autoScan = profile.auto_scan || {};

    setInputValue(el.fieldMaxTokens, tokenPolicy.max_tokens);
    setInputValue(el.fieldReasoningEffort, tokenPolicy.reasoning_effort);
    setInputValue(el.fieldTemperature, overrides.temperature);
    setInputValue(el.fieldTopP, overrides.top_p);

    setCheckboxValue(el.fieldAutoScanEnabled, autoScan.enabled);
    setInputValue(el.fieldAutoScanInterval, autoScan.interval_ms);

    setInputValue(el.fieldSystemPrompt, profile.system_prompt);
    setInputValue(el.fieldOverviewPrompt, profile.overview_prompt);
    placeOptionalFields(profile);
    el.profileFields.disabled = false;
  };

  const clearProfileForm = () => {
    setInputValue(el.fieldId, "");
    setInputValue(el.fieldLabel, "");
    setInputValue(el.fieldDescription, "");
    setInputValue(el.fieldProvider, "");
    setInputValue(el.fieldModelId, "");
    setCheckboxValue(el.fieldStreaming, false);
    setCheckboxValue(el.fieldSupportsReasoning, false);
    setCheckboxValue(el.fieldSupportsVision, false);
    setCheckboxValue(el.fieldSupportsJson, false);
    setInputValue(el.fieldMaxTokens, "");
    setInputValue(el.fieldTemperature, "");
    setInputValue(el.fieldReasoningEffort, "");
    setInputValue(el.fieldTopP, "");
    setCheckboxValue(el.fieldAutoScanEnabled, false);
    setInputValue(el.fieldAutoScanInterval, "");
    setInputValue(el.fieldSystemPrompt, "");
    setInputValue(el.fieldOverviewPrompt, "");
    el.profileFields.disabled = true;
  };

  const updateActionState = () => {
    const hasData = Boolean(state.data);
    const profiles = getProfiles();
    const hasProfile = profiles.length > 0 && state.currentIndex >= 0;

    el.profileSelect.disabled = !hasProfile;
    el.duplicateButton.disabled = !hasProfile;
    el.deleteButton.disabled = !hasProfile;
    el.setDefaultButton.disabled = !hasProfile;
    el.validateButton.disabled = !hasData;
    el.downloadButton.disabled = !hasData;
    el.saveButton.disabled = !hasData;
    if (!hasProfile) {
      el.profileFields.disabled = true;
    }
  };

  const ensureObject = (profile, key) => {
    if (!profile[key] || typeof profile[key] !== "object" || Array.isArray(profile[key])) {
      profile[key] = {};
    }
    return profile[key];
  };

  const cleanupEmptyObject = (profile, key) => {
    const value = profile[key];
    if (value && typeof value === "object" && !Array.isArray(value)) {
      if (Object.keys(value).length === 0) {
        delete profile[key];
      }
    }
  };

  const updateOptionalNumber = (target, key, input) => {
    const raw = input.value.trim();
    if (raw === "") {
      delete target[key];
      return;
    }
    const numberValue = Number(raw);
    if (Number.isFinite(numberValue)) {
      target[key] = numberValue;
    }
  };

  const updateOptionalString = (profile, key, input) => {
    const raw = input.value;
    if (raw.trim() === "") {
      delete profile[key];
      return;
    }
    profile[key] = raw;
  };

  const updateValidation = () => {
    if (!state.data) {
      setValidation([]);
      return;
    }
    setValidation(validateData(state.data));
  };

  const validateData = (data) => {
    const errors = [];
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      errors.push("Root must be an object.");
      return errors;
    }

    if (typeof data.default_profile_id !== "string") {
      errors.push("default_profile_id must be a string.");
    } else if (data.default_profile_id.trim() === "") {
      errors.push("default_profile_id must not be empty.");
    }

    if (!Array.isArray(data.profiles)) {
      errors.push("profiles must be an array.");
      return errors;
    }

    const ids = new Set();

    data.profiles.forEach((profile, index) => {
      const path = `profiles[${index}]`;
      if (!profile || typeof profile !== "object" || Array.isArray(profile)) {
        errors.push(`${path} must be an object.`);
        return;
      }

      ["id", "label", "description", "model_id"].forEach((key) => {
        if (typeof profile[key] !== "string") {
          errors.push(`${path}.${key} must be a string.`);
        } else if (profile[key].trim() === "") {
          errors.push(`${path}.${key} must not be empty.`);
        }
      });

      if (typeof profile.id === "string") {
        if (ids.has(profile.id)) {
          errors.push(`${path}.id '${profile.id}' is duplicated.`);
        } else {
          ids.add(profile.id);
        }
      }

      if (hasOwn(profile, "streaming_enabled") && typeof profile.streaming_enabled !== "boolean") {
        errors.push(`${path}.streaming_enabled must be boolean.`);
      }

      if (hasOwn(profile, "capabilities")) {
        if (
          !profile.capabilities ||
          typeof profile.capabilities !== "object" ||
          Array.isArray(profile.capabilities)
        ) {
          errors.push(`${path}.capabilities must be an object.`);
        } else {
          ["supports_reasoning", "supports_vision", "supports_json"].forEach((key) => {
            if (hasOwn(profile.capabilities, key) && typeof profile.capabilities[key] !== "boolean") {
              errors.push(`${path}.capabilities.${key} must be boolean.`);
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
          ["temperature", "top_p"].forEach((key) => {
            if (hasOwn(profile.parameter_overrides, key)) {
              const value = profile.parameter_overrides[key];
              if (typeof value !== "number" || !Number.isFinite(value)) {
                errors.push(`${path}.parameter_overrides.${key} must be a number.`);
              }
            }
          });
        }
      }

      if (hasOwn(profile, "token_policy")) {
        if (
          !profile.token_policy ||
          typeof profile.token_policy !== "object" ||
          Array.isArray(profile.token_policy)
        ) {
          errors.push(`${path}.token_policy must be an object.`);
        } else {
          if (hasOwn(profile.token_policy, "max_tokens")) {
            const value = profile.token_policy.max_tokens;
            if (typeof value !== "number" || !Number.isFinite(value)) {
              errors.push(`${path}.token_policy.max_tokens must be a number.`);
            } else if (value <= 0) {
              errors.push(`${path}.token_policy.max_tokens must be > 0.`);
            }
          }
          if (hasOwn(profile.token_policy, "reasoning_effort")) {
            const value = profile.token_policy.reasoning_effort;
            if (typeof value !== "string") {
              errors.push(`${path}.token_policy.reasoning_effort must be a string.`);
            } else if (value.trim() === "") {
              errors.push(`${path}.token_policy.reasoning_effort must not be empty.`);
            }
          }
        }
      }

      if (hasOwn(profile, "auto_scan")) {
        if (!profile.auto_scan || typeof profile.auto_scan !== "object" || Array.isArray(profile.auto_scan)) {
          errors.push(`${path}.auto_scan must be an object.`);
        } else {
          if (hasOwn(profile.auto_scan, "enabled") && typeof profile.auto_scan.enabled !== "boolean") {
            errors.push(`${path}.auto_scan.enabled must be boolean.`);
          }
          if (hasOwn(profile.auto_scan, "interval_ms")) {
            const value = profile.auto_scan.interval_ms;
            if (typeof value !== "number" || !Number.isFinite(value)) {
              errors.push(`${path}.auto_scan.interval_ms must be a number.`);
            } else if (value < 1000) {
              errors.push(`${path}.auto_scan.interval_ms must be >= 1000.`);
            }
          }
        }
      }
    });

    if (typeof data.default_profile_id === "string" && data.default_profile_id.trim() !== "") {
      if (!ids.has(data.default_profile_id)) {
        errors.push(
          `default_profile_id '${data.default_profile_id}' does not match any profile.id.`
        );
      }
    }

    return errors;
  };

  const generateCopyId = (baseId) => {
    const profiles = getProfiles();
    const existing = new Set(
      profiles.map((profile) => profile && profile.id).filter((id) => typeof id === "string")
    );
    let index = 1;
    let candidate = `${baseId}-copy-${index}`;
    while (existing.has(candidate)) {
      index += 1;
      candidate = `${baseId}-copy-${index}`;
    }
    return candidate;
  };

  const loadJsonText = (text, fileName, fileHandle) => {
    let parsed;
    try {
      parsed = JSON.parse(text);
    } catch (error) {
      setStatus("Failed to parse JSON.");
      return;
    }

    if (!parsed || typeof parsed !== "object") {
      setStatus("JSON root must be an object.");
      return;
    }
    if (!Array.isArray(parsed.profiles)) {
      parsed.profiles = [];
    }

    state.data = parsed;
    state.fileHandle = fileHandle || null;
    state.fileName = fileName || "vlm-profiles.json";

    el.defaultProfileId.value = parsed.default_profile_id || "";
    el.defaultProvider.value = getDefaultProvider();
    el.downloadButton.disabled = false;
    refreshProfileSelect(false);
    updateValidation();

    const count = Array.isArray(parsed.profiles) ? parsed.profiles.length : 0;
    setStatus(`${state.fileName} loaded (${count} profiles)`);
  };

  const openFile = async () => {
    if (window.showOpenFilePicker) {
      try {
        const [handle] = await window.showOpenFilePicker({
          types: [
            {
              description: "JSON",
              accept: { "application/json": [".json"] },
            },
          ],
          excludeAcceptAllOption: false,
          multiple: false,
        });
        if (!handle) {
          return;
        }
        const file = await handle.getFile();
        const text = await file.text();
        loadJsonText(text, file.name, handle);
        return;
      } catch (error) {
        if (error && error.name === "AbortError") {
          return;
        }
      }
    }
    el.fileInput.click();
  };

  const getPrettyJson = () => {
    if (!state.data) {
      return "";
    }
    return `${JSON.stringify(state.data, null, 2)}\n`;
  };

  const downloadJson = () => {
    if (!state.data) {
      return;
    }
    const text = getPrettyJson();
    const blob = new Blob([text], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = state.fileName || "vlm-profiles.json";
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  const saveJson = async () => {
    if (!state.data) {
      return;
    }
    if (!state.fileHandle || !state.fileHandle.createWritable) {
      downloadJson();
      setStatus("Downloaded JSON (no file handle).");
      return;
    }
    try {
      const writable = await state.fileHandle.createWritable();
      await writable.write(getPrettyJson());
      await writable.close();
      setStatus(`Saved ${state.fileName}`);
    } catch (error) {
      setStatus("Save failed; use Download instead.");
    }
  };

  el.openButton.addEventListener("click", () => {
    openFile();
  });

  el.fileInput.addEventListener("change", async (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) {
      return;
    }
    const text = await file.text();
    loadJsonText(text, file.name, null);
    el.fileInput.value = "";
  });

  el.profileSelect.addEventListener("change", (event) => {
    const nextIndex = Number(event.target.value);
    const profiles = getProfiles();
    if (!Number.isNaN(nextIndex) && profiles[nextIndex]) {
      state.currentIndex = nextIndex;
      renderProfile(profiles[nextIndex]);
      updateValidation();
    }
  });

  el.duplicateButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    const profiles = getProfiles();
    if (!profile) {
      return;
    }
    const copy = JSON.parse(JSON.stringify(profile));
    const baseId = profile.id && profile.id.trim() !== "" ? profile.id : "profile";
    copy.id = generateCopyId(baseId);
    profiles.splice(state.currentIndex + 1, 0, copy);
    state.currentIndex += 1;
    refreshProfileSelect(true);
    updateValidation();
  });

  el.deleteButton.addEventListener("click", () => {
    const profiles = getProfiles();
    if (!profiles.length) {
      return;
    }
    const profile = getCurrentProfile();
    const name = profile ? profile.label || profile.id || "this profile" : "this profile";
    const confirmed = window.confirm(`Delete ${name}?`);
    if (!confirmed) {
      return;
    }
    profiles.splice(state.currentIndex, 1);
    if (profiles.length === 0) {
      state.currentIndex = -1;
      refreshProfileSelect(true);
    } else {
      state.currentIndex = Math.min(state.currentIndex, profiles.length - 1);
      refreshProfileSelect(true);
    }
    updateValidation();
  });

  el.setDefaultButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    if (!profile || !state.data) {
      return;
    }
    state.data.default_profile_id = profile.id || "";
    el.defaultProfileId.value = state.data.default_profile_id || "";
    updateDefaultIndicators();
    updateValidation();
  });

  el.validateButton.addEventListener("click", () => {
    updateValidation();
  });

  el.saveButton.addEventListener("click", () => {
    saveJson();
  });

  el.downloadButton.addEventListener("click", () => {
    downloadJson();
  });

  el.fieldId.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile || !state.data) {
      return;
    }
    const previousId = profile.id;
    profile.id = el.fieldId.value;
    if (state.data.default_profile_id === previousId) {
      state.data.default_profile_id = profile.id;
      el.defaultProfileId.value = profile.id || "";
    }
    updateDefaultIndicators();
    updateValidation();
  });

  el.fieldLabel.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    profile.label = el.fieldLabel.value;
    updateDefaultIndicators();
    updateValidation();
  });

  el.fieldDescription.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    profile.description = el.fieldDescription.value;
    updateValidation();
  });

  el.fieldProvider.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    updateOptionalString(profile, "provider", el.fieldProvider);
    updateValidation();
  });

  el.fieldModelId.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    profile.model_id = el.fieldModelId.value;
    updateValidation();
  });

  el.fieldStreaming.addEventListener("change", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    profile.streaming_enabled = el.fieldStreaming.checked;
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldSupportsReasoning.addEventListener("change", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const caps = ensureObject(profile, "capabilities");
    caps.supports_reasoning = el.fieldSupportsReasoning.checked;
    cleanupEmptyObject(profile, "capabilities");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldSupportsVision.addEventListener("change", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const caps = ensureObject(profile, "capabilities");
    caps.supports_vision = el.fieldSupportsVision.checked;
    cleanupEmptyObject(profile, "capabilities");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldSupportsJson.addEventListener("change", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const caps = ensureObject(profile, "capabilities");
    caps.supports_json = el.fieldSupportsJson.checked;
    cleanupEmptyObject(profile, "capabilities");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldMaxTokens.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const policy = ensureObject(profile, "token_policy");
    updateOptionalNumber(policy, "max_tokens", el.fieldMaxTokens);
    cleanupEmptyObject(profile, "token_policy");
    updateValidation();
  });

  el.fieldReasoningEffort.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const policy = ensureObject(profile, "token_policy");
    updateOptionalString(policy, "reasoning_effort", el.fieldReasoningEffort);
    cleanupEmptyObject(profile, "token_policy");
    updateValidation();
  });

  el.fieldTemperature.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const overrides = ensureObject(profile, "parameter_overrides");
    updateOptionalNumber(overrides, "temperature", el.fieldTemperature);
    cleanupEmptyObject(profile, "parameter_overrides");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldTopP.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const overrides = ensureObject(profile, "parameter_overrides");
    updateOptionalNumber(overrides, "top_p", el.fieldTopP);
    cleanupEmptyObject(profile, "parameter_overrides");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldAutoScanEnabled.addEventListener("change", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const autoScan = ensureObject(profile, "auto_scan");
    autoScan.enabled = el.fieldAutoScanEnabled.checked;
    cleanupEmptyObject(profile, "auto_scan");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldAutoScanInterval.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    const autoScan = ensureObject(profile, "auto_scan");
    updateOptionalNumber(autoScan, "interval_ms", el.fieldAutoScanInterval);
    cleanupEmptyObject(profile, "auto_scan");
    placeOptionalFields(profile);
    updateValidation();
  });

  el.fieldSystemPrompt.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    updateOptionalString(profile, "system_prompt", el.fieldSystemPrompt);
    updateValidation();
  });

  el.fieldOverviewPrompt.addEventListener("input", () => {
    const profile = getCurrentProfile();
    if (!profile) {
      return;
    }
    updateOptionalString(profile, "overview_prompt", el.fieldOverviewPrompt);
    updateValidation();
  });
})();
