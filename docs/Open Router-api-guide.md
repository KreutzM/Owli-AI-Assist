# OpenRouter API Guide (für VLM / Multimodal)

Dieser Guide beschreibt **präzise und implementierungsnah**, wie du über **OpenRouter.ai** verschiedene **Vision-Language-Models (VLMs)** per **OpenAI-kompatibler Chat-Completions API** ansteuerst – inkl. **Bildeingaben**, **Reasoning-Token**, **Streaming**, **Parameter-Sanitizing** und **Profile-basierter Modell-Austauschbarkeit**.

---

## 1) Grundprinzip

* OpenRouter stellt **eine einheitliche, OpenAI-kompatible** Schnittstelle bereit.
* Für Text + Bild (VLM) nutzt du immer:

  * **Endpoint:** `/api/v1/chat/completions`
  * **Body:** `model` + `messages` (mit Content-Array für multimodale Inputs)
* OpenRouter kann je nach Modell auf unterschiedliche Provider routen; dein Code bleibt dabei gleich.

---

## 2) Base URL, Endpoint, Auth, optionale App-Header

### Base URL

In allen Beispielen:

```text
https://openrouter.ai/api/v1
```

### Endpoint (Chat Completions)

```text
POST https://openrouter.ai/api/v1/chat/completions
```

### Required Header

```http
Authorization: Bearer <OPENROUTER_API_KEY>
Content-Type: application/json
```

### Optionale Header (Empfehlung)

Diese helfen bei App-Identifikation/Rankings auf OpenRouter:

```http
HTTP-Referer: <DEINE_APP_URL>
X-Title: <DEIN_APP_NAME>
```

---

## 3) Modell-Auswahl (Model IDs)

OpenRouter-Modelle werden i.d.R. als **`<provider>/<model>`** adressiert, z.B.

* `openai/gpt-4o-mini`
* `openai/gpt-5-nano`
* `anthropic/claude-...`

**Wichtig:** Für VLM brauchst du ein Modell, das **Images** unterstützt.

### Empfohlene Strategie (Profile)

Lege pro Modell ein Profil an (z.B. JSON), das mindestens enthält:

* `model`: string (OpenRouter Model ID)
* `capabilities`: { `vision`: bool, `reasoning`: bool, `tools`: bool, ... }
* `defaults`: { `max_tokens`, `temperature`, ... }
* `image`: { `maxSidePx`, `jpegQuality`, `detail` }
* `prompt`: { `system`, `overview` }

So kannst du Modelle **ohne Codeänderung** austauschen.

---

## 4) Request: Minimales Format (Text)

```json
{
  "model": "openai/gpt-4o-mini",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Say hi"}
  ],
  "max_tokens": 128,
  "temperature": 0.2,
  "stream": false
}
```

### Parameter: Grundregeln

* **Sende keine `null`-Werte.** Wenn ein Profil `null` enthält → Feld **weglassen**.
* Manche Modelle ignorieren unbekannte Parameter, andere nicht zuverlässig. Daher:

  * **pro Modellfamilie** eine Allowlist oder „sanitize“-Phase einbauen.

---

## 5) Multimodal: Bildeingabe (VLM)

### Wichtige Struktur

Bei multimodalen Nachrichten ist `content` **ein Array** aus Content-Teilen.

* Textteil:

  * `{ "type": "text", "text": "..." }`
* Bildteil:

  * `{ "type": "image_url", "image_url": { "url": "..." } }`

**Best Practice:** **Text zuerst**, dann Bild(er).

### Bild per URL

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "Describe this image."},
    {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg"}}
  ]
}
```

### Bild per Base64 Data URL (für lokale Kamera-Frames)

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "Describe this image."},
    {
      "type": "image_url",
      "image_url": {
        "url": "data:image/jpeg;base64,<BASE64_JPEG>"
      }
    }
  ]
}
```

### Mehrere Bilder

Mehrere Images = mehrere `image_url`-Einträge im gleichen `content`-Array.
Beachte: Max-Anzahl variiert nach Provider/Modell.

### Optional: `detail`

Einige Modelle unterstützen `detail` (z.B. `low|high|auto`). Falls du es nutzt, setze es im `image_url`-Objekt (nur wenn unterstützt):

```json
{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,...","detail":"auto"}}
```

---

## 6) Reasoning Tokens (wichtig für GPT-5 / Thinking-Modelle)

OpenRouter normalisiert Reasoning über ein einheitliches Feld:

```json
"reasoning": {
  "effort": "low",
  "exclude": false
}
```

### `reasoning.effort` Werte

* `xhigh`, `high`, `medium`, `low`, `minimal`, `none`

**Achtung: Output-Budget-Falle**

* Reasoning Tokens zählen als **Output Tokens**.
* Bei `effort=medium` kann ein großer Anteil des Output-Budgets „in thinking“ gehen.
* Wenn `max_tokens` zu klein ist, kann `message.content` leer bleiben.

### Empfehlung für produktive VLM-Beschreibungen (kurz, TTS-freundlich)

* Setze standardmäßig:

  * `reasoning.effort = "low"` oder `"minimal"`
  * oder: `reasoning.exclude = true` (Reasoning intern, aber nicht zurückgeben)

### Reasoning aus Response entfernen

Wenn du Reasoning NICHT im Response brauchst (z.B. für TTS), nutze:

```json
"reasoning": { "exclude": true }
```

---

## 7) Max Tokens & Output-Limits

### `max_tokens`

