import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun configValue(name: String, fallback: String): String =
    localProperties.getProperty(name) ?: System.getenv(name) ?: fallback

// OneDrive can lock generated build folders while syncing. Keep disposable
// compiler output in LocalAppData so Clean and Run remain reliable.
val externalBuildRoot = System.getenv("ENGLISH_TUTOR_BUILD_DIR")
    ?: (System.getenv("LOCALAPPDATA") + "/EnglishTutorBuildAndroidStudio")
layout.buildDirectory.set(file("$externalBuildRoot/app"))

android {
    namespace = "ru.savvina.englishtutor"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.savvina.englishtutor"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${configValue("SUPABASE_URL", "https://eesamyzjakycnhlvmnzv.supabase.co")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${configValue("SUPABASE_PUBLISHABLE_KEY", "sb_publishable_l8_UtgixV3VCWN0xv0XWeg_nIf_ozGK")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.1")
    implementation("androidx.compose.foundation:foundation:1.12.1")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-android:3.6.0")
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.12.1")
}
