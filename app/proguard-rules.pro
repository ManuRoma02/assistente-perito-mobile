# Proguard Rules for PeritoAI (On-Device Studio Peritale)

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}

# Native JNI Bridge (Llamatik / LlamaBridge)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.llamatik.** { *; }
-keep class com.example.assistenteperitomobile.data.** { *; }

# ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# AndroidX WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# AndroidX Navigation & Compose
-dontwarn androidx.compose.**
-dontwarn kotlinx.coroutines.**
