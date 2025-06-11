plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("io.realm.kotlin") version ("2.3.0")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.mobile_health_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mobile_health_app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    android {
        packaging {
            resources {
                excludes.add("META-INF/native-image/**")
            }
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Password strength checker library
    implementation("com.nulab-inc:zxcvbn:1.9.0")

    // MongoDB dependencies
    implementation(platform("org.mongodb:mongodb-driver-bom:5.5.0"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlinx:5.5.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation ("io.realm.kotlin:library-base:2.3.0")

    implementation("io.realm.kotlin:library-sync:2.3.0")


    // chỉ include đúng file AAR:
    implementation(files("libs/samsung-health-data-1.5.1.aar"))
    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    //implementation (files("libs/samsung-health-data-api-1.0.0-b2.aar"))
    implementation ("com.google.code.gson:gson:2.9.0")
    //implementation(name = "samsung-health-data-api-1.5.1", ext = "aar")

    implementation("androidx.health.connect:connect-client:1.1.0-rc02")
    //implementation("androidx.health:health-connect-client:1.1.0-alpha02")
    // Optional - only if you need these specific components
    // implementation("androidx.health:health-connect-client-standard:1.1.0-alpha02")
    // implementation("androidx.health:health-connect-client-permission-types:1.1.0-alpha02")


    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")



    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}