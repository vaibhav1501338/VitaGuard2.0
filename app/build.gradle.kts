plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.vitaguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vitaguard"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Utilities for multi-contact management and animation
    implementation(libs.gson)
    // FIX: Lottie library often fails alias resolution; use direct string format as fallback
    implementation("com.airbnb.android:lottie:6.4.1")

    // Firebase Core and Firestore
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore.ktx)
}
