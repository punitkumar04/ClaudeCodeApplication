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
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }

# Keep data classes used with Firestore
-keepclassmembers class com.punitkumar.gruhkharch.domain.model.** { *; }
-keepclassmembers class com.punitkumar.gruhkharch.data.local.entity.** { *; }
