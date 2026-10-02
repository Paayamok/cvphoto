pluginManagement {
    repositories {
        google()
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

rootProject.name = "FuShengCe"

include(":app")
include(":core:media")
include(":core:database")
include(":core:background")
include(":feature:home")
include(":feature:gallery")
include(":feature:viewer")
include(":core:metadata")
include(":core:ui")
include(":feature:search")
include(":feature:albums")
include(":feature:tasks")
