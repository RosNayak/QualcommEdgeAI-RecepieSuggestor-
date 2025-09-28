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

# Keep ML Kit GenAI classes and methods
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep Google Generative AI classes
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Keep OpenNLP classes
-keep class opennlp.** { *; }
-dontwarn opennlp.**

# Keep CameraX classes
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep RecyclerView classes
-keep class androidx.recyclerview.** { *; }

# Keep application-specific classes that might be accessed via reflection
-keep class com.example.recepiesuggestor.models.** { *; }
-keep class com.example.recepiesuggestor.services.** { *; }
-keep class com.example.recepiesuggestor.data.** { *; }

# Keep Recipe class for Parcelable
-keep class com.example.recepiesuggestor.Recipe { *; }

# Keep callback interfaces
-keep interface com.example.recepiesuggestor.** {
    public *;
}

# Keep debugging information for better crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}