plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)


    alias(libs.plugins.daggerHilt)
    alias(libs.plugins.org.jetbrains.kotlin.kapt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.firebase.crashlytics)

}

android {
    namespace = "com.engyh.friendhub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.engyh.friendhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.kotlinx.coroutines.android)


    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.glide)

    implementation(libs.androidx.palette.ktx)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.lottie)

    implementation(libs.emoji.google.v0160)

    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.google.firebase.database.ktx)
    implementation(libs.google.firebase.storage.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

}

kapt {
    correctErrorTypes = true
}