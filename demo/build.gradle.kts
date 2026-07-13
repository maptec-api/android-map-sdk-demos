plugins {
    alias(libs.plugins.android.application.withoutVersion)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.compose)
}
val useSource = (project.findProperty("useSource") as? String) != "false"
val useLog = (project.findProperty("useLog") as? String) != "false"
android {
    namespace = "com.maptec.applied.demo"
    compileSdk = 36
    // 与 platform/android/mapsdk/buildSrc/.../Versions.kt 保持一致。
    // app 模块必须声明 ndkVersion，否则 Linux CI 上 strip*DebugSymbols 找不到 llvm-strip，
    // 会把依赖传来的未 strip .so 原样打进 APK（Mac 本地可能仍能 strip，故仅 CNB 复现）。
    ndkVersion = "29.0.14206865"

    testBuildType = "release"
    defaultConfig {
        applicationId = "com.maptec.applied.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testProguardFiles("proguard-test-rules.pro")

        buildConfigField("boolean", "USE_LOG", "${useLog}")
    }

    signingConfigs {
        create("config") {
            // 请确保在 demo 目录下存在 maptec.jks 文件
            storeFile = file("maptec.jks")
            storePassword = "android"
            keyAlias = "maptec"
            keyPassword = "android"
        }
    }
    buildTypes {
        debug {
            isJniDebuggable = true
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    cppFlags("-g", "-O0", "-fno-limit-debug-info", "-DDEBUG")
                }
            }
            signingConfig = signingConfigs.getByName("config")

        }
        release {
            isMinifyEnabled = false
            isJniDebuggable = false
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    cppFlags("-g", "-O2", "-DNDEBUG")
                }
            }
            signingConfig = signingConfigs.getByName("config")
        }

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "renderer"
    productFlavors {
        create("opengl") {
            dimension = "renderer"
            externalNativeBuild {
                cmake {
                    arguments("-DMLN_WITH_OPENGL=ON", "-DMLN_WITH_VULKAN=OFF")
                }
            }
        }
    }
}



dependencies {
    if (useSource) {
        implementation(project(":mapengine-android"))
    } else {
        implementation(libs.maptec.mapsdk)
    }

    implementation(libs.navigationCompose)
    implementation(libs.appcompat)
    implementation(libs.coreKtx)
    implementation(libs.lifecycleRuntimeKtx)
    implementation(libs.activityCompose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
//    implementation("com.github.bytedance:memory-leak-detector:0.2.1")
    implementation(platform(libs.composeBom))
    implementation(libs.ui)
    implementation(libs.uiGraphics)
    implementation(libs.uiToolingPreview)
    implementation(libs.material3)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(libs.gson)
    androidTestImplementation(libs.testRunner)
    androidTestImplementation(libs.testRules)
    androidTestImplementation(libs.testEspressoCore)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.uiTestJunit4)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.gson)
    androidTestImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    debugImplementation(libs.uiTooling)
    debugImplementation(libs.uiTestManifest)
    debugImplementation(libs.uiTestManifest)

    debugImplementation("com.bytedance.tools.codelocator:codelocator-core:2.0.4")
}
configurations.all {
    resolutionStrategy {
        force("androidx.test.espresso:espresso-core:3.7.0")
        // 如果你还用到了其他 espresso 组件，也一并强制
        force("androidx.test.espresso:espresso-contrib:3.7.0")
        force("androidx.test.espresso:espresso-intents:3.7.0")
        force("androidx.test.espresso:espresso-idling-resource:3.7.0")
    }
}
