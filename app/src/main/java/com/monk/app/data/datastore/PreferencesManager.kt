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

/**
 * Manages all app preferences using DataStore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
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
        val FOCUS_ACTIVE = booleanPreferencesKey("focus_active")
        val FOCUS_START_TIME = longPreferencesKey("focus_start_time")
        val FOCUS_DURATION_MS = longPreferencesKey("focus_duration_ms")
    }

    // Onboarding

    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    // Reply message

    val replyMessage: Flow<String> = dataStore.data
        .catchIoException()
        .map { it[Keys.REPLY_MESSAGE] ?: DEFAULT_REPLY_MESSAGE }

    suspend fun setReplyMessage(message: String) {
        dataStore.edit { it[Keys.REPLY_MESSAGE] = message }
    }

    // Reply cooldown

    val replyCooldownMinutes: Flow<Int> = dataStore.data
        .catchIoException()
        .map { it[Keys.REPLY_COOLDOWN_MINUTES] ?: 5 }

    suspend fun setReplyCooldownMinutes(minutes: Int) {
        dataStore.edit { it[Keys.REPLY_COOLDOWN_MINUTES] = minutes }
    }

    // Enabled apps

    val enabledApps: Flow<Set<String>> = dataStore.data
        .catchIoException()
        .map { it[Keys.ENABLED_APPS] ?: DEFAULT_ENABLED_APPS }

    suspend fun setEnabledApps(apps: Set<String>) {
        dataStore.edit { it[Keys.ENABLED_APPS] = apps }
    }

    // Whitelisted contacts

    val whitelistedContacts: Flow<Set<String>> = dataStore.data
        .catchIoException()
        .map { it[Keys.WHITELISTED_CONTACTS] ?: emptySet() }

    suspend fun setWhitelistedContacts(contacts: Set<String>) {
        dataStore.edit { it[Keys.WHITELISTED_CONTACTS] = contacts }
    }

    // Feature toggles

    val silenceNotifications: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.SILENCE_NOTIFICATIONS] ?: true }

    suspend fun setSilenceNotifications(enabled: Boolean) {
        dataStore.edit { it[Keys.SILENCE_NOTIFICATIONS] = enabled }
    }

    val autoReplyEnabled: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.AUTO_REPLY_ENABLED] ?: true }

    suspend fun setAutoReplyEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_REPLY_ENABLED] = enabled }
    }

    // UI state

    val notificationHintDismissed: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.NOTIFICATION_HINT_DISMISSED] ?: false }

    suspend fun setNotificationHintDismissed(dismissed: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATION_HINT_DISMISSED] = dismissed }
    }

    // Deep focus

    val deepFocusEnabled: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.DEEP_FOCUS_ENABLED] ?: false }

    suspend fun setDeepFocusEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DEEP_FOCUS_ENABLED] = enabled }
    }

    // Do Not Disturb

    val dndEnabled: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.DND_ENABLED] ?: false }

    suspend fun setDndEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DND_ENABLED] = enabled }
    }

    // Focus session state (for boot resume)

    val focusActive: Flow<Boolean> = dataStore.data
        .catchIoException()
        .map { it[Keys.FOCUS_ACTIVE] ?: false }

    val focusStartTime: Flow<Long> = dataStore.data
        .catchIoException()
        .map { it[Keys.FOCUS_START_TIME] ?: 0L }

    val focusDurationMs: Flow<Long> = dataStore.data
        .catchIoException()
        .map { it[Keys.FOCUS_DURATION_MS] ?: 0L }

    suspend fun setFocusSession(active: Boolean, startTime: Long = 0, durationMs: Long = 0) {
        dataStore.edit {
            it[Keys.FOCUS_ACTIVE] = active
            it[Keys.FOCUS_START_TIME] = startTime
            it[Keys.FOCUS_DURATION_MS] = durationMs
        }
    }

    private fun Flow<Preferences>.catchIoException(): Flow<Preferences> = catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    companion object {
        const val DEFAULT_REPLY_MESSAGE = "I'm currently in monk mode and will respond later."

        val DEFAULT_ENABLED_APPS = setOf(
            "com.whatsapp",
            "com.facebook.orca",
            "com.instagram.android",
            "com.google.android.apps.messaging",
            "org.telegram.messenger"
        )
    }
}
