# Monk - Agent Context

Paste this into a new agent when starting work on this project:

---

This is Monk - an Android focus/productivity app.

GitHub: https://github.com/wardmic4/monk-android

Stack: Android (Kotlin, Jetpack Compose, Hilt DI)

Features:
- Focus mode with scheduled sessions
- Auto-reply to calls/texts during focus
- App blocking
- Notification filtering

Key files:
- app/src/main/java/com/monk/app/service/FocusService.kt - Core focus mode logic
- app/src/main/java/com/monk/app/service/AutoReplyService.kt - SMS/call auto-reply
- app/src/main/java/com/monk/app/service/NotificationListener.kt - Notification filtering
- app/src/main/java/com/monk/app/ui/screens/HomeScreen.kt - Main UI
- app/src/main/java/com/monk/app/ui/screens/HomeViewModel.kt - State management
- app/src/main/java/com/monk/app/data/datastore/PreferencesManager.kt - Settings persistence

Architecture:
- MVVM with Jetpack Compose
- Hilt for dependency injection
- DataStore for preferences
- Foreground service for focus mode

Commit and push to GitHub after every substantive change.
