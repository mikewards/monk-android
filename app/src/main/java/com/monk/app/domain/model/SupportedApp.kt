package com.monk.app.domain.model

/**
 * Supported messaging apps for auto-reply
 * 
 * These apps support notification reply actions (RemoteInput),
 * which means auto-reply works automatically without any
 * app-specific configuration.
 * 
 * Users can enable/disable specific apps in settings.
 */
enum class SupportedApp(
    val displayName: String,
    val packageName: String
) {
    WHATSAPP(
        displayName = "WhatsApp",
        packageName = "com.whatsapp"
    ),
    
    WHATSAPP_BUSINESS(
        displayName = "WhatsApp Business",
        packageName = "com.whatsapp.w4b"
    ),
    
    MESSENGER(
        displayName = "Messenger",
        packageName = "com.facebook.orca"
    ),
    
    MESSENGER_LITE(
        displayName = "Messenger Lite",
        packageName = "com.facebook.mlite"
    ),
    
    INSTAGRAM(
        displayName = "Instagram",
        packageName = "com.instagram.android"
    ),
    
    TELEGRAM(
        displayName = "Telegram",
        packageName = "org.telegram.messenger"
    ),
    
    TELEGRAM_X(
        displayName = "Telegram X",
        packageName = "org.thunderdog.chalern"
    ),
    
    MESSAGES(
        displayName = "Google Messages",
        packageName = "com.google.android.apps.messaging"
    ),
    
    SAMSUNG_MESSAGES(
        displayName = "Samsung Messages",
        packageName = "com.samsung.android.messaging"
    ),
    
    SIGNAL(
        displayName = "Signal",
        packageName = "org.thoughtcrime.securesms"
    ),
    
    SNAPCHAT(
        displayName = "Snapchat",
        packageName = "com.snapchat.android"
    ),
    
    DISCORD(
        displayName = "Discord",
        packageName = "com.discord"
    ),
    
    SLACK(
        displayName = "Slack",
        packageName = "com.Slack"
    ),
    
    TWITTER(
        displayName = "X (Twitter)",
        packageName = "com.twitter.android"
    ),
    
    LINKEDIN(
        displayName = "LinkedIn",
        packageName = "com.linkedin.android"
    ),
    
    TEAMS(
        displayName = "Microsoft Teams",
        packageName = "com.microsoft.teams"
    ),
    
    GROUPME(
        displayName = "GroupMe",
        packageName = "com.groupme.android"
    ),
    
    VIBER(
        displayName = "Viber",
        packageName = "com.viber.voip"
    ),
    
    WECHAT(
        displayName = "WeChat",
        packageName = "com.tencent.mm"
    ),
    
    LINE(
        displayName = "LINE",
        packageName = "jp.naver.line.android"
    ),
    
    KIK(
        displayName = "Kik",
        packageName = "kik.android"
    ),
    
    SKYPE(
        displayName = "Skype",
        packageName = "com.skype.raider"
    ),
    
    GOOGLE_CHAT(
        displayName = "Google Chat",
        packageName = "com.google.android.apps.dynamite"
    ),
    
    THREEMA(
        displayName = "Threema",
        packageName = "ch.threema.app"
    ),
    
    WIRE(
        displayName = "Wire",
        packageName = "com.wire"
    );
    
    companion object {
        /**
         * Find app by package name
         */
        fun fromPackageName(packageName: String): SupportedApp? {
            return entries.find { it.packageName == packageName }
        }
        
        /**
         * Get all package names
         */
        val allPackageNames: List<String>
            get() = entries.map { it.packageName }
            
        /**
         * Default enabled apps
         */
        val defaultEnabled: Set<String>
            get() = setOf(
                WHATSAPP.packageName,
                WHATSAPP_BUSINESS.packageName,
                MESSENGER.packageName,
                INSTAGRAM.packageName,
                TELEGRAM.packageName,
                MESSAGES.packageName,
                SIGNAL.packageName
            )
    }
}
