(function (globalScope) {
  const DEFAULT_PIN = "1597";
  const PBKDF2_ITERATIONS = 200000;
  const SALT_LENGTH = 16;
  const IV_LENGTH = 12;
  const encoder = new TextEncoder();

  function normalizeKey(rawKey) {
    return String(rawKey || "").trim();
  }

  function buildPlainPayload(key) {
    return "openrouter:key=" + normalizeKey(key);
  }

  function normalizePin(rawPin) {
    const trimmed = String(rawPin || "").trim();
    if (trimmed === "") {
      return DEFAULT_PIN;
    }
    if (!/^\d{4}$/.test(trimmed)) {
      throw new Error("PIN must be exactly 4 digits.");
    }
    return trimmed;
  }

  function base64UrlFromBytes(bytes) {
    let binary = "";
    for (let index = 0; index < bytes.length; index += 1) {
      binary += String.fromCharCode(bytes[index]);
    }
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
  }

  async function deriveAesKey(pin, saltBytes) {
    const cryptoKey = await crypto.subtle.importKey(
      "raw",
      encoder.encode(pin),
      { name: "PBKDF2" },
      false,
      ["deriveKey"]
    );

    return crypto.subtle.deriveKey(
      {
        name: "PBKDF2",
        hash: "SHA-256",
        salt: saltBytes,
        iterations: PBKDF2_ITERATIONS
      },
      cryptoKey,
      { name: "AES-GCM", length: 256 },
      false,
      ["encrypt"]
    );
  }

  async function buildEncryptedPayload(key, pinInput) {
    const normalizedKey = normalizeKey(key);
    const normalizedPin = normalizePin(pinInput);
    const saltBytes = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
    const ivBytes = crypto.getRandomValues(new Uint8Array(IV_LENGTH));
    const aesKey = await deriveAesKey(normalizedPin, saltBytes);
    const ciphertextBuffer = await crypto.subtle.encrypt(
      {
        name: "AES-GCM",
        iv: ivBytes
      },
      aesKey,
      encoder.encode(normalizedKey)
    );
    const ciphertextBytes = new Uint8Array(ciphertextBuffer);

    return [
      "openrouter:keyenc:v1:pbkdf2-sha256",
      String(PBKDF2_ITERATIONS),
      base64UrlFromBytes(saltBytes),
      base64UrlFromBytes(ivBytes),
      base64UrlFromBytes(ciphertextBytes)
    ].join(":");
  }

  function createToolState() {
    return {
      qrCode: null
    };
  }

  function setMessage(messageNode, text, tone) {
    messageNode.textContent = text;
    messageNode.className = "message";
    if (tone) {
      messageNode.classList.add(tone);
    }
  }

  function clearQr(state, qrOutput, downloadButton) {
    if (state.qrCode) {
      state.qrCode.clear();
      state.qrCode = null;
    }
    qrOutput.innerHTML = "";
    downloadButton.disabled = true;
  }

  function getRenderedGraphic(qrOutput) {
    return qrOutput.querySelector("canvas, img");
  }

  function generateQr(state, qrOutput, downloadButton, payload) {
    clearQr(state, qrOutput, downloadButton);
    state.qrCode = new QRCode(qrOutput, {
      text: payload,
      width: 320,
      height: 320,
      correctLevel: QRCode.CorrectLevel.M
    });
  }

  function getSelectedMode(formNode) {
    const selected = formNode.querySelector('input[name="qr-mode"]:checked');
    return selected ? selected.value : "plain";
  }

  function updateModeVisibility(formNode) {
    const pinMode = getSelectedMode(formNode) === "pin";
    formNode.querySelector("#pin-field").classList.toggle("is-hidden", !pinMode);
    formNode.querySelector("#pin-help").classList.toggle("is-hidden", !pinMode);
    formNode.querySelector("#show-pin-wrap").classList.toggle("is-hidden", !pinMode);
  }

  function resetForm(formNode, messageNode, payloadPreview, state, qrOutput, downloadButton) {
    formNode.reset();
    formNode.querySelector("#key-input").type = "password";
    formNode.querySelector("#pin-input").type = "password";
    updateModeVisibility(formNode);
    payloadPreview.textContent = "openrouter:key=";
    clearQr(state, qrOutput, downloadButton);
    setMessage(messageNode, "Enter a key and generate a QR code.", null);
    formNode.querySelector("#key-input").focus();
  }

  function downloadQr(qrOutput, messageNode) {
    const rendered = getRenderedGraphic(qrOutput);
    if (!rendered) {
      return;
    }

    let sourceUrl = null;
    if (rendered.tagName === "CANVAS") {
      sourceUrl = rendered.toDataURL("image/png");
    } else if (rendered.tagName === "IMG") {
      sourceUrl = rendered.currentSrc || rendered.src;
    }

    if (!sourceUrl) {
      setMessage(messageNode, "Could not export the QR image in this browser.", "error");
      return;
    }

    const link = document.createElement("a");
    link.href = sourceUrl;
    link.download = "openrouter-key-qr.png";
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  function initDom() {
    const form = document.getElementById("qr-form");
    if (!form) {
      return;
    }

    const keyInput = document.getElementById("key-input");
    const pinInput = document.getElementById("pin-input");
    const showKeyToggle = document.getElementById("show-key");
    const showPinToggle = document.getElementById("show-pin");
    const resetButton = document.getElementById("reset-button");
    const downloadButton = document.getElementById("download-button");
    const qrOutput = document.getElementById("qr-output");
    const message = document.getElementById("message");
    const payloadPreview = document.getElementById("payload-preview");
    const state = createToolState();

    updateModeVisibility(form);

    form.addEventListener("change", function (event) {
      if (event.target && event.target.name === "qr-mode") {
        updateModeVisibility(form);
        payloadPreview.textContent = getSelectedMode(form) === "pin"
          ? "openrouter:keyenc:v1:pbkdf2-sha256:..."
          : "openrouter:key=";
      }
    });

    showKeyToggle.addEventListener("change", function () {
      keyInput.type = showKeyToggle.checked ? "text" : "password";
    });

    showPinToggle.addEventListener("change", function () {
      pinInput.type = showPinToggle.checked ? "text" : "password";
    });

    form.addEventListener("submit", async function (event) {
      event.preventDefault();

      const normalizedKey = normalizeKey(keyInput.value);
      if (!normalizedKey) {
        clearQr(state, qrOutput, downloadButton);
        payloadPreview.textContent = getSelectedMode(form) === "pin"
          ? "openrouter:keyenc:v1:pbkdf2-sha256:..."
          : "openrouter:key=";
        setMessage(message, "Enter an OpenRouter key first.", "error");
        keyInput.focus();
        return;
      }

      try {
        const mode = getSelectedMode(form);
        const payload = mode === "pin"
          ? await buildEncryptedPayload(normalizedKey, pinInput.value)
          : buildPlainPayload(normalizedKey);

        payloadPreview.textContent = payload;
        generateQr(state, qrOutput, downloadButton, payload);
        downloadButton.disabled = false;
        setMessage(
          message,
          mode === "pin"
            ? "PIN-protected QR code generated locally in your browser."
            : "Plain QR code generated locally in your browser.",
          "success"
        );
      } catch (error) {
        clearQr(state, qrOutput, downloadButton);
        setMessage(message, error.message || "Could not generate QR code.", "error");
      }
    });

    resetButton.addEventListener("click", function () {
      resetForm(form, message, payloadPreview, state, qrOutput, downloadButton);
    });

    downloadButton.addEventListener("click", function () {
      downloadQr(qrOutput, message);
    });
  }

  const exportedApi = {
    DEFAULT_PIN: DEFAULT_PIN,
    PBKDF2_ITERATIONS: PBKDF2_ITERATIONS,
    normalizeKey: normalizeKey,
    normalizePin: normalizePin,
    buildPlainPayload: buildPlainPayload,
    buildEncryptedPayload: buildEncryptedPayload
  };

  if (typeof module !== "undefined" && module.exports) {
    module.exports = exportedApi;
  }

  globalScope.QrGenApp = exportedApi;

  if (typeof document !== "undefined") {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", initDom);
    } else {
      initDom();
    }
  }
})(typeof globalThis !== "undefined" ? globalThis : window);