OpenRouter nutzt (OpenAI-kompatibel) typischerweise:

```json
"max_tokens": 256
```

**Praxisregel für Reasoning-Modelle:**

* `max_tokens` muss **deutlich** größer sein als der Reasoning-Budget-Anteil, sonst bleibt nichts für „final content“.
* Wenn du Reasoning aktiv nutzt, plane **höhere** `max_tokens` ein oder setze `reasoning.exclude=true`.

---

## 8) Streaming (SSE)

Setze:

```json
"stream": true
```

Dann kommt eine Server-Sent-Events (SSE) Antwort.

* Es können gelegentlich „comment“-Events auftreten → ignorieren.
* In Streaming werden Inhalte typischerweise in `delta`-Feldern geliefert.

**Empfehlung:** VLM im Mobile-Kontext kann Streaming für schnelleren TTS-Start nutzen (z.B. sobald erste sinnvolle Phrase da ist).

---

## 9) Response-Format (Parsing)

### Typische Response-Struktur

```json
{
  "id": "...",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "...",
        "reasoning": "...",
        "reasoning_details": [ ... ]
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 123,
    "completion_tokens": 456,
    "total_tokens": 579,
    "completion_tokens_details": {
      "reasoning_tokens": 300
    }
  }
}
```

### Parsing-Regeln (robust)

* Für UI/TTS **immer primär**: `choices[0].message.content`
* `message.reasoning` / `reasoning_details`:

  * nur für Debug/Telemetry oder Tool-Flow
* Wenn `content` leer UND reasoning vorhanden:

  * interpretieren als **Budgetproblem** (oder Modell gibt nur reasoning aus)
  * Fallback: `reasoning.exclude=true` und/oder `effort=low` und/oder `max_tokens` erhöhen

---

## 10) Fehlerbehandlung & Routing

OpenRouter kann bei Provider-Problemen (5xx) fallbacken.
Du solltest trotzdem robust bauen:

* 4xx:

  * Payload/Parameterfehler → Request sanitizen
* 429:

  * Rate limit → Backoff/Retry
* 5xx:

  * Provider down → Retry (OpenRouter kann ohnehin fallbacken)

### Logging

* Logge die final gesendete Payload **ohne API-Key**.
* Logge `usage` und `finish_reason`.
* Für Debug: Feature-Flag „echo upstream“ nur lokal nutzen.

---

## 11) Best Practices: Profile-basierte Multi-VLM Architektur

### Ziel

Neue Modelle sollen nur über JSON-Profile integrierbar sein:

* Profil definiert:

  * Model ID
  * Standard-Parameter
  * Capabilities
  * Prompt-Templates
  * Image-Preprocessing
  * Retry-Strategie

### Empfohlenes internes Datenmodell

* `VlmProfile`

  * `id`, `provider`, `model`
  * `capabilities`
  * `defaults` (max_tokens, temperature, top_p, reasoning, etc.)
  * `image` (maxSidePx, jpegQuality, detail)
  * `prompts` (system, overview)
  * `retryPolicy`

* `VlmProvider` Interface

  * `sendChat(profile, messages, images, overrides) -> VlmResult`

* `OpenRouterProvider` Implementierung

### Sanitizer pro Modellfamilie

Implementiere eine Policy-Schicht:

* GPT-5 / reasoning models:

  * Default: `reasoning.exclude=true` oder `effort=low`
  * höhere `max_tokens`
  * `temperature` optional (nur wenn gesetzt)
* klassische chat models:

  * `temperature` ok
  * `reasoning` weglassen

---

## 12) Copy-Paste Beispiele

### A) VLM Bildbeschreibung (kurz, TTS-freundlich)

```json
{
  "model": "openai/gpt-5-nano",
  "stream": false,
  "max_tokens": 800,
  "reasoning": { "exclude": true },
  "messages": [
    {
      "role": "system",
      "content": "Du bist ein Assistent für einen blinden Nutzer. Antworte kurz (max 3 Sätze), fokussiere auf Hindernisse, Verkehr, Umgebung."
    },
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "Beschreibe das Bild für sicheres Weiterfahren."},
        {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,<BASE64>"}}
      ]
    }
  ]
}
```

### B) VLM + Reasoning (wenn du es wirklich brauchst)

```json
{
  "model": "openai/gpt-5-nano",
  "stream": false,
  "max_tokens": 1200,
  "reasoning": { "effort": "low", "exclude": false },
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": [
      {"type": "text", "text": "..."},
      {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,<BASE64>"}}
    ]}
  ]
}
```

### C) Streaming (SSE) minimal

```json
{
  "model": "openai/gpt-4o-mini",
  "stream": true,
  "messages": [
    {"role": "user", "content": "Stream this."}
  ]
}
```

---

## 13) Checkliste (für Codex beim Implementieren)

* [ ] API Base URL & Endpoint korrekt
* [ ] Authorization Header gesetzt
* [ ] Optional: HTTP-Referer + X-Title gesetzt
* [ ] Multimodal: `messages[].content` als Array mit `text` + `image_url`
* [ ] Text vor Bild
* [ ] Base64 als `data:image/jpeg;base64,...`
* [ ] Keine `null`-Parameter senden
* [ ] Primär `message.content` ausgeben (TTS)
* [ ] Reasoning nur Debug/Telemetry
* [ ] Bei leerem `content` + Reasoning: Retry/Fallback (exclude/effort low/max_tokens höher)
* [ ] usage + finish_reason loggen
