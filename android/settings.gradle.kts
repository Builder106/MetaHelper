pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = java.util.Properties()
val localPropertiesFile = settingsDir.resolve("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Add this block
        maven {
            url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
            credentials {
                username = "" // not needed
                password = localProperties.getProperty("github_token")
                           ?: providers.gradleProperty("github_token").orNull 
                           ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "MetaHelper"
include(":app")