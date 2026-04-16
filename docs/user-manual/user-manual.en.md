# Owli-AI Assist - User Manual (English)

Version: 2026-04-16

## 1. Overview

Owli-AI Assist is an accessibility-oriented assistant app. In VLM mode, you aim the camera, capture an image on demand, and receive an answer as text plus optional speech output.

## 2. Requirements

- Android phone with a camera
- Internet connection (for VLM/LLM)
- Optional: Text-to-Speech (TTS) enabled

> Privacy: Images and text are sent to an AI service only after you explicitly start a scene request or send a follow-up question.

## 3. Quick Start

1. Open the app.
2. Allow camera permission.
3. Aim the camera in the live preview.
4. Tap `New scene` to capture a still image.
5. Ask your question by text and tap `Send`.
6. Or tap the microphone to insert speech, or long-press it to send speech immediately.
7. Tap `Reset` to close the current chat and return to the live preview.

## 4. Main Screen

- Live preview: helps you aim before capture.
- Still image: shown after `New scene`.
- App menu in the top bar: opens Settings, Privacy Policy, Help, and About.
- In-chat actions: use `More actions` to repeat the last answer or prepare another image for the current chat. Then aim the camera and confirm the extra image with `Capture image`.
- Attachments: manage additional images for the current chat.
- Auto: if the active VLM profile supports auto scan, you can toggle periodic `New scene` requests on or off.

## 5. Settings

- Choose a VLM profile. The available profiles depend on the active transport mode.
- Manage the VLM transport:
  - `Owli backend` is the normal production path.
  - `Custom OpenRouter key` uses direct BYOK mode with your locally encrypted stored key.
  - In debug or development builds, an embedded app key may also be available.
- In `Manage key`, you can:
  - save a custom OpenRouter key manually,
  - import it from a QR code,
  - delete the stored key,
  - switch the active transport,
  - request OpenRouter key info for direct OpenRouter modes.
- Set language to System / German / English.
- Configure TTS on/off, speech rate, pitch, and optional streaming TTS.

## 6. Accessibility Tips

- Controls follow a stable order for TalkBack navigation.
- Use swipe navigation to move between controls.
- Button labels are descriptive and should be read correctly by screen readers.

## 7. Troubleshooting

- No answer: check your internet connection.
- Voice input not working: ensure speech recognition is available on your device.
- Camera not working: check camera permission in Android settings.
- Choppy speech: disable streaming TTS or reduce speech rate.
- `Auto` is missing: the currently selected VLM profile does not support auto scan.
- Additional image for a follow-up does not work: extra follow-up images are currently available only in direct OpenRouter BYOK mode, not in Owli backend mode.

## 8. Feedback

Feedback is valuable: which actions do you use most, and where is navigation unclear?
