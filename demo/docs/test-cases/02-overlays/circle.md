# 圆

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | 圆（含 8 个子示例） |
| 导航路径 | 主页 → **业务图层** → **圆** → 子项 |
| 路由 | `circle` → `circle_basic` / `circle_geodesic` / … |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/circle/*.kt`、`src/main/java/com/maptec/applied/demo/ui/screens/overlays/circle/CircleCommon.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/CircleScreenTest.kt` |
| Demo 目的 | 验证 Circle 绘制、等距/拖拽、阴影发光及扫描/脉动/半径呼吸动画 |

## 覆盖 API 清单（共享）

| SDK API | 类型 | Demo 调用说明 |
|---------|------|--------------|
| `MapOverlayEngine.addCircle` / `deleteAllCircles` | 方法 | 圆形增删 |
| `Circle` / `CircleOptions` | 对象/配置 | 半径/颜色/描边 |
| `withGeodesic` | 配置 | 等距圆 |
| `withDraggable` | 配置 | 可拖拽 |
| `withInnerShadow` | 配置 | 内阴影 |
| `withOuterGlow` | 配置 | 外发光 |
| `withScanEnabled` | 配置 | 扫描动画 |
| `ScanningAnimation` / `PulsatingAnimation` / `RadiusAnimation` | 动画 | 圆动画 |
| `circle.startAnimation()` | 方法 | 启动动画 |
| `CameraUpdateFactory` | 工厂 | 绘制后相机 |

---

## 子屏测试用例

### 列表 hub

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-001 | 子菜单展示 | `testCircleList_allSubMenusDisplayed` | P1 |

### 基础绘制（circle_basic）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-101 | UI 展示 | `testBasicScreen_uiElementsDisplayed` | P1 |
| TC-102 | 默认绘制 API | `testBasicScreen_drawDefault_apiVerified` | P0 |
| TC-103 | 自定义参数 | `testBasicScreen_drawCustomValues_apiVerified` | P0 |
| TC-104 | 非法坐标禁用 | `testBasicScreen_invalidLatLng_drawDisabled` | P1 |
| TC-105 | 重绘更新相机 | `testBasicScreen_redrawUpdatesCamera` | P1 |

### 等距模式（circle_geodesic）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-201 | 等距开启 | `testGeodesicScreen_geodesicEnabled_apiVerified` | P0 |

### 可拖拽（circle_draggable）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-301 | 默认可拖拽 | `testDraggableScreen_draggableDefault_apiVerified` | P0 |
| TC-302 | 关闭拖拽 | `testDraggableScreen_draggableDisabled_apiVerified` | P1 |

### 内阴影（circle_inner_shadow）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-401 | 内阴影开启 | `testInnerShadowScreen_innerShadowEnabled_apiVerified` | P0 |
| TC-402 | 自定义模糊 | `testInnerShadowScreen_customBlur_apiVerified` | P1 |

### 外发光（circle_outer_glow）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-501 | 发光条件 UI | `testOuterGlowScreen_glowConditionalUI` | P1 |
| TC-502 | 自定义发光 | `testOuterGlowScreen_customGlowValues_apiVerified` | P0 |

### 扫描动画（circle_scan）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-601 | 扫描输入展示 | `testScanScreen_scanInputsDisplayed` | P1 |
| TC-602 | 绘制创建圆 | `testScanScreen_drawCreatesCircle_apiVerified` | P0 |
| TC-603 | 绘制后启动动画 | `testScanScreen_startAnimation_afterDraw` | P0 |

### 脉动动画（circle_pulse）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-701 | 绘制并播放 | `testPulseScreen_drawAndStartAnimation_noCrash` | P0 |

### 半径呼吸（circle_radius_breath）

| 编号 | 用例名称 | 自动化 | 优先级 |
|------|---------|--------|--------|
| TC-801 | 绘制并播放 | `testRadiusBreathScreen_drawAndStartAnimation_noCrash` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.CircleScreenTest
```
