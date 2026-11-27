plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.iosexperience.music"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.iosexperience.music"
        minSdk = 29
        targetSdk = 35
        versionCode = 40
        versionName = "40.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
