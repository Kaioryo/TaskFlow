# ====================================================================================================
# TaskFlow ProGuard Configuration
# ====================================================================================================

# ====================================================================================================
# BASIC SETTINGS
# ====================================================================================================

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep generic signature for reflection
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# Keep exception info
-keepattributes Exceptions

# Keep inner classes
-keepattributes InnerClasses

# Keep encryption attributes
-keepattributes EnclosingMethod

# ====================================================================================================
# KOTLIN
# ====================================================================================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Kotlin companion objects
-keep class **$Companion {
    *;
}

# Kotlin reflection
-keep class kotlin.reflect.** { *; }
-keep class kotlin.jvm.internal.** { *; }

# ====================================================================================================
# ANDROID
# ====================================================================================================

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Fragment

# Keep custom view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep view methods
-keepclassmembers class * extends android.view.View {
    void set*(***);
    *** get*();
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====================================================================================================
# TASKFLOW APP SPECIFIC
# ====================================================================================================

# Keep all TaskFlow models (data classes)
-keep class com.taskflow.app.Task { *; }
-keep class com.taskflow.app.QuoteResponse { *; }
-keep class com.taskflow.app.TaskEntity { *; }

# Keep TaskFlow Application class
-keep class com.taskflow.app.TaskFlowApplication { *; }

# Keep all Activities
-keep class com.taskflow.app.LoginActivity { *; }
-keep class com.taskflow.app.RegisterActivity { *; }
-keep class com.taskflow.app.MainActivity { *; }
-keep class com.taskflow.app.AddTaskActivity { *; }
-keep class com.taskflow.app.TaskDetailActivity { *; }

# Keep BroadcastReceivers
-keep class com.taskflow.app.ReminderReceiver { *; }
-keep class com.taskflow.app.TaskActionReceiver { *; }

# Keep Fragment classes
-keep class com.taskflow.app.HomeFragment { *; }
-keep class com.taskflow.app.TaskFragment { *; }
-keep class com.taskflow.app.ProfileFragment { *; }

# Keep QuoteManager singleton
-keep class com.taskflow.app.QuoteManager { *; }

# ====================================================================================================
# FIREBASE
# ====================================================================================================

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep interface com.google.firebase.auth.** { *; }

# Keep Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep interface com.google.firebase.firestore.** { *; }

# Keep Firebase models
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Keep Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }

# FirebaseUI
-keep class com.firebase.ui.** { *; }
-dontwarn com.firebase.ui.**

# ====================================================================================================
# ROOM DATABASE
# ====================================================================================================

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Room annotations
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep database classes
-keep class com.taskflow.app.AppDatabase { *; }
-keep class com.taskflow.app.TaskDao { *; }
-keep class com.taskflow.app.TaskRepository { *; }

# ====================================================================================================
# RETROFIT & GSON
# ====================================================================================================

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Retrofit API interfaces
-keep interface com.taskflow.app.QuoteApiService { *; }
-keep class com.taskflow.app.RetrofitClient { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Gson generic types
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep model classes for Gson
-keep class com.taskflow.app.QuoteResponse { *; }
-keep class com.taskflow.app.Task { *; }

# ====================================================================================================
# COROUTINES
# ====================================================================================================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ====================================================================================================
# NAVIGATION COMPONENT
# ====================================================================================================

-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** {
    *;
}

# ====================================================================================================
# LIFECYCLE & VIEWMODEL
# ====================================================================================================

-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# ====================================================================================================
# MATERIAL DESIGN
# ====================================================================================================

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ====================================================================================================
# ANDROID X
# ====================================================================================================

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ====================================================================================================
# REMOVE LOGGING (for Release)
# ====================================================================================================

# Remove all Log calls (except Log.e for errors)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Keep error logs
-assumenosideeffects class android.util.Log {
    public static *** e(...);
}

# ====================================================================================================
# OPTIMIZATION
# ====================================================================================================

# Optimization iterations
-optimizationpasses 5

# Don't use mixed case class names
-dontusemixedcaseclassnames

# Don't skip non-public library class members
-dontskipnonpubliclibraryclasses

# Don't skip non-public library classes
-dontskipnonpubliclibraryclassmembers

# Verbose output
-verbose

# Don't warn about missing classes
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# ====================================================================================================
# SECURITY
# ====================================================================================================

# Rename source files (hide original filenames)
-renamesourcefileattribute SourceFile

# Firebase Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep crash reporting classes
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Remove debug info (optional - uncomment for extra security)
# -assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#     static void checkParameterIsNotNull(...);
#     static void checkNotNullParameter(...);
# }

# ====================================================================================================
# END OF CONFIGURATION
# ====================================================================================================
