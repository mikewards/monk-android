# Monk ProGuard Rules
# ═══════════════════════════════════════════════════════════════════════════
# PRIVACY CRITICAL: Strip ALL logging in release builds
# Even accidental sensitive logs will not appear in production
# ═══════════════════════════════════════════════════════════════════════════
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Keep accessibility service
-keep class com.monk.app.service.AutoReplyService { *; }
-keep class com.monk.app.service.NotificationListener { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes
-keep class com.monk.app.domain.model.** { *; }
