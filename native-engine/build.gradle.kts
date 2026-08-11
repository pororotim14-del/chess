plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.chessassistant.nativeengine"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    ndkVersion = libs.versions.ndk.get()
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }
}