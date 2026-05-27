# ProGuard rules for LeChenMusic

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep Retrofit service method return types (critical for generic type resolution)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
# Keep all Subsonic model classes and their fields
-keep class com.lechenmusic.data.model.** { *; }
# Keep generic type info for Gson deserialization
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# Preserve Type information for generic classes
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Media3
-keep class androidx.media3.** { *; }

# Coil
-keep class coil.** { *; }

# Keep the API interface
-keep class com.lechenmusic.data.api.SubsonicApi { *; }
-keep class com.lechenmusic.data.api.ApiClient { *; }
-keep class com.lechenmusic.data.api.SafeJsonConverterFactory { *; }

# Compose - prevent R8 from stripping compose internals that cause crashes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin serialization and metadata
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep all Kotlin classes (prevents various Compose crashes in release)
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }
-keep class kotlin.reflect.** { *; }

# Prevent stripping of Compose Foundation text input classes
-keep class androidx.compose.foundation.text.** { *; }
-keep class androidx.compose.ui.text.** { *; }
-keep class androidx.compose.ui.text.input.** { *; }

# Keep AndroidX core classes
-keep class androidx.core.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.navigation.** { *; }
