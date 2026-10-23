plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// CI passes -PversionCode=<run number> so every release installs over the previous one.
val appVersionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val releaseKeystore: String? = System.getenv("BS_KEYSTORE")

android {
    namespace = "com.betweensets.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.betweensets.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"
    }

    // github: the APK from GitHub Releases, which updates itself. play: the Play Store build, which
    // can't (Play forbids self-updating apps), so it has no updater, internet or install permission.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            isDefault = true
        }
        create("play") {
            dimension = "store"
        }
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore != null) {
                storeFile = file(releaseKeystore)
                storeType = "pkcs12"
                storePassword = System.getenv("BS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BS_KEY_ALIAS")
                keyPassword = System.getenv("BS_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    "githubImplementation"(libs.androidx.work)

    testImplementation(libs.junit)
}
