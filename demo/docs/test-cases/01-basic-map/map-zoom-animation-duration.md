# 缩放动画时长

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 缩放动画时长 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **缩放动画时长** |
| 路由 | `main` → `map` → `map_gestures` → `map_zoom_animation_duration` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapZoomAnimationDurationScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapZoomAnimationDurationScreenTest.kt` |
| Demo 目的 | 验证缩放动画开关与动画时长配置 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.isZoomAnimationDurationEnabled` | 属性 | 缩放动画是否启用 | ✅ TC-001 |
| `UiSettings.zoomAnimationDuration` | 属性 | 缩放动画时长（ms） | ✅ TC-002 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 动画开关 | 切换 `switch_zoom_animation_enabled` | `isZoomAnimationDurationEnabled` 同步 | `isZoomAnimationDurationEnabled` | UI+API | `toggleZoomAnimationEnabledSwitch` | P0 |
| TC-002 | 时长滑条 | 拖动 `slider_zoom_animation_duration` | `zoomAnimationDuration` 更新 | `zoomAnimationDuration` | UI+API | `changeZoomAnimationDurationViaSlider` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapZoomAnimationDurationScreenTest
```
