pluginManagement {
    repositories {
        google() // <-- Make sure this is here
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // <-- And especially here
        mavenCentral()
    }
}

rootProject.name = "VitaGuard"
include(":app")