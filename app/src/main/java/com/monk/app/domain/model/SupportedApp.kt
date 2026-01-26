package com.monk.app.domain.model

/**
 * Represents messaging apps that Monk can send auto-replies to.
 * Each app has specific UI element identifiers for the accessibility service.
 */
enum class SupportedApp(
    val packageName: String,
    val displayName: String,
    val iconRes: Int? = null, // TODO: Add icons
    val inputFieldId: String? = null,
    val sendButtonId: String? = null,
    val sendButtonContentDesc: String? = null
) {
    WHATSAPP(
        packageName = "com.whatsapp",
        displayName = "WhatsApp",
        inputFieldId = "com.whatsapp:id/entry",
        sendButtonId = "com.whatsapp:id/send",
        sendButtonContentDesc = "Send"
    ),
    WHATSAPP_BUSINESS(
        packageName = "com.whatsapp.w4b",
        displayName = "WhatsApp Business",
        inputFieldId = "com.whatsapp.w4b:id/entry",
        sendButtonId = "com.whatsapp.w4b:id/send",
        sendButtonContentDesc = "Send"
    ),
    MESSENGER(
        packageName = "com.facebook.orca",
        displayName = "Messenger",
        sendButtonContentDesc = "Send"
    ),
    MESSENGER_LITE(
        packageName = "com.facebook.mlite",
        displayName = "Messenger Lite",
        sendButtonContentDesc = "Send"
    ),
    INSTAGRAM(
        packageName = "com.instagram.android",
        displayName = "Instagram",
        sendButtonContentDesc = "Send"
    ),
    TELEGRAM(
        packageName = "org.telegram.messenger",
        displayName = "Telegram",
        inputFieldId = "org.telegram.messenger:id/chat_input_text",
        sendButtonId = "org.telegram.messenger:id/send_button"
    ),
    TELEGRAM_X(
        packageName = "org.thunderdog.challegram",
        displayName = "Telegram X",
        sendButtonContentDesc = "Send"
    ),
    SIGNAL(
        packageName = "org.thoughtcrime.securesms",
        displayName = "Signal",
        sendButtonContentDesc = "Send"
    ),
    MESSAGES(
        packageName = "com.google.android.apps.messaging",
        displayName = "Messages",
        sendButtonContentDesc = "Send SMS"
    ),
    SAMSUNG_MESSAGES(
        packageName = "com.samsung.android.messaging",
        displayName = "Samsung Messages",
        sendButtonContentDesc = "Send"
    ),
    DISCORD(
        packageName = "com.discord",
        displayName = "Discord",
        sendButtonContentDesc = "Send"
    ),
    SLACK(
        packageName = "com.Slack",
        displayName = "Slack",
        sendButtonContentDesc = "Send"
    ),
    TWITTER(
        packageName = "com.twitter.android",
        displayName = "X (Twitter)",
        sendButtonContentDesc = "Send"
    ),
    SNAPCHAT(
        packageName = "com.snapchat.android",
        displayName = "Snapchat",
        sendButtonContentDesc = "Send"
    );

    companion object {
        /**
         * Find a supported app by its package name
         */
        fun fromPackageName(packageName: String): SupportedApp? {
            return entries.find { it.packageName == packageName }
        }

        /**
         * Check if a package is a supported messaging app
         */
        fun isSupported(packageName: String): Boolean {
            return entries.any { it.packageName == packageName }
        }

        /**
         * Get default apps to enable (most common messaging apps)
         */
        fun defaultEnabled(): Set<SupportedApp> {
            return setOf(WHATSAPP, MESSENGER, INSTAGRAM, MESSAGES, TELEGRAM)
        }
    }
}
