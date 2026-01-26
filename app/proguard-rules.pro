# Monk ProGuard Rules

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
-keep class com.monk.app.data.local.entity.** { *; }
