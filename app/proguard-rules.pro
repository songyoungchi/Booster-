# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep model classes
-keep class com.ultrabooster.gamebooster.utils.** { *; }
-keep class com.ultrabooster.gamebooster.service.** { *; }
-keep class com.ultrabooster.gamebooster.ui.** { *; }

# Keep accessibility service
-keep class com.ultrabooster.gamebooster.service.GameDetectionService { *; }

# Keep floating window service
-keep class com.ultrabooster.gamebooster.service.FloatingWindowService { *; }

# Keep boost related classes
-keep class com.ultrabooster.gamebooster.utils.BoostManager { *; }
-keep class com.ultrabooster.gamebooster.utils.PerformanceMonitor { *; }
-keep class com.ultrabooster.gamebooster.utils.FPSMonitor { *; }
-keep class com.ultrabooster.gamebooster.utils.NetworkOptimizer { *; }
-keep class com.ultrabooster.gamebooster.utils.TemperatureMonitor { *; }
-keep class com.ultrabooster.gamebooster.utils.BatteryOptimizer { *; }
-keep class com.ultrabooster.gamebooster.utils.GameDetector { *; }
-keep class com.ultrabooster.gamebooster.utils.PermissionManager { *; }

# Keep data classes
-keep class com.ultrabooster.gamebooster.utils.**$* { *; }

# Keep view binding
-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep custom exceptions
-keep class * extends java.lang.Exception { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
