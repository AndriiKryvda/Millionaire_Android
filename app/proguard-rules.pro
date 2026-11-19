# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vibecoding.millionaire.**$$serializer { *; }
-keepclassmembers class com.vibecoding.millionaire.** {
    *** Companion;
}
-keepclasseswithmembers class com.vibecoding.millionaire.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
-dontwarn com.google.errorprone.annotations.**

# --- Compose navigation type-safe routes (kept by name for serialization) ---
-keep class com.vibecoding.millionaire.ui.navigation.** { *; }