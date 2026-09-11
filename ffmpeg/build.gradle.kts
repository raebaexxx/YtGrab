plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.yausername.ffmpeg"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
