# youtubedl-android / Jackson models
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn org.apache.commons.io.**
-dontwarn org.apache.commons.compress.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
