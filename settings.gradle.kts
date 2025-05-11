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
        maven { url = uri("https://jitpack.io") } // Добавляем JitPack для MPAndroidChart
    }
    versionCatalogs {
        val libs by creating {
            // Явное объявление без файла
            version("mpandroidchart", "3.1.0")
            library("mpandroidchart", "com.github.PhilJay", "MPAndroidChart").versionRef("mpandroidchart")
        }
    }
}

rootProject.name = "HealthTracker"
include(":app")