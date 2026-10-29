import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { input -> load(input) }
    }
}

// CI receives these only during an explicit trusted signing job. Local builds
// retain their existing untracked keystore.properties configuration.
val releaseStorePath = System.getenv("MOKA_SIGNING_STORE_FILE")?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty("storeFile")
val releaseStorePassword = System.getenv("MOKA_SIGNING_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty("storePassword")
val releaseKeyAlias = System.getenv("MOKA_SIGNING_KEY_ALIAS")?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty("keyAlias")
val releaseKeyPassword = System.getenv("MOKA_SIGNING_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty("keyPassword")
val releaseSigningConfigured = listOf(
    releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.mokamusic.player"
    compileSdk { version = release(37) { minorApiLevel = 2 } }

    defaultConfig {
        applicationId = "com.mokamusic.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 39
        versionName = "4.0.0-beta06.1-wonhee-preview"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-inspector:1.11.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.guava:guava:33.7.2-android")
    testImplementation("junit:junit:4.13.2")
    // Android's org.json stubs throw on the local JVM; use the real JSON parser in tests.
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
