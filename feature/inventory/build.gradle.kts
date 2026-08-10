plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hninakari.salemaster.inventory"

    compileSdk = 35
    
    defaultConfig {
		multiDexEnabled = true
	}

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
    }
    
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2025.01.00"))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
