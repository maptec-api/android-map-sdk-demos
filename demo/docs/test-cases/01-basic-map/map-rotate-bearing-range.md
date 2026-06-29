# 旋转角度范围

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 旋转角度范围 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **旋转角度范围** |
| 路由 | `main` → `map` → `map_gestures` → `map_rotate_bearing_range` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapRotateBearingRangeScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapRotateBearingRangeScreenTest.kt` |
| Demo 目的 | 验证旋转手势允许的最小/最大方位角范围 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.rotateGestureMinBearing` | 属性 | 最小方位角 | ✅ TC-003 |
| `UiSettings.rotateGestureMaxBearing` | 属性 | 最大方位角 | ✅ TC-004 |
| `UiSettings.setRotateGestureBearingRange` | 方法 | 设置范围 | ✅ TC-003、TC-004 |
| `cameraPosition.bearing` | 属性 | 当前方位角展示 | ✅ TC-002 |
| `addOnCameraMoveListener` | 监听 | 方位角实时更新 | — |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 标题与提示 | 进入页面 | 标题与说明文案可见 | — | UI | `testTitleAndHintDisplayed` | P2 |
| TC-002 | 当前方位角 | 进入页面 | 当前 bearing 数值展示 | `cameraPosition.bearing` | UI | `testCurrentBearingDisplayed` | P1 |
| TC-003 | 最小方位角滑条 | 拖动 `slider_min_bearing` | `rotateGestureMinBearing` 更新 | `rotateGestureMinBearing` | UI+API | `testMinBearingSlider_UpdatesApi` | P0 |
| TC-004 | 最大方位角滑条 | 拖动 `slider_max_bearing` | `rotateGestureMaxBearing` 更新 | `rotateGestureMaxBearing` | UI+API | `testMaxBearingSlider_UpdatesApi` | P0 |
| TC-005 | 范围一致性 | 设置 min/max | min ≤ max | 范围约束 | API | `testMinAndMaxBearing_RangeConsistency` | P1 |
| TC-006 | 滑条标签（min） | 查看 min 滑条 | 标签与数值正确 | — | UI | `testMinBearingSlider_ValueRangeSliderLabels` | P2 |
| TC-007 | 滑条标签（max） | 查看 max 滑条 | 标签与数值正确 | — | UI | `testMaxBearingSlider_ValueRangeSliderLabels` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapRotateBearingRangeScreenTest
```
