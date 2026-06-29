# 惯性时长

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 惯性时长 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **惯性时长** |
| 路由 | `main` → `map` → `map_gestures` → `map_fling_duration` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapFlingDurationScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapFlingDurationScreenTest.kt` |
| Demo 目的 | 验证惯性滚动开关与惯性动画时长 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `gesturesManager.isInertiaScrollEnabled` | 属性 | 惯性滚动是否开启 | ✅ TC-002 |
| `gesturesManager.setInertiaScrollEnabled` | 方法 | 设置惯性滚动 | ✅ TC-002 |
| `gesturesManager.inertiaScrollDuration` | 属性 | 惯性动画时长 | ✅ TC-003 |
| `MaptecMap.cameraPosition` | 属性 | 地图可访问性 | ✅ TC-004 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 默认状态 | 进入页面 | 惯性开关与时长为 SDK 默认值 | `isInertiaScrollEnabled`、`inertiaScrollDuration` | UI+API | `testFlingDuration_DefaultState` | P0 |
| TC-002 | 惯性开关 | 切换 `switch_inertia_scroll_enabled` | `isInertiaScrollEnabled` 同步 | 惯性开关 API | UI+API | `testFlingDuration_ToggleInertiaSwitch` | P0 |
| TC-003 | 时长滑条 | 拖动 `slider_fling_duration` | `inertiaScrollDuration` 更新 | `inertiaScrollDuration` | UI+API | `testFlingDuration_SliderChangesValue` | P0 |
| TC-004 | 相机可访问 | 进入页面 | `cameraPosition` 可读 | `cameraPosition` | API | `testMapView_CameraIsAccessible` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapFlingDurationScreenTest
```
