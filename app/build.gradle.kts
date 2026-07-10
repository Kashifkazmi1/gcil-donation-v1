import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    // Use the production applicationId as the namespace so R classes and resources
    // are generated under the correct package for release builds.
    namespace = "com.stripe.aod.sampleapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ghamidicenter.donations"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Set BACKEND_URL to the provided production backend URL
        val backendUrl = "https://backend-apk-production-e9bb.up.railway.app/"
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Enable code shrinking and resource shrinking for production.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            if (!System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Do not treat all warnings as errors for CI debug builds to avoid
        // failing the build on SDK-related warnings. This allows producing a
        // debug APK for personal use even if the SDK emits warnings.
        allWarningsAsErrors = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

fun Project.findLocalProperty(propertyName: String): String {
    val localPropertiesFile = rootProject.file("local.properties")
    return if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.reader().use { properties.load(it) }
        properties.getProperty(propertyName, "")
    } else {
        ""
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.fragment.ktx)

    // OK HTTP
    implementation(libs.okhttp)

    // Retrofit
    implementation(libs.retrofit2.retrofit)
    implementation(libs.retrofit2.converter.gson)

    // Stripe Terminal library
    implementation(libs.stripe.terminal.core)
    implementation(libs.stripe.terminal.appsondevices)
    implementation(libs.stripe.terminal.ktx)

    // navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
}
