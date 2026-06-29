# 地图手势控制

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 地图手势控制 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **地图手势控制** |
| 路由 | `main` → `map` → `map_gestures` → `map_gesture_control` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapGestureScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapGestureScreenTest.kt` |
| Demo 目的 | 验证 `UiSettings` 各手势开关（平移/旋转/倾斜/缩放等） |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.setAllGesturesEnabled` / `areAllGesturesEnabled` | 方法/属性 | 总开关 | ✅ TC-001 |
| `UiSettings.isScrollGesturesEnabled` | 属性 | 平移手势 | ✅ TC-002 |
| `UiSettings.isRotateGesturesEnabled` | 属性 | 旋转手势 | ✅ TC-003 |
| `UiSettings.isTiltGesturesEnabled` | 属性 | 倾斜手势 | ✅ TC-004 |
| `UiSettings.isZoomGesturesEnabled` | 属性 | 双指缩放手势 | ✅ TC-005 |
| `UiSettings.isDoubleTapGesturesEnabled` | 属性 | 双击缩放手势 | ✅ TC-006 |

---

## 测试用例

| 编号 | 用例名称 | 前置条件 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 总手势开关 | 已进入页面 | 切换 `switch_all_gesture` | `areAllGesturesEnabled` 与 Switch 同步 | `setAllGesturesEnabled` | UI+API | `toggleAllGestureSwitch` | P0 |
| TC-002 | 平移手势 | 已进入页面 | 切换 `switch_scroll_gesture` | `isScrollGesturesEnabled` 同步 | `UiSettings` 平移 | UI+API | `toggleScrollGestureSwitch` | P0 |
| TC-003 | 旋转手势 | 已进入页面 | 切换 `switch_rotate_gesture` | `isRotateGesturesEnabled` 同步 | `UiSettings` 旋转 | UI+API | `toggleRotateGestureSwitch` | P0 |
| TC-004 | 倾斜手势 | 已进入页面 | 切换 `switch_tilt_gesture` | `isTiltGesturesEnabled` 同步 | `UiSettings` 倾斜 | UI+API | `toggleTiltGestureSwitch` | P0 |
| TC-005 | 双指缩放 | 已进入页面 | 切换 `switch_double_finger_zoom_gesture` | `isZoomGesturesEnabled` 同步 | `UiSettings` 缩放 | UI+API | `toggleDoubleFingerZoomGestureSwitch` | P0 |
| TC-006 | 双击缩放 | 已进入页面 | 切换 `switch_double_click_zoom_gesture` | `isDoubleTapGesturesEnabled` 同步 | `UiSettings` 双击 | UI+API | `toggleDoubleClickZoomGestureSwitch` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapGestureScreenTest
```
