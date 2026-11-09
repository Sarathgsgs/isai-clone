# Isai Music Player - Production ProGuard & R8 Optimization Rules

# ==============================================================================
# General & Stack Trace Retaining
# ==============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==============================================================================
# Media3 & Audio Playback Engine
# ==============================================================================
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.extractor.** { *; }
-dontwarn androidx.media3.**

# Maintain MediaSessionService binder stubs
-keepclassmembers class * extends androidx.media3.session.MediaSessionService {
    public <init>();
    public androidx.media3.session.MediaSession onGetSession(androidx.media3.session.MediaSession$ControllerInfo);
}

# ==============================================================================
# Room Database & DAOs
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep class com.isai.player.data.db.** { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-dontwarn androidx.room.paging.**

# ==============================================================================
# Dagger Hilt & Dependency Injection
# ==============================================================================
-keep class * extends dagger.hilt.internal.GeneratedComponentManager
-keep class * extends dagger.hilt.internal.ComponentManager
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.android.internal.builders.ActivityComponentBuilder
-keep class * implements dagger.hilt.android.internal.builders.ServiceComponentBuilder
-keep class * implements dagger.hilt.android.internal.builders.ViewModelComponentBuilder
-dontwarn dagger.hilt.**

# ==============================================================================
# Kotlinx Serialization
# ==============================================================================
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static *** INSTANCE;
}

# ==============================================================================
# Coil Image Loader & AndroidX Palette
# ==============================================================================
-keep class coil.** { *; }
-dontwarn coil.**
-keep class androidx.palette.graphics.** { *; }

# ==============================================================================
# Jetpack Glance & App Widgets
# ==============================================================================
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# ==============================================================================
# DataStore Preferences
# ==============================================================================
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ==============================================================================
# Coroutines & Bounded Dispatchers
# ==============================================================================
-dontwarn kotlinx.coroutines.**
