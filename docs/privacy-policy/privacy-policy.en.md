# Owli-AI Assist - Privacy Policy

Version: 2026-04-16

## 1. Purpose of the app

Owli-AI Assist is an accessibility-oriented assistant app for image descriptions and follow-up questions about a captured scene.

## 2. When data leaves the device

Images and text are sent only after your explicit action, for example after `New scene` or when sending a follow-up question.

## 3. Production mode: Owli backend

In normal production mode, the app uses the Owli backend.

- For `New scene`, the app sends a captured image to `https://api.owli-ai.com`.
- For follow-up questions, the app sends the question text to the Owli backend.
- In this mode, the app does not use a direct OpenRouter user key on the device.

## 4. Optional direct mode: your own OpenRouter key (BYOK)

If you explicitly activate direct OpenRouter BYOK mode in Settings, the app sends requests directly to OpenRouter.

- In this mode, the scene image, optional additional image attachments, and your question text may be sent directly to OpenRouter.
- This mode is used only if you select it yourself and a custom key is stored.

## 5. Data stored locally

The app stores the following locally on the device:

- language settings
- TTS settings
- the selected VLM transport mode
- the selected VLM profile
- an optional custom OpenRouter key only as separate encrypted local storage in the Android Keystore context
- a cache of public profile metadata from the Owli backend

The optional custom OpenRouter key is not stored as plaintext in normal app settings.

## 6. What stays local

The live camera preview runs locally on the device. Data is sent only after an explicit user action for a VLM request.

## 7. Profile and runtime data

The app can load public profile information from `https://api.owli-ai.com/api/v1/profiles` and cache it locally so profile selection remains available during temporary backend issues. This cache does not contain user keys.

## 8. Debug and development mode

An embedded app key is intended only for debug or development builds and is not a normal production path in release builds.

## 9. Contact

For questions about app behavior or privacy-related notes: `feedback@owli-ai.com`
