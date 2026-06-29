# 缩放中心模式

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 缩放中心模式 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **缩放中心模式** |
| 路由 | `main` → `map` → `map_gestures` → `map_zoom_center_mode` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapZoomCenterModeScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapZoomCenterModeScreenTest.kt` |
| Demo 目的 | 验证缩放手势中心（手势焦点 vs 屏幕中心） |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.zoomCenterMode` | 属性 | 当前缩放中心模式 | ✅ TC-002、TC-003 |
| `Constants.ZOOM_CENTER_GESTURE` | 常量 | 以手势为中心 | ✅ TC-002、TC-003 |
| `Constants.ZOOM_CENTER_SCREEN` | 常量 | 以屏幕为中心 | ✅ TC-003 |
| `Constants.DEFAULT_ZOOM_CENTER_MODE` | 常量 | 默认模式 | ✅ TC-002 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 地图渲染 | 进入页面 | 地图正常显示 | `MapView` | UI | `renderMapSuccess` | P0 |
| TC-002 | 默认选手势中心 | 查看单选状态 | 默认选中 `radio_zoom_center_gesture`；API 为默认模式 | `zoomCenterMode` | UI+API | `defaultGestureCenterModeSelected` | P0 |
| TC-003 | 切换屏幕中心 | 选择 `radio_zoom_center_screen` | `zoomCenterMode` 更新 | `zoomCenterMode` | UI+API | `switchZoomCenterModeUpdatesUiSettings` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapZoomCenterModeScreenTest
```
