# Monk

Focus without distractions. Monk silences notifications and sends auto-replies so you can concentrate on what matters.

## Features

- **Focus Mode** - One tap to enter distraction-free mode
- **Auto-Reply** - Automatically respond to messages while you're busy
- **Do Not Disturb** - Silence your phone completely during focus
- **Deep Focus** - Prevents you from leaving the app during sessions
- **Whitelist** - Let important contacts still reach you
- **Timer** - Set focus duration or go indefinite
- **Cooldown** - Avoid spamming the same person with replies

## Privacy

Monk is built with a zero-knowledge architecture:

- No internet permission - data never leaves your device
- No analytics or tracking
- No cloud services
- Messages processed in memory only, never stored

## Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| Notification Access | Yes | Detect messages and send auto-replies |
| Do Not Disturb | No | Silence phone during focus |
| Accessibility | No | Deep focus mode (prevents app switching) |
| Contacts | No | Whitelist feature |

## Building

```bash
git clone https://github.com/wardmic4/monk-android.git
cd monk-android
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- Kotlin
- Jetpack Compose
- Hilt
- DataStore
- Coroutines

## Project Structure

```
app/src/main/java/com/monk/app/
├── data/         # DataStore preferences
├── di/           # Hilt modules
├── domain/       # Models
├── receiver/     # Boot receiver
├── service/      # NotificationListener, AccessibilityService, FocusService
├── ui/           # Compose screens
└── util/         # Permission helpers
```

## License

MIT

