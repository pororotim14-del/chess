pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ChessEngineAssistant"

include(":app")
include(":core-chess")
include(":core-engine")
include(":core-security")
include(":core-ui")
include(":data")
include(":domain")
include(":feature-analysis")
include(":feature-board")
include(":feature-games")
include(":feature-settings")
include(":native-engine")