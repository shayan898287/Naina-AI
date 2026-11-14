# Naina AI — Android starter project

This is a native Android Studio starter app for the Naina AI companion.

## Included in this first prototype
- Dark chat interface
- Naina companion identity
- Roman Urdu / English text input
- Android speech-to-text input (device/provider support required)
- Android TextToSpeech spoken demo replies
- Basic local demo reply logic

## Important current limitation
This starter is **not yet connected to a real AI model**. Replies are local demo responses, not an intelligent LLM conversation. The voice feature is speech input plus spoken replies, not a live phone-call experience. A real AI backend and a secure server-side API integration are needed for natural conversations and low-latency two-way voice.

## Build an APK
1. Install Android Studio (recent stable version) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync and install any prompted SDK components.
4. Connect an Android phone with USB debugging enabled, or use an emulator.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Android Studio will show the generated APK location, normally `app/build/outputs/apk/debug/app-debug.apk`.

## Next development steps
1. Add a secure backend endpoint for AI chat (never embed a private API key in the APK).
2. Connect a conversational AI model with conversation history.
3. Add natural female voice streaming / interruption handling for real-time voice conversations.
4. Add avatar image, settings, consent, privacy controls, and production testing.


## Added in version 1 prototype update
- Three conversation modes: Sweet Girlfriend, Romantic & Flirty, Adult 18+.
- Adult 18+ mode asks the user to create a private four-digit PIN on first use and requires it on later unlocks.
- Adult mode is labeled for adults 18+ and uses respectful, consensual conversational demo replies.

Note: The PIN is a basic local app lock, not high-security encryption. It does not protect app data from someone with device-level access. The conversation engine is still a local demo, not a connected AI model.
