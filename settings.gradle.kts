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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack para hostear dependencias no publicadas en Maven Central
        // (en este proyecto: chesslib de bhlangonijr).
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Plantilla_ajedrez"
include(":app")
include(":domain")
include(":data")