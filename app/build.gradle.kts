plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pritam.voiceassistantjetpack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pritam.voiceassistantjetpack"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.wear.compose:compose-material:1.1.2")
    implementation("androidx.wear.compose:compose-foundation:1.1.2")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("com.airbnb.android:lottie-compose:5.2.0")
    implementation ("androidx.compose.ui:ui:1.6.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
    implementation ("androidx.navigation:navigation-compose:2.7.7")

    implementation ("com.google.zxing:core:3.4.1")


    implementation ("com.google.android.gms:play-services-wearable:18.1.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation ("androidx.datastore:datastore-preferences:1.0.0")


    implementation ("androidx.compose.material:material:1.6.1")
    implementation ("androidx.activity:activity-ktx:1.8.2")
}