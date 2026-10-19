# Maptec Maps SDK for Android Demo

这是 Maptec Maps SDK for Android 的示例工程，展示地图初始化、地图控件、手势交互、覆盖物绘制、定位能力、Web 服务调用以及几何计算工具等常见接入场景。

示例代码面向 Android 开发者，适合用于快速了解 SDK API 的使用方式，也可以作为接入项目时的参考实现。

## 示例内容

工程中的示例按能力模块组织：

| 模块 | 说明 |
| --- | --- |
| 鉴权配置 | 配置 API Key 与应用签名 SHA1，并验证地图、搜索、路线规划等服务 |
| 地图 | 地图初始化、样式切换、渲染参数、相机控制、路况展示等基础能力 |
| 覆盖物 | Marker、Info Window、Polyline、Polygon、Circle、Geofence、热力图、点聚合等覆盖物能力 |
| 地图交互 | 地图事件、POI 点击、控件显示、手势配置、定位图层、地图截图、视野限制、相机动画、内容边距等交互能力 |
| Web 服务 | 文本搜索、附近搜索、沿途搜索、输入提示、地点详情、地理编码、逆地理编码、路线规划等服务调用示例 |
| 工具 | **几何计算**（单页聚合 Demo）：球面几何（测距、夹角、面积、带符号面积、偏移、反推原点、插值）、路径与多边形（路径抽稀、闭合段索引、点到线段、是否闭合）、折线编解码；对应 `SphericalUtil` / `PolyUtil` |

主要示例源码位于：

```text
demo/src/main/java/com/maptec/applied/demo/ui/screens
```

## 环境要求

- Android Studio Narwhal 或更新版本
- JDK 17 或 Android Studio 内置 JDK
- Android Gradle Plugin 8.10.1
- Android 7.0（API 24）及以上设备或模拟器
- 可访问 Maptec Maven 仓库的网络环境

## 当前版本

当前对外发布的 Maptec Maps SDK 版本：

```text
com.maptec.applied:mapsdk:1.0.2
```

如果工程中的本地调试配置与对外版本不一致，请以开放平台发布版本为准。

## 快速运行

1. 克隆工程并打开根目录。

```bash
git clone <repository-url>
cd android-map-sdk-demos
```

2. 使用 Android Studio 打开工程，等待 Gradle 同步完成。

3. 配置 API Key 与应用签名 SHA1。

可以直接运行 App 后，在「鉴权配置」页面输入 API Key 和 SHA1；也可以在本地资源文件中配置默认测试值。对外发布或提交代码时，请不要提交真实 API Key。

本地调试可创建或修改：

```xml
<!-- demo/src/main/res/values/maptec.xml -->
<resources>
    <string name="maptec_apiKey">YOUR_API_KEY</string>
    <string name="signature_sha1">YOUR_APP_SIGNATURE_SHA1</string>
</resources>
```

4. 运行 Demo。

在 Android Studio 中选择 `demo` 模块，构建变体选择 `openglDebug`，或使用命令行构建：

```bash
./gradlew :demo:assembleOpenglDebug
```

构建完成后，将 APK 安装到设备：

```bash
./gradlew :demo:installOpenglDebug
```

## API Key 配置说明

地图、搜索、地理编码和路线规划服务都需要有效的 API Key。Demo 支持在运行时输入 Key，并通过统一入口应用到 SDK：

```kotlin
MaptecInitializer.setApiKey(apiKey)
```

如果你的 Key 绑定了 Android 应用签名，还需要配置 SHA1：

```kotlin
MaptecInitializer.setSignatureSha1(signatureSha1)
```

正式项目中建议将 API Key 与包名、签名或服务范围绑定，避免 Key 被未授权应用使用。

## 工程结构

```text
android-map-sdk-demos
├── demo
│   ├── src/main/java/com/maptec/applied/demo
│   │   ├── ui/screens/auth            # 鉴权配置
│   │   ├── ui/screens/maps            # 地图基础能力
│   │   ├── ui/screens/overlays        # 覆盖物示例
│   │   ├── ui/screens/interaction     # 地图交互示例
│   │   ├── ui/screens/web_services    # Web 服务示例
│   │   └── ui/screens/tools           # 几何计算（GeometryDemoScreen + geometry/）
│   ├── src/main/res                   # 资源文件
│   └── build.gradle.kts               # Demo App 构建配置（仅 opengl flavor）
├── gradle/libs.versions.toml          # 依赖版本配置
├── local.properties.example           # 本地密钥模板（复制为 local.properties）
├── settings.gradle.kts                # Gradle 工程配置
└── README.md
```

## 集成方式参考

在实际项目中接入 SDK 时，通常需要完成以下步骤。

1. 添加 Maven 仓库。

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.maptec.com/repository/maven-releases/")
        }
    }
}
```

2. 添加 SDK 依赖。

```kotlin
dependencies {
    implementation("com.maptec.applied:mapsdk:1.0.2")
}
```

3. 初始化 SDK。

```kotlin
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MaptecInitializer.initialize(this)
    }
}
```

4. 在页面中创建地图并使用对应能力。

完整写法可以参考 `demo/src/main/java/com/maptec/applied/demo/ui/screens` 下的各个示例页面。

## 权限说明

Demo 使用了以下 Android 权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

定位相关示例需要用户授权定位权限。若你的应用不使用定位能力，可以不申请定位权限。

## 安全建议

- 不要将真实 API Key、账号密码、签名证书或生产环境配置提交到公开仓库。
- 不要在正式包中开启全局 HTTP 明文流量。
- 不要在正式包中开启 verbose 级别日志。
- 发布正式应用时，请使用自己的签名证书，不要复用 Demo 中的测试签名。
- 建议在开放平台中为 API Key 配置应用包名、签名 SHA1 和服务范围限制。

## 常见问题

### 地图没有显示怎么办？

请检查网络连接、API Key 是否有效、应用签名 SHA1 是否匹配，以及设备是否允许当前 App 访问网络。

### 搜索或路线规划接口返回鉴权失败怎么办？

请确认 API Key 已应用到对应服务，并确认当前 Key 已开通搜索、地理编码或路线规划服务。

### 命令行构建提示找不到 `assemblePublicOpenglDebug` 或 `useSource` 怎么办？

对外工程没有 `public` / `inner` 渠道，也没有源码依赖开关。请使用：

```bash
./gradlew :demo:assembleOpenglDebug
```

## 反馈

如果你在接入过程中遇到问题，请提供以下信息，便于定位：

- Demo 分支或提交号
- Android Studio 与 Gradle 版本
- 设备型号与 Android 系统版本
- 复现步骤、错误日志和相关截图
