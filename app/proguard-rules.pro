# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.punitkumar.gruhkharch.**$$serializer { *; }
-keepclassmembers class com.punitkumar.gruhkharch.** { *** Companion; }
-keepclasseswithmembers class com.punitkumar.gruhkharch.** { kotlinx.serialization.KSerializer serializer(...); }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
