import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---- Release signing -----------------------------------------------------
// Loaded from keystore.properties (git-ignored) or environment variables.
// Nothing secret is ever hard-coded here.
val keystoreFile: String? = project.findProperty("KEYSTORE_FILE") as? String
    ?: System.getenv("KEYSTORE_FILE")
val keystorePassword: String? = project.findProperty("KEYSTORE_PASSWORD") as? String
    ?: System.getenv("KEYSTORE_PASSWORD")
val keyAlias: String? = project.findProperty("KEY_ALIAS") as? String
    ?: System.getenv("KEY_ALIAS")
val keyPassword: String? = project.findProperty("KEY_PASSWORD") as? String
    ?: System.getenv("KEY_PASSWORD")

fun loadSigningProperties(): Pair<Properties, Boolean> {
    val props = Properties()
    val file = rootProject.file("keystore.properties")
    if (file.exists() && file.isFile) {
        file.inputStream().use { props.load(it) }
        return props to true
    }
    return props to false
}

val (signProps, hasSigning) = loadSigningProperties()

android {
    namespace = "com.chessassistant.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.chessassistant.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasSigning || keystoreFile != null) {
                signingConfigs.create("release") {
                    val filePath = (signProps.getProperty("KEYSTORE_FILE") ?: keystoreFile) ?: ""
                    storeFile = rootProject.file(filePath)
                    storePassword = signProps.getProperty("KEYSTORE_PASSWORD") ?: keystorePassword ?: ""
                    keyAlias = signProps.getProperty("KEY_ALIAS") ?: keyAlias ?: ""
                    keyPassword = signProps.getProperty("KEY_PASSWORD") ?: keyPassword ?: ""
                }
            } else {
                null
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = null
            applicationIdSuffix = ".benchmark"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core-chess"))
    implementation(project(":core-engine"))
    implementation(project(":core-security"))
    implementation(project(":core-ui"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":native-engine"))
    implementation(project(":feature-board"))
    implementation(project(":feature-analysis"))
    implementation(project(":feature-games"))
    implementation(project(":feature-settings"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}