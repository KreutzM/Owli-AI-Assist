# OpenRouter QR Generator

Static browser tool for generating an Owli-compatible OpenRouter key QR code entirely on the client.

## Files

- `index.html` - static page markup
- `styles.css` - local styling
- `app.js` - browser-only logic
- `qrcode.min.js` - vendored QR generator library from `davidshimjs/qrcodejs`

## Usage

Recommended local preview:

```bash
cd tools/QR-Gen
python -m http.server 5174
```

Then open:

- `http://localhost:5174`

Fallback:

- Open `tools/QR-Gen/index.html` directly in a browser.

## Behavior

- Plain mode uses `openrouter:key=<KEY>`.
- PIN mode uses `openrouter:keyenc:v1:pbkdf2-sha256:<iterations>:<salt_b64url>:<iv_b64url>:<ciphertext_b64url>`.
- A blank PIN defaults to `1597`; any non-empty PIN must be exactly 4 digits.
- PIN protection is for QR transport only and is not intended as strong long-term secret protection.
- QR generation happens locally in the browser.
- The key is not sent to a backend.
- The tool does not use `localStorage`, `sessionStorage`, query parameters, or server-side code.
- Browser-side cryptography uses Web Crypto with PBKDF2-SHA256 and AES-GCM.
