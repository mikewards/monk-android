# Monk - Agent Context

Paste this into a new agent when starting work on this project.

---

## Overview

Monk is an Android focus/productivity app that silences notifications and sends automatic replies during focus sessions. Privacy-first: works completely offline with no data transmission.

**GitHub:** https://github.com/mikewards/monk-android

**Stack:** Android (Kotlin, Jetpack Compose, Hilt DI, DataStore)

## Directory Structure

```
app/src/main/java/com/monk/app/
  ui/
    screens/          HomeScreen, SettingsScreen, AppsScreen, ContactsScreen, OnboardingScreen
    navigation/       NavGraph
    theme/            Color, Theme, Type
  service/            FocusService, NotificationListener, AutoReplyService
  receiver/           BootReceiver
  data/
    datastore/        PreferencesManager
  domain/
    model/            FocusState, SupportedApp
  di/                 AppModule (Hilt)
  util/               PermissionHelper
```

## Screens

### HomeScreen
- Focus orb (BEGIN/END button)
- Timer display (countdown or elapsed)
- Duration selector: 30m, 1h, 2h, Custom, Indefinite
- Stats: notifications silenced, replies sent
- Haptic feedback on start/stop

### SettingsScreen
- Auto-reply message editor
- Cooldown slider (1-60 minutes)
- Toggles: Silence Phone (DND), Stay in Monk (Deep Focus)
- Links to: Enabled Apps, Whitelisted Contacts, Permissions

### AppsScreen
- Lists 28 supported messaging apps
- Toggle per app (enabled/disabled for auto-reply)
- Default enabled: WhatsApp, Messenger, Instagram, Telegram, Messages, Signal

### ContactsScreen
- Privacy-first: stores only contact IDs, not names
- Add/remove whitelisted contacts
- Contact picker with search
- **NOTE:** Whitelist UI complete but logic not wired up (see incomplete features)

### OnboardingScreen
- 3-page introduction
- Requests Notification Access permission
- Shown only once

## Services

### FocusService (Foreground Service)
**File:** `service/FocusService.kt`

Core focus mode manager:
- Starts/stops focus sessions
- Timer support (auto-stop after duration)
- Boot resume (persists session state via DataStore)
- Coordinates NotificationListener and AutoReplyService
- DND integration
- Deep Focus toggle
- Foreground notification with stats (updates every 60s)

**State:**
- `isRunning` - Focus active
- `focusStartTimeMs` - Session start timestamp
- `focusDurationMs` - Timer duration (0 = indefinite)
- `deepFocusEnabled` - Prevent app switching
- `dndEnabled` - Do Not Disturb

### NotificationListener (NotificationListenerService)
**File:** `service/NotificationListener.kt`

Auto-reply engine:
- Intercepts notifications from enabled messaging apps
- Extracts sender/message from notification extras
- Checks cooldown (default 5 min per sender)
- Sends reply via RemoteInput action
- Cancels notification after processing
- Privacy: processes in memory only, no persistence

**State:**
- `isFocusModeActive` - Controlled by FocusService
- `replyMessage` - Configurable auto-reply text
- `cooldownMinutes` - Time between replies to same sender
- `enabledApps` - Set of enabled package names
- `notificationsSilenced` - Counter
- `repliesSent` - Counter

### AutoReplyService (AccessibilityService)
**File:** `service/AutoReplyService.kt`

Two purposes:
1. **Fallback auto-reply** - For apps without RemoteInput support
   - Receives broadcast from NotificationListener
   - Finds text field via accessibility tree
   - Sets text and clicks send button
2. **Deep Focus** - Prevents user from leaving Monk
   - Monitors window changes
   - Relaunches Monk if user switches apps

## Auto-Reply Flow

**Primary Method (RemoteInput):**
1. Notification arrives from enabled app
2. NotificationListener extracts sender/message
3. Checks cooldown via AutoReplyManager
4. Finds RemoteInput action in notification
5. Sends reply via `RemoteInput.addResultsToIntent()`
6. Cancels notification

**Fallback Method (Accessibility):**
1. NotificationListener broadcasts `ACTION_SEND_REPLY`
2. AutoReplyService receives, stores as PendingReply
3. Waits for app window to open
4. Finds editable field via accessibility tree
5. Sets text, finds send button, clicks

## Permissions

**Required:**
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` - FocusService
- `POST_NOTIFICATIONS` - Android 13+ notifications
- `RECEIVE_BOOT_COMPLETED` - BootReceiver

**Optional:**
- `READ_CONTACTS` - Whitelist feature
- `ACCESS_NOTIFICATION_POLICY` - DND control

**Service Permissions (declared in manifest):**
- `BIND_NOTIFICATION_LISTENER_SERVICE` - NotificationListener
- `BIND_ACCESSIBILITY_SERVICE` - AutoReplyService

**No Internet Permission** - App works completely offline

## Data Persistence (DataStore)

**File:** `data/datastore/PreferencesManager.kt`

Stored preferences:
- Onboarding completion
- Auto-reply message
- Cooldown minutes
- Enabled apps (set)
- Whitelisted contact IDs (set)
- Deep focus enabled
- DND enabled
- Focus session state (for boot resume)

## ViewModel (`HomeViewModel.kt`)

**State:**
- `focusState: StateFlow<FocusState>` - Active, startedAt, scheduledEndAt, stats
- `selectedDuration: StateFlow<FocusDuration>`

**Behavior:**
- Polls FocusService every 1 second
- Loads preferences on startup
- Exposes `startFocus()` and `stopFocus()` actions

## Incomplete Features

### Whitelist Logic (UI done, logic missing)
**Location:** `NotificationListener.kt`

The whitelist contacts UI is complete but the logic to skip auto-reply for whitelisted contacts is not implemented.

**To implement:**
1. Load whitelisted contact IDs in NotificationListener
2. Extract phone number/identifier from notification
3. Match against whitelist
4. Skip auto-reply if contact is whitelisted

## Architecture Notes

- **MVVM** with Jetpack Compose
- **Hilt** for dependency injection
- **DataStore** for preferences (no Room DB needed)
- **Privacy-first:** No internet, no analytics, no message storage
- **Foreground service** for reliable background operation

## Supported Apps (28 total)

WhatsApp, Messenger, Instagram, Telegram, Messages, Signal, Discord, Slack, Teams, Snapchat, LINE, WeChat, Viber, KakaoTalk, iMessage (Samsung), Skype, Threema, Wire, Element, GroupMe, TextNow, Textra, Pulse SMS, YAATA, Chomp SMS, Handcent, GO SMS, Facebook Lite

## Commit Strategy

Commit and push to GitHub after every substantive change.
