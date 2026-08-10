plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
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
    implementation("androidx.room:room-runtime:2.7.0")
	implementation("androidx.room:room-ktx:2.7.0")
	ksp("androidx.room:room-compiler:2.7.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
