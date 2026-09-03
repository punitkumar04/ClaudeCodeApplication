# Strip Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.punitkumar.gruhkharch.**$$serializer { *; }
-keepclassmembers class com.punitkumar.gruhkharch.** { *** Companion; }
-keepclasseswithmembers class com.punitkumar.gruhkharch.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keepclasseswithmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }

# Keep data classes used with Firestore
-keepclassmembers class com.punitkumar.gruhkharch.domain.model.** { *; }
-keepclassmembers class com.punitkumar.gruhkharch.data.local.entity.** { *; }
