# Monk 🧘

**Focus without distractions.** Monk silences notifications and sends auto-replies so you can concentrate on what matters.

## Features

- **Focus Mode Toggle** - One-tap to enter distraction-free mode
- **Auto-Reply** - Automatically respond to messages with a custom "busy" message
- **Notification Silencing** - Mute incoming notifications during focus sessions
- **Multi-App Support** - Works with WhatsApp, Messenger, Instagram, Telegram, SMS, and more
- **Whitelist Contacts** - Let important people still reach you
- **Session Timer** - Set focus duration or go indefinite
- **Reply Cooldown** - Avoid spamming the same person

## Supported Apps

| App | Auto-Reply | Silence |
|-----|------------|---------|
| WhatsApp | ✅ | ✅ |
| WhatsApp Business | ✅ | ✅ |
| Messenger | ✅ | ✅ |
| Instagram | ✅ | ✅ |
| Telegram | ✅ | ✅ |
| Signal | ✅ | ✅ |
| Discord | ✅ | ✅ |
| Slack | ✅ | ✅ |
| SMS/Messages | ✅ | ✅ |

## Tech Stack

- **Kotlin** - Modern Android development
- **Jetpack Compose** - Declarative UI
- **Hilt** - Dependency injection
- **Room** - Local database
- **Coroutines & Flow** - Async operations

## Permissions

Monk requires the following permissions:

| Permission | Purpose |
|------------|---------|
| **Notification Access** | Read incoming messages to trigger auto-replies |
| **Accessibility Service** | Interact with messaging apps to send replies |
| **SMS** | Send/receive SMS auto-replies |
| **Contacts** | Whitelist contacts feature |

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/
├── src/main/java/com/monk/app/
│   ├── ui/           # Compose UI screens and components
│   ├── service/      # Background services
│   ├── domain/       # Business logic and models
│   ├── data/         # Data layer (Room, DataStore)
│   ├── di/           # Hilt modules
│   ├── receiver/     # Broadcast receivers
│   └── util/         # Utilities
```

## Privacy

- All data stays on your device
- No analytics or tracking
- No cloud services required
- Messages are processed locally only

## License

MIT License - see LICENSE file for details.

---

Made with 🧘 for focused minds.
