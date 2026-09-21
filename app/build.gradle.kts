import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.den.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.den.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keyFile = providers.gradleProperty("KEYSTORE_FILE").orNull
            if (keyFile != null) {
                storeFile = file(keyFile)
                storePassword = providers.gradleProperty("KEYSTORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = null
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val useSignedRelease = providers.gradleProperty("KEYSTORE_FILE").isPresent
            signingConfig = if (useSignedRelease) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            val abi = ""
            val newName = "Den-v${versionName}-${if (name.contains("release")) "release" else "debug"}${abi}.apk"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = newName
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core / lifecycle
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Room + SQLCipher encrypted storage
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite-framework:2.6.0")

    // Background work (auto backups, rescheduling)
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // DataStore preferences
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Biometric app lock
    implementation("androidx.biometric:biometric:1.1.0")

    // Serialization for backup payloads
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Video playback
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")

    testImplementation("junit:junit:4.13.2")
}