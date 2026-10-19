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

val mavenUsername = readLocalProperty("mavenUsername")
val mavenPassword = readLocalProperty("mavenPassword")

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
            if (!mavenUsername.isNullOrBlank() && !mavenPassword.isNullOrBlank()) {
                credentials {
                    username = mavenUsername
                    password = mavenPassword
                }
            }
        }
    }
}

include(":demo")
