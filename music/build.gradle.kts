plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingStoreFile = providers.environmentVariable("SONIC_KEYSTORE_PATH").orNull
val signingStorePassword = providers.environmentVariable("SONIC_KEYSTORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("SONIC_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("SONIC_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword).all { !it.isNullOrBlank() }

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

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
