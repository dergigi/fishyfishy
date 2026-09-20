import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
val local = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String): String? = System.getenv(key) ?: local.getProperty(key)
val signingKeys = listOf("OEM_STORE_FILE", "OEM_STORE_PASSWORD", "OEM_KEY_ALIAS", "OEM_KEY_PASSWORD")
val canSign = signingKeys.all { !signingValue(it).isNullOrBlank() }
android {
    namespace = "org.dergigi.fishyfishy"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "org.dergigi.fishyfishy"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.6.0"
    }
    if (canSign) signingConfigs.create("release") {
        storeFile = file(signingValue("OEM_STORE_FILE")!!)
        storePassword = signingValue("OEM_STORE_PASSWORD")
        keyAlias = signingValue("OEM_KEY_ALIAS")
        keyPassword = signingValue("OEM_KEY_PASSWORD")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint {
        abortOnError = true
        // Lifecycle 2.9's LiveData detector crashes with AGP 8.7's Kotlin analysis.
        // Same workaround as Boris; this app uses Compose state, not LiveData.
        disable += "NullSafeMutableLiveData"
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
