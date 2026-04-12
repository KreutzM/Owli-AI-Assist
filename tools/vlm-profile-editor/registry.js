"use strict";

(() => {
  const REGISTRY_SCHEMA_VERSION = "vlm_profile_registry/v1";
  const state = { data: null, fileHandle: null, fileName: null, currentIndex: -1 };
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
    schemaVersion: $("schemaVersion"),
    defaultProfileId: $("defaultProfileId"),
    validationSummary: $("validationSummary"),
    validationErrors: $("validationErrors"),
    profileFields: $("profileFields"),
    fileInput: $("fileInput"),
    fieldId: $("fieldId"),
    fieldLabel: $("fieldLabel"),
    fieldDescription: $("fieldDescription"),
    fieldAvailability: $("fieldAvailability"),
    fieldUiGroup: $("fieldUiGroup"),
    fieldUiSortOrder: $("fieldUiSortOrder"),
    fieldUiBadges: $("fieldUiBadges"),
    fieldUiHidden: $("fieldUiHidden"),
    fieldBackendProfileId: $("fieldBackendProfileId"),
    fieldBackendStreaming: $("fieldBackendStreaming"),
    fieldBackendFollowup: $("fieldBackendFollowup"),
    fieldBackendFollowupImages: $("fieldBackendFollowupImages"),
    fieldBackendNotes: $("fieldBackendNotes"),
    fieldByokProvider: $("fieldByokProvider"),
    fieldByokModelId: $("fieldByokModelId"),
    fieldByokFamily: $("fieldByokFamily"),
    fieldByokStreaming: $("fieldByokStreaming"),
    fieldByokSupportsVision: $("fieldByokSupportsVision"),
    fieldByokSupportsReasoning: $("fieldByokSupportsReasoning"),
    fieldByokSupportsJson: $("fieldByokSupportsJson"),
    fieldDebugEmbeddedAllowed: $("fieldDebugEmbeddedAllowed"),
    fieldByokImageMaxSide: $("fieldByokImageMaxSide"),
    fieldByokImageJpegQuality: $("fieldByokImageJpegQuality"),
    fieldByokMaxTokens: $("fieldByokMaxTokens"),
    fieldByokTemperature: $("fieldByokTemperature"),
    fieldByokReasoningEffort: $("fieldByokReasoningEffort"),
    fieldByokReasoningExclude: $("fieldByokReasoningExclude"),
    fieldByokRetry1: $("fieldByokRetry1"),
    fieldByokRetry2: $("fieldByokRetry2"),
    fieldByokAutoScanEnabled: $("fieldByokAutoScanEnabled"),
    fieldByokAutoScanInterval: $("fieldByokAutoScanInterval"),
    fieldByokAutoScanSpeakFree: $("fieldByokAutoScanSpeakFree"),
    fieldByokSystemPrompt: $("fieldByokSystemPrompt"),
    fieldByokOverviewPrompt: $("fieldByokOverviewPrompt"),
  };

  const setStatus = (message) => { el.fileStatus.textContent = message || ""; };
  const setInputValue = (input, value) => { input.value = value === undefined || value === null ? "" : String(value); };
  const setCheckboxValue = (input, value) => { input.checked = Boolean(value); };
  const getProfiles = () => state.data && Array.isArray(state.data.profiles) ? state.data.profiles : [];
  const getCurrentProfile = () => {
    const profiles = getProfiles();
    return state.currentIndex >= 0 && state.currentIndex < profiles.length ? profiles[state.currentIndex] : null;
  };
  const ensureObject = (parent, key) => {
    if (!parent[key] || typeof parent[key] !== "object" || Array.isArray(parent[key])) parent[key] = {};
    return parent[key];
  };
  const cleanupEmptyObject = (parent, key) => {
    if (parent && parent[key] && typeof parent[key] === "object" && !Array.isArray(parent[key]) && Object.keys(parent[key]).length === 0) {
      delete parent[key];
    }
  };
  const readOptionalString = (input) => {
    const raw = input.value || "";
    return raw.trim() === "" ? null : raw;
  };
  const readOptionalNumber = (input) => {
    const raw = (input.value || "").trim();
    if (raw === "") return null;
    const value = Number(raw);
    return Number.isFinite(value) ? value : null;
  };
  const badgesFromInput = (value) => value.split(",").map((item) => item.trim()).filter(Boolean);
  const stableStringify = (value) => JSON.stringify(value, null, 2) + "\n";

  const validateRegistry = (data) => {
    const errors = [];
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      errors.push("Root must be an object.");
      return errors;
    }
    if (data.schema_version !== REGISTRY_SCHEMA_VERSION) {
      errors.push(`schema_version must be '${REGISTRY_SCHEMA_VERSION}'.`);
    }
    if (typeof data.default_profile_id !== "string" || data.default_profile_id.trim() === "") {
      errors.push("default_profile_id must be a non-empty string.");
    }
    if (!Array.isArray(data.profiles)) {
      errors.push("profiles must be an array.");
      return errors;
    }
    const ids = new Set();
    const validAvailability = new Set(["backend", "byok", "both"]);
    data.profiles.forEach((profile, index) => {
      const path = `profiles[${index}]`;
      if (!profile || typeof profile !== "object" || Array.isArray(profile)) {
        errors.push(`${path} must be an object.`);
        return;
      }
      ["id", "label", "description", "availability"].forEach((key) => {
        if (typeof profile[key] !== "string" || profile[key].trim() === "") {
          errors.push(`${path}.${key} must be a non-empty string.`);
        }
      });
      if (typeof profile.id === "string") {
        if (ids.has(profile.id)) errors.push(`${path}.id '${profile.id}' is duplicated.`);
        ids.add(profile.id);
      }
      if (!validAvailability.has(profile.availability)) {
        errors.push(`${path}.availability must be one of backend, byok, both.`);
      }
      if ((profile.availability === "backend" || profile.availability === "both") && (!profile.backend || typeof profile.backend !== "object")) {
        errors.push(`${path}.backend is required for availability '${profile.availability}'.`);
      }
      if ((profile.availability === "byok" || profile.availability === "both") && (!profile.byok || typeof profile.byok !== "object")) {
        errors.push(`${path}.byok is required for availability '${profile.availability}'.`);
      }
    });
    if (typeof data.default_profile_id === "string" && data.default_profile_id.trim() !== "" && !ids.has(data.default_profile_id)) {
      errors.push(`default_profile_id '${data.default_profile_id}' does not match any profile.id.`);
    }
    return errors;
  };

  const setValidation = (errors) => {
    el.validationErrors.innerHTML = "";
    if (!errors.length) {
      el.validationSummary.textContent = "No validation errors.";
      el.validationSummary.classList.remove("warn");
      return;
    }
    el.validationSummary.textContent = `${errors.length} validation issue(s) found.`;
    el.validationSummary.classList.add("warn");
    errors.forEach((error) => {
      const li = document.createElement("li");
      li.textContent = error;
      el.validationErrors.appendChild(li);
    });
  };

  const updateValidation = () => setValidation(state.data ? validateRegistry(state.data) : []);

  const renderProfile = (profile) => {
    if (!profile) return;
    const ui = profile.ui || {};
    const backend = profile.backend || {};
    const byok = profile.byok || {};
    const capabilities = byok.capabilities || {};
    const image = byok.image || {};
    const tokenPolicy = byok.token_policy || {};
    const overrides = byok.parameter_overrides || {};
    const autoScan = byok.auto_scan || {};
    const debug = profile.debug || {};

    setInputValue(el.fieldId, profile.id);
    setInputValue(el.fieldLabel, profile.label);
    setInputValue(el.fieldDescription, profile.description);
    setInputValue(el.fieldAvailability, profile.availability);
    setInputValue(el.fieldUiGroup, ui.group);
    setInputValue(el.fieldUiSortOrder, ui.sort_order);
    setInputValue(el.fieldUiBadges, Array.isArray(ui.badges) ? ui.badges.join(", ") : "");
    setCheckboxValue(el.fieldUiHidden, ui.hidden);
    setInputValue(el.fieldBackendProfileId, backend.profile_id);
    setCheckboxValue(el.fieldBackendStreaming, backend.supports_streaming);
    setCheckboxValue(el.fieldBackendFollowup, backend.supports_followup);
    setCheckboxValue(el.fieldBackendFollowupImages, backend.supports_followup_images);
    setInputValue(el.fieldBackendNotes, backend.notes);
    setInputValue(el.fieldByokProvider, byok.provider);
    setInputValue(el.fieldByokModelId, byok.model_id);
    setInputValue(el.fieldByokFamily, byok.family);
    setCheckboxValue(el.fieldByokStreaming, byok.streaming_enabled);
    setCheckboxValue(el.fieldByokSupportsVision, capabilities.supports_vision);
    setCheckboxValue(el.fieldByokSupportsReasoning, capabilities.supports_reasoning);
    setCheckboxValue(el.fieldByokSupportsJson, capabilities.supports_json);
    setCheckboxValue(el.fieldDebugEmbeddedAllowed, debug.embedded_key_allowed);
    setInputValue(el.fieldByokImageMaxSide, image.max_side_px);
    setInputValue(el.fieldByokImageJpegQuality, image.jpeg_quality);
    setInputValue(el.fieldByokMaxTokens, tokenPolicy.max_tokens);
    setInputValue(el.fieldByokTemperature, overrides.temperature);
    setInputValue(el.fieldByokReasoningEffort, tokenPolicy.reasoning_effort);
    setCheckboxValue(el.fieldByokReasoningExclude, tokenPolicy.reasoning_exclude);
    setInputValue(el.fieldByokRetry1, tokenPolicy.retry1_max_tokens);
    setInputValue(el.fieldByokRetry2, tokenPolicy.retry2_max_tokens);
    setCheckboxValue(el.fieldByokAutoScanEnabled, autoScan.enabled_by_default);
    setInputValue(el.fieldByokAutoScanInterval, autoScan.interval_ms);
    setInputValue(el.fieldByokAutoScanSpeakFree, autoScan.speak_free_every_ms);
    setInputValue(el.fieldByokSystemPrompt, byok.system_prompt);
    setInputValue(el.fieldByokOverviewPrompt, byok.overview_prompt);
    el.profileFields.disabled = false;
  };

  const clearForm = () => {
    [
      el.fieldId, el.fieldLabel, el.fieldDescription, el.fieldUiGroup, el.fieldUiSortOrder,
      el.fieldUiBadges, el.fieldBackendProfileId, el.fieldBackendNotes, el.fieldByokProvider,
      el.fieldByokModelId, el.fieldByokFamily, el.fieldByokImageMaxSide, el.fieldByokImageJpegQuality,
      el.fieldByokMaxTokens, el.fieldByokTemperature, el.fieldByokReasoningEffort, el.fieldByokRetry1,
      el.fieldByokRetry2, el.fieldByokAutoScanInterval, el.fieldByokAutoScanSpeakFree,
      el.fieldByokSystemPrompt, el.fieldByokOverviewPrompt
    ].forEach((input) => setInputValue(input, ""));
    [
      el.fieldUiHidden, el.fieldBackendStreaming, el.fieldBackendFollowup, el.fieldBackendFollowupImages,
      el.fieldByokStreaming, el.fieldByokSupportsVision, el.fieldByokSupportsReasoning,
      el.fieldByokSupportsJson, el.fieldDebugEmbeddedAllowed, el.fieldByokReasoningExclude,
      el.fieldByokAutoScanEnabled
    ].forEach((input) => setCheckboxValue(input, false));
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

  const refreshProfileSelect = () => {
    const profiles = getProfiles();
    el.profileSelect.innerHTML = "";
    profiles.forEach((profile, index) => {
      const option = document.createElement("option");
      option.value = String(index);
      option.textContent = `${profile.label} (${profile.id})${profile.id === state.data.default_profile_id ? " (default)" : ""}`;
      el.profileSelect.appendChild(option);
    });
    if (profiles.length) {
      if (state.currentIndex < 0 || state.currentIndex >= profiles.length) state.currentIndex = 0;
      el.profileSelect.value = String(state.currentIndex);
      renderProfile(getCurrentProfile());
    } else {
      state.currentIndex = -1;
      clearForm();
    }
    setInputValue(el.schemaVersion, state.data.schema_version);
    setInputValue(el.defaultProfileId, state.data.default_profile_id);
    updateActionState();
    updateValidation();
  };

  const writeCurrentProfile = () => {
    const profile = getCurrentProfile();
    if (!profile) return;
    profile.id = readOptionalString(el.fieldId) || "";
    profile.label = readOptionalString(el.fieldLabel) || "";
    profile.description = readOptionalString(el.fieldDescription) || "";
    profile.availability = el.fieldAvailability.value;

    const ui = ensureObject(profile, "ui");
    const group = readOptionalString(el.fieldUiGroup);
    const sortOrder = readOptionalNumber(el.fieldUiSortOrder);
    const badges = badgesFromInput(el.fieldUiBadges.value || "");
    if (group) ui.group = group; else delete ui.group;
    if (sortOrder !== null) ui.sort_order = sortOrder; else delete ui.sort_order;
    if (badges.length) ui.badges = badges; else delete ui.badges;
    if (el.fieldUiHidden.checked) ui.hidden = true; else delete ui.hidden;
    cleanupEmptyObject(profile, "ui");

    const backendWanted = profile.availability === "backend" || profile.availability === "both";
    if (backendWanted) {
      const backend = ensureObject(profile, "backend");
      const profileId = readOptionalString(el.fieldBackendProfileId);
      if (profileId) backend.profile_id = profileId; else delete backend.profile_id;
      backend.supports_streaming = el.fieldBackendStreaming.checked;
      backend.supports_followup = el.fieldBackendFollowup.checked;
      backend.supports_followup_images = el.fieldBackendFollowupImages.checked;
      const notes = readOptionalString(el.fieldBackendNotes);
      if (notes) backend.notes = notes; else delete backend.notes;
    } else {
      delete profile.backend;
    }

    const byokWanted = profile.availability === "byok" || profile.availability === "both";
    if (byokWanted) {
      const byok = ensureObject(profile, "byok");
      const provider = readOptionalString(el.fieldByokProvider);
      const modelId = readOptionalString(el.fieldByokModelId);
      const family = readOptionalString(el.fieldByokFamily);
      const systemPrompt = readOptionalString(el.fieldByokSystemPrompt);
      const overviewPrompt = readOptionalString(el.fieldByokOverviewPrompt);
      if (provider) byok.provider = provider; else delete byok.provider;
      if (modelId) byok.model_id = modelId; else delete byok.model_id;
      if (family) byok.family = family; else delete byok.family;
      byok.streaming_enabled = el.fieldByokStreaming.checked;
      if (systemPrompt) byok.system_prompt = systemPrompt; else delete byok.system_prompt;
      if (overviewPrompt) byok.overview_prompt = overviewPrompt; else delete byok.overview_prompt;

      const capabilities = ensureObject(byok, "capabilities");
      capabilities.supports_vision = el.fieldByokSupportsVision.checked;
      capabilities.supports_reasoning = el.fieldByokSupportsReasoning.checked;
      capabilities.supports_json = el.fieldByokSupportsJson.checked;

      const image = ensureObject(byok, "image");
      const maxSide = readOptionalNumber(el.fieldByokImageMaxSide);
      const jpegQuality = readOptionalNumber(el.fieldByokImageJpegQuality);
      if (maxSide !== null) image.max_side_px = maxSide; else delete image.max_side_px;
      if (jpegQuality !== null) image.jpeg_quality = jpegQuality; else delete image.jpeg_quality;
      cleanupEmptyObject(byok, "image");

      const tokenPolicy = ensureObject(byok, "token_policy");
      const maxTokens = readOptionalNumber(el.fieldByokMaxTokens);
      const reasoningEffort = readOptionalString(el.fieldByokReasoningEffort);
      const retry1 = readOptionalNumber(el.fieldByokRetry1);
      const retry2 = readOptionalNumber(el.fieldByokRetry2);
      if (maxTokens !== null) tokenPolicy.max_tokens = maxTokens; else delete tokenPolicy.max_tokens;
      if (reasoningEffort) tokenPolicy.reasoning_effort = reasoningEffort; else delete tokenPolicy.reasoning_effort;
      if (el.fieldByokReasoningExclude.checked) tokenPolicy.reasoning_exclude = true; else delete tokenPolicy.reasoning_exclude;
      if (retry1 !== null) tokenPolicy.retry1_max_tokens = retry1; else delete tokenPolicy.retry1_max_tokens;
      if (retry2 !== null) tokenPolicy.retry2_max_tokens = retry2; else delete tokenPolicy.retry2_max_tokens;
      cleanupEmptyObject(byok, "token_policy");

      const overrides = ensureObject(byok, "parameter_overrides");
      const temperature = readOptionalNumber(el.fieldByokTemperature);
      if (temperature !== null) overrides.temperature = temperature; else delete overrides.temperature;
      cleanupEmptyObject(byok, "parameter_overrides");

      const autoScan = ensureObject(byok, "auto_scan");
      if (el.fieldByokAutoScanEnabled.checked) autoScan.enabled_by_default = true; else delete autoScan.enabled_by_default;
      const interval = readOptionalNumber(el.fieldByokAutoScanInterval);
      const speakFree = readOptionalNumber(el.fieldByokAutoScanSpeakFree);
      if (interval !== null) autoScan.interval_ms = interval; else delete autoScan.interval_ms;
      if (speakFree !== null) autoScan.speak_free_every_ms = speakFree; else delete autoScan.speak_free_every_ms;
      cleanupEmptyObject(byok, "auto_scan");
    } else {
      delete profile.byok;
    }

    const debug = ensureObject(profile, "debug");
    if (el.fieldDebugEmbeddedAllowed.checked) debug.embedded_key_allowed = true; else delete debug.embedded_key_allowed;
    cleanupEmptyObject(profile, "debug");

    updateValidation();
    refreshProfileSelect();
  };

  const generateCopyId = (baseId) => {
    const ids = new Set(getProfiles().map((profile) => profile.id));
    let counter = 1;
    let candidate = `${baseId}-copy-${counter}`;
    while (ids.has(candidate)) {
      counter += 1;
      candidate = `${baseId}-copy-${counter}`;
    }
    return candidate;
  };

  const loadJsonText = (text, fileName, fileHandle) => {
    try {
      const parsed = JSON.parse(text);
      if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
        setStatus("JSON root must be an object.");
        return;
      }
      if (!Array.isArray(parsed.profiles)) parsed.profiles = [];
      if (!parsed.schema_version) parsed.schema_version = REGISTRY_SCHEMA_VERSION;
      state.data = parsed;
      state.fileHandle = fileHandle || null;
      state.fileName = fileName || "vlm-profile-registry.json";
      state.currentIndex = parsed.profiles.length ? 0 : -1;
      refreshProfileSelect();
      setStatus(`${state.fileName} loaded (${parsed.profiles.length} profiles)`);
    } catch {
      setStatus("Failed to parse JSON.");
    }
  };

  const openFileViaPicker = async () => {
    if (!window.showOpenFilePicker) return false;
    try {
      const [handle] = await window.showOpenFilePicker({
        types: [{ description: "JSON", accept: { "application/json": [".json"] } }],
        multiple: false,
      });
      const file = await handle.getFile();
      loadJsonText(await file.text(), file.name, handle);
      return true;
    } catch {
      return false;
    }
  };

  const downloadJson = (fileName, text) => {
    const blob = new Blob([text], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
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

  el.openButton.addEventListener("click", async () => {
    const ok = await openFileViaPicker();
    if (!ok) {
      el.fileInput.value = "";
      el.fileInput.click();
    }
  });

  el.fileInput.addEventListener("change", async (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    loadJsonText(await file.text(), file.name, null);
  });

  el.profileSelect.addEventListener("change", () => {
    state.currentIndex = Number(el.profileSelect.value);
    renderProfile(getCurrentProfile());
    updateActionState();
  });

  [
    el.schemaVersion, el.defaultProfileId, el.fieldId, el.fieldLabel, el.fieldDescription,
    el.fieldUiGroup, el.fieldUiSortOrder, el.fieldUiBadges, el.fieldBackendProfileId,
    el.fieldBackendNotes, el.fieldByokProvider, el.fieldByokModelId, el.fieldByokFamily,
    el.fieldByokImageMaxSide, el.fieldByokImageJpegQuality, el.fieldByokMaxTokens,
    el.fieldByokTemperature, el.fieldByokReasoningEffort, el.fieldByokRetry1,
    el.fieldByokRetry2, el.fieldByokAutoScanInterval, el.fieldByokAutoScanSpeakFree,
    el.fieldByokSystemPrompt, el.fieldByokOverviewPrompt
  ].forEach((input) => input.addEventListener("input", () => {
    if (!state.data) return;
    state.data.schema_version = readOptionalString(el.schemaVersion) || REGISTRY_SCHEMA_VERSION;
    state.data.default_profile_id = readOptionalString(el.defaultProfileId) || "";
    writeCurrentProfile();
  }));

  [
    el.fieldAvailability, el.fieldUiHidden, el.fieldBackendStreaming, el.fieldBackendFollowup,
    el.fieldBackendFollowupImages, el.fieldByokStreaming, el.fieldByokSupportsVision,
    el.fieldByokSupportsReasoning, el.fieldByokSupportsJson, el.fieldDebugEmbeddedAllowed,
    el.fieldByokReasoningExclude, el.fieldByokAutoScanEnabled
  ].forEach((input) => input.addEventListener("change", () => {
    if (!state.data) return;
    writeCurrentProfile();
  }));

  el.duplicateButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    if (!profile) return;
    const copy = JSON.parse(JSON.stringify(profile));
    copy.id = generateCopyId(profile.id || "profile");
    state.data.profiles.push(copy);
    state.currentIndex = state.data.profiles.length - 1;
    refreshProfileSelect();
  });

  el.deleteButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    if (!profile) return;
    if (!window.confirm(`Delete profile '${profile.id || "(unknown)"}'?`)) return;
    state.data.profiles.splice(state.currentIndex, 1);
    if (state.currentIndex >= state.data.profiles.length) state.currentIndex = state.data.profiles.length - 1;
    refreshProfileSelect();
  });

  el.setDefaultButton.addEventListener("click", () => {
    const profile = getCurrentProfile();
    if (!profile) return;
    state.data.default_profile_id = profile.id;
    setInputValue(el.defaultProfileId, profile.id);
    refreshProfileSelect();
  });

  el.validateButton.addEventListener("click", () => updateValidation());

  el.saveButton.addEventListener("click", async () => {
    if (!state.data) return;
    const text = stableStringify(state.data);
    const saved = await saveToHandle(text);
    if (!saved) {
      downloadJson(state.fileName || "vlm-profile-registry.json", text);
      setStatus("Downloaded JSON (Save-back not available in this context).");
    }
  });

  el.downloadButton.addEventListener("click", () => {
    if (!state.data) return;
    downloadJson(state.fileName || "vlm-profile-registry.json", stableStringify(state.data));
  });

  clearForm();
  updateActionState();
  updateValidation();
  setStatus("Open a vlm-profile-registry.json to begin.");
})();
