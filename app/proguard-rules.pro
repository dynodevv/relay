# ProGuard rules for Relay

# Keep attributes
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# General
-keep public class * { public protected *; }
-dontwarn java.lang.management.**
-dontwarn org.joda.time.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.http.**

# Google Tink / EncryptedSharedPreferences
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Entity class * { <init>(...); }

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Markdown renderer
-keep class com.mikepenz.markdown.** { *; }
-dontwarn com.mikepenz.markdown.**

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Reorderable
-keep class sh.calvin.reorderable.** { *; }
-dontwarn sh.calvin.reorderable.**
