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
        maven { url = uri("https://jitpack.io") }
        // Maptec Maven 私服（releases + snapshots，拉取无需凭据）
        maven {
            url = uri(settings.extra["mavenReleasesUrl"] as String)
        }
        maven {
            url = uri(settings.extra["mavenSnapshotsUrl"] as String)
            credentials {
                username = settings.extra["mavenUsername"] as String?
                password = settings.extra["mavenPassword"] as String?
            }

        }
    }
}

val useSource = providers.gradleProperty("useSource").orNull?.toBoolean() ?: false

println("settings: useSource = $useSource")

if (useSource) {
    val mapsdkRootDir = file("../platform/android/mapsdk")

    println("settings: mapsdkRootDir = ${mapsdkRootDir.absolutePath}")

    if (!mapsdkRootDir.exists()) {
        throw GradleException("mapsdk root directory not found: ${mapsdkRootDir.absolutePath}")
    }

    val includeMapbizAndroid =
        gradle.parent != null ||
                providers.gradleProperty("mapsdk.includeMapbizAndroid").orNull == "true"

    println("includeMapbizAndroid: $includeMapbizAndroid")

    fun projectDir(subPath: String): File {
        return file("${mapsdkRootDir.absolutePath}/$subPath")
    }

    include(":mapengine-android")
    project(":mapengine-android").projectDir = projectDir("mapengine-android")

    if (includeMapbizAndroid) {
        include(":mapbiz-android")
        project(":mapbiz-android").projectDir = projectDir("mapbiz-android")
    }

    include(":javabase")
    val javabaseDir = file("${mapsdkRootDir.absolutePath}/../../../java-base")
    if (!javabaseDir.exists()) {
        throw GradleException("java-base directory not found: ${javabaseDir.absolutePath}")
    }
    project(":javabase").projectDir = javabaseDir

    includeBuild("${mapsdkRootDir.absolutePath}/buildSrc") {
        name = "mapsdk-build-logic"
    }

    includeBuild("${mapsdkRootDir.absolutePath}/map-plugin")

    buildCache {
        local {
            isEnabled = true
        }
    }

    include(":route")
    project(":route").projectDir = projectDir("route")

    include(":search")
    project(":search").projectDir = projectDir("search")

    include(":maptec-gps-provider")
    project(":maptec-gps-provider").projectDir = projectDir("maptec-gps-provider")

    include(":gesture")
    project(":gesture").projectDir = projectDir("gesture")

}

include(":demo")
