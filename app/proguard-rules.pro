# Add project specific ProGuard rules here.

# Keep Room entities
-keep class com.bkiptv.app.db.entity.** { *; }

# Keep data models
-keep class com.bkiptv.app.data.model.** { *; }
-keep class com.bkiptv.app.data.parser.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Coil
-keep class coil.** { *; }

# ML Kit Barcode
-keep class com.google.mlkit.** { *; }
