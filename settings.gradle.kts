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

/** 从根目录 local.properties 读取（已 gitignore）；不存在或未配置时返回 null。 */
fun readLocalProperty(key: String): String? {
    val file = settings.rootDir.resolve("local.properties")
    if (!file.isFile) return null
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate {
            val idx = it.indexOf('=')
            it.substring(0, idx).trim() to it.substring(idx + 1).trim()
        }[key]
        ?.takeIf { it.isNotEmpty() }
}

val mavenUsername = readLocalProperty("MAVEN_USERNAME")
val mavenPassword = readLocalProperty("MAVEN_PASSWORD")

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
            // 仅当 local.properties 中同时配置了用户名和密码时才附加 credentials
            if (!mavenUsername.isNullOrBlank() && !mavenPassword.isNullOrBlank()) {
                credentials {
                    username = mavenUsername
                    password = mavenPassword
                }
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
