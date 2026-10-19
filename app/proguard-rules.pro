# Proguard Rules for PeritoAI (On-Device Studio Peritale)

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlin Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
    <init>(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# AndroidX WorkManager & Room
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.work.ListenableWorker {
    <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>(...);
    public <init>();
}
-dontwarn androidx.work.impl.**

# App Package & ViewModels
-keep class com.example.assistenteperitomobile.** { *; }
-keepclassmembers class com.example.assistenteperitomobile.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Native JNI Bridge (Llamatik / LlamaBridge)
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.llamatik.** { *; }

# ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# AndroidX Navigation & Compose
-dontwarn androidx.compose.**
-dontwarn kotlinx.coroutines.**

