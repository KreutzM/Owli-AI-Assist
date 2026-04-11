(function () {
  const keyInput = document.getElementById("key-input");
  const showKeyToggle = document.getElementById("show-key");
  const form = document.getElementById("qr-form");
  const resetButton = document.getElementById("reset-button");
  const downloadButton = document.getElementById("download-button");
  const qrOutput = document.getElementById("qr-output");
  const message = document.getElementById("message");
  const payloadPreview = document.getElementById("payload-preview");

  let qrCode = null;

  function buildPayload(key) {
    return "openrouter:key=" + key.trim();
  }

  function setMessage(text, tone) {
    message.textContent = text;
    message.className = "message";
    if (tone) {
      message.classList.add(tone);
    }
  }

  function clearQr() {
    if (qrCode) {
      qrCode.clear();
      qrCode = null;
    }
    qrOutput.innerHTML = "";
    downloadButton.disabled = true;
  }

  function getRenderedGraphic() {
    return qrOutput.querySelector("canvas, img");
  }

  function generateQr(payload) {
    clearQr();
    qrCode = new QRCode(qrOutput, {
      text: payload,
      width: 320,
      height: 320,
      correctLevel: QRCode.CorrectLevel.M
    });
  }

  function downloadQr() {
    const rendered = getRenderedGraphic();
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
      setMessage("Could not export the QR image in this browser.", "error");
      return;
    }

    const link = document.createElement("a");
    link.href = sourceUrl;
    link.download = "openrouter-key-qr.png";
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  function resetForm() {
    keyInput.value = "";
    showKeyToggle.checked = false;
    keyInput.type = "password";
    payloadPreview.textContent = "openrouter:key=";
    clearQr();
    setMessage("Enter a key and generate a QR code.", null);
    keyInput.focus();
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();

    const rawKey = keyInput.value.trim();
    if (!rawKey) {
      clearQr();
      payloadPreview.textContent = "openrouter:key=";
      setMessage("Enter an OpenRouter key first.", "error");
      keyInput.focus();
      return;
    }

    const payload = buildPayload(rawKey);
    payloadPreview.textContent = payload;
    generateQr(payload);
    downloadButton.disabled = false;
    setMessage("QR code generated locally in your browser.", "success");
  });

  showKeyToggle.addEventListener("change", function () {
    keyInput.type = showKeyToggle.checked ? "text" : "password";
  });

  resetButton.addEventListener("click", resetForm);
  downloadButton.addEventListener("click", downloadQr);
})();
