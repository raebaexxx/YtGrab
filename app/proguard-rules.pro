# youtubedl-android: called directly from Kotlin, no reflection — plain shrinking is safe.
# Jackson is only used for JsonNode/readTree in the updater; R8 keeps the
# reachable chain. The mapper (VideoInfo/readValue) is dead code here and gets removed.
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn org.apache.commons.io.**
-dontwarn org.apache.commons.compress.**

# keep enum values() / valueOf() used via Kotlin enum conversion
-keepclassmembers enum com.yausername.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
