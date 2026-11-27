plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.iosexperience.launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.iosexperience.launcher"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
