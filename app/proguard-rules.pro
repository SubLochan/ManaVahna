# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers and source file names for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Application, Activities, and ViewModels
-keep class com.manavahana.ManaVahanaApplication { *; }
-keep class com.manavahana.MainActivity { *; }
-keep class com.manavahana.ui.ManaVahanaViewModel { *; }
-keep class com.manavahana.ui.ManaVahanaViewModelFactory { *; }
-keep class com.manavahana.ui.PlayStoreVersionFetcher { *; }
-keep class com.manavahana.ui.AppUpdateHelper { *; }

# Keep Room Database, Entities, and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.manavahana.data.database.** { *; }
-keep class com.manavahana.data.model.** { *; }
-keep class com.manavahana.data.dao.** { *; }
-keep class com.manavahana.data.repository.** { *; }
-keep class com.manavahana.data.preferences.** { *; }

# Keep WorkManager Workers with constructors for system instantiation
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.manavahana.worker.** { *; }

# Keep Moshi JSON & Serialization classes
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }

# Keep OkHttp for background version checks
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Google Play In-App Update API
-keep class com.google.android.play.core.** { *; }

