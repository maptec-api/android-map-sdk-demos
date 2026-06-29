# 双击重置倾斜

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 双击重置倾斜 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **双击重置倾斜** |
| 路由 | `main` → `map` → `map_gestures` → `map_double_tap_tilt_reset` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapDoubleTapTiltResetScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapDoubleTapTiltResetScreenTest.kt`（1 个 `@Test`） |
| Demo 目的 | 验证双击是否重置地图倾斜角 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOptions.doubleTapTiltResetEnabled` | 配置 | 初始化选项 | — | — |
| `UiSettings.isDoubleTapTiltResetEnabled` | 属性 | 双击重置倾斜 | ✅ TC-001 | — |
| `cameraPosition.tilt` | 属性 | 倾斜角 | — | 未覆盖 |
| 双击手势重置 tilt | 手势 | 倾斜后双击 | — | 未覆盖 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 双击重置倾斜开关 | 切换 `switch_double_tap_tilt_reset` | `isDoubleTapTiltResetEnabled` 同步 | `isDoubleTapTiltResetEnabled` | UI+API | `toggleDoubleTapTiltResetSwitch` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapDoubleTapTiltResetScreenTest
```
