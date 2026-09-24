plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.trotabares.elteoremadelmorfi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trotabares.elteoremadelmorfi"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
