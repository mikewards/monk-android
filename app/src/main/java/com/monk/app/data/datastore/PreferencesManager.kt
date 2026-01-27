package com.monk.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "monk_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    // ═══════════════════════════════════════════════════════════════
    // KEYS
    // ═══════════════════════════════════════════════════════════════

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val REPLY_MESSAGE = stringPreferencesKey("reply_message")
        val REPLY_COOLDOWN_MINUTES = intPreferencesKey("reply_cooldown_minutes")
        val ENABLED_APPS = stringSetPreferencesKey("enabled_apps")
        val WHITELISTED_CONTACTS = stringSetPreferencesKey("whitelisted_contacts")
        val SILENCE_NOTIFICATIONS = booleanPreferencesKey("silence_notifications")
        val AUTO_REPLY_ENABLED = booleanPreferencesKey("auto_reply_enabled")
        val NOTIFICATION_HINT_DISMISSED = booleanPreferencesKey("notification_hint_dismissed")
        val DEEP_FOCUS_ENABLED = booleanPreferencesKey("deep_focus_enabled")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
    }

    // ═══════════════════════════════════════════════════════════════
    // ONBOARDING
    // ═══════════════════════════════════════════════════════════════

    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REPLY MESSAGE
    // ═══════════════════════════════════════════════════════════════

    val replyMessage: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.REPLY_MESSAGE] 
                ?: "I'm currently in monk mode and will respond later."
        }

    suspend fun setReplyMessage(message: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLY_MESSAGE] = message
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REPLY COOLDOWN
    // ═══════════════════════════════════════════════════════════════

    val replyCooldownMinutes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.REPLY_COOLDOWN_MINUTES] ?: 5
        }

    suspend fun setReplyCooldownMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLY_COOLDOWN_MINUTES] = minutes
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ENABLED APPS
    // ═══════════════════════════════════════════════════════════════

    val enabledApps: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ENABLED_APPS] ?: defaultEnabledApps
        }

    suspend fun setEnabledApps(apps: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_APPS] = apps
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // WHITELISTED CONTACTS
    // ═══════════════════════════════════════════════════════════════

    val whitelistedContacts: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.WHITELISTED_CONTACTS] ?: emptySet()
        }

    suspend fun setWhitelistedContacts(contacts: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WHITELISTED_CONTACTS] = contacts
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FEATURE TOGGLES
    // ═══════════════════════════════════════════════════════════════

    val silenceNotifications: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SILENCE_NOTIFICATIONS] ?: true
        }

    suspend fun setSilenceNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SILENCE_NOTIFICATIONS] = enabled
        }
    }

    val autoReplyEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_REPLY_ENABLED] ?: true
        }

    suspend fun setAutoReplyEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_REPLY_ENABLED] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION HINT (dismissible tip about app notifications)
    // ═══════════════════════════════════════════════════════════════

    val notificationHintDismissed: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_HINT_DISMISSED] ?: false
        }

    suspend fun setNotificationHintDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_HINT_DISMISSED] = dismissed
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DEEP FOCUS (auto-reopen when user tries to leave)
    // ═══════════════════════════════════════════════════════════════

    val deepFocusEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEEP_FOCUS_ENABLED] ?: false
        }

    suspend fun setDeepFocusEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEEP_FOCUS_ENABLED] = enabled
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DO NOT DISTURB (silence phone during focus)
    // ═══════════════════════════════════════════════════════════════

    val dndEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DND_ENABLED] ?: false
        }

    suspend fun setDndEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_ENABLED] = enabled
        }
    }

    companion object {
        // Default enabled apps
        val defaultEnabledApps = setOf(
            "com.whatsapp",
            "com.facebook.orca",
            "com.instagram.android",
            "com.google.android.apps.messaging",
            "org.telegram.messenger"
        )
    }
}
