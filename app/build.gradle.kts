plugins {
    alias(libs.plugins.android.application)
}

base {
    archivesName.set("Video")
}

android {
    namespace = "com.video.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.video.app"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
