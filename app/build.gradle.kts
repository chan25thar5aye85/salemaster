plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hninakari.salemaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hninakari.salemaster"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    dependencies {
        implementation(project(":core"))
        implementation(project(":feature:inventory"))

        implementation("androidx.activity:activity-compose:1.10.0")

        implementation(platform("androidx.compose:compose-bom:2025.01.00"))

        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.ui:ui-tooling-preview")

        debugImplementation("androidx.compose.ui:ui-tooling")
    }
}
