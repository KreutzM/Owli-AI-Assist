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

- The QR payload format is `openrouter:key=<KEY>`.
- QR generation happens locally in the browser.
- The key is not sent to a backend.
- The tool does not use `localStorage`, `sessionStorage`, query parameters, or server-side code.
