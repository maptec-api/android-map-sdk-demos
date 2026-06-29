# 地图视角控制

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 地图视角控制 |
| 导航路径 | 主页 → **基础地图能力** → **地图视角控制** |
| 路由 | `main` → `map` → `map_control` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/MapControlScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapControlScreenTest.kt` |
| Demo 目的 | 验证相机动画、俯仰/缩放偏好限制及相机状态读取 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapView` / `MapOptions` / `getMapAsync` | 初始化 | 创建地图并加载样式 | ✅ setUp | — |
| `MaptecMap.moveCamera(CameraUpdate)` | 方法 | 执行相机动画 | ✅ TC-001 | — |
| `CameraUpdateFactory` | 工厂 | 构造相机更新 | ✅ TC-001 | — |
| `MaptecMap.cameraPosition` | 属性 | 读取 tilt/bearing/zoom | ✅ TC-001 ~ TC-005 | — |
| `MaptecMap.setMinZoomPreference` / `setMaxZoomPreference` | 方法 | 缩放上下限 | ✅ TC-002、TC-003 | — |
| `MaptecMap.setMinPitchPreference` / `setMaxPitchPreference` | 方法 | 俯仰上下限 | ✅ TC-004、TC-005 | — |
| `MaptecMap.minPitch` / `maxPitch` | 属性 | 读取俯仰限制 | ✅ TC-004、TC-005 | — |
| `addOnCameraMoveListener` | 监听 | 相机移动更新 UI | — | 未覆盖 |

---

## 测试用例

### 相机与限制

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 相机动画 | 输入 tilt/bearing/zoom，点击动画 | `cameraPosition` 更新为输入值 | `moveCamera` | UI+API | `testAnimateCamera` | P0 |
| TC-002 | 最小缩放限制 | 修改 minZoom 并验证 | zoom 不低于 minZoom | `setMinZoomPreference` | API | `testMinZoom` | P1 |
| TC-003 | 最大缩放限制 | 修改 maxZoom 并验证 | zoom 不高于 maxZoom | `setMaxZoomPreference` | API | `testMaxZoom` | P1 |
| TC-004 | 最小俯仰限制 | 修改 minTilt 并验证 | `minPitch` 与设置一致 | `setMinPitchPreference` | API | `testMinTilt` | P1 |
| TC-005 | 最大俯仰限制 | 修改 maxTilt 并验证 | `maxPitch` 与设置一致 | `setMaxPitchPreference` | API | `testMaxTilt` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapControlScreenTest
```
