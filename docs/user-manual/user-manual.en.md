# Owli‑AI Assist – User Manual (English)

Version: 2026-01-24

## 1. Overview

Owli‑AI Assist is an accessibility‑oriented assistant app. In **VLM mode**, you describe a scene on demand: you aim the camera, capture an image, and receive an answer (text + optional speech).

The **Offline Detector** is experimental and **hidden by default**. It can be enabled in Settings.

## 2. Requirements

- Android phone with a camera
- Internet connection (for VLM/LLM)
- Optional: Text‑to‑Speech (TTS) enabled

> **Privacy:** For VLM mode, images and text are sent to an AI service (e.g., via OpenRouter) to generate the response.

## 3. Quick Start

1. Open the app → it starts in **VLM mode**.
2. A **live camera preview** is shown.
3. Aim the camera.
4. Tap **“New scene”** → the app captures a **still image**.
5. Ask your question (text or voice) and tap **Send**.
6. To aim again: tap **“Reset”** → back to the live preview.

## 4. Main Screen (VLM)

### 4.1 Camera preview and still image

- **Preview (live):** helps you aim before capturing.
- **Still image:** after “New scene”, the captured image is displayed.

### 4.2 “New scene” and “Reset”

- **New scene:** clears the chat context, captures a new image, and shows it as still.
- **Reset:** clears the context and returns to the live preview.

### 4.3 Input bar (bottom)

The input bar contains:

- **Text field** (left): type your question/instruction.
- **Send** (middle/right): sends your message with the current image (and attachments).
- **Microphone** (right): voice input.

**Voice input:**
- **Tap:** dictate and insert text into the field.
- **Press & hold:** dictate and **send immediately** (if enabled in the UI).

### 4.4 More actions (three‑dot menu)

In **More actions**, you can find:

- **Repeat last answer:** speaks the last LLM answer again.
- **Add image:** captures an additional image and attaches it to the current chat.

### 4.5 Attachments (multiple images)

When you attach multiple images (e.g., close‑ups), an **attachment counter** is shown near the input bar.

- Tap the counter to manage attachments.
- Remove individual images if needed.

**When is “Add image” useful?**
- When the AI asks for a detail (“please zoom in”).
- When you need multiple angles.

## 5. Settings (VLM)

### 5.1 Choose a profile

The app can offer multiple VLM profiles (e.g., “short”, “detailed”). Choose the one that fits your use case.

### 5.2 Language and speech output

- **Language:** System / German / English
- **TTS:** On/Off, speech rate, pitch
- Optional: **Streaming TTS** (speaks while the answer is arriving).

### 5.3 Developer / Experimental

- **Enable Offline Detector (Experimental):** reveals the offline detector mode and its settings.
- Note: This mode is intended for developers and can be more complex.

## 6. Offline Detector (experimental)

When enabled, a separate mode is available that continuously detects objects and can provide audio cues. It is **not** part of the beta flow.

## 7. Accessibility tips (TalkBack)

- Controls are arranged in a predictable order.
- Use swipe navigation to move between controls.
- Button labels are descriptive; rely on the announced text.

## 8. Troubleshooting

- **No answer:** check your internet connection.
- **Voice input not working:** ensure voice recognition is available on your device.
- **Camera not working:** check camera permission in Android settings.
- **Speech sounds choppy:** disable streaming TTS or reduce speech rate.

## 9. Feedback

Feedback is very valuable: which actions do you use most? Where is navigation unclear?

---
*This manual describes the current beta behavior. Menus/features may differ slightly between builds.*
