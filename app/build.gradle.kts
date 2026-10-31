plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.autolounge.park"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.autolounge.park"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0-internal"

        // Lisans sunucusu (website/worker) ve imza doğrulama için açık anahtar.
        // Açık anahtar gizli değildir; özel anahtar yalnızca sunucuda (.secrets/) durur.
        buildConfigField("String", "LICENSE_API_URL", "\"https://autolounge.app\"")
        buildConfigField("String", "LICENSE_PUBLIC_KEY", "\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKtE2xllMiDlQ+3VE9+UEWSv86EL/VSZUeRmdIcqjIk8dvuDc3dBo1Kqu/7UlroWXBKHGIHE9MbjRr8ElS5kmmw==\"")
        buildConfigField("String", "PURCHASE_URL", "\"https://autolounge.app/#pricing\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"destek@autolounge.app\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil:2.6.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation(files("libs/aauto-sdk.jar"))
}
