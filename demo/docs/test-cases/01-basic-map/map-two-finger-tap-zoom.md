# 双指点击缩放

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 双指点击缩放 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **双指点击缩放** |
| 路由 | `main` → `map` → `map_gestures` → `map_two_finger_tap_zoom` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapTwoFingerTapZoomScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapTwoFingerTapZoomScreenTest.kt` |
| Demo 目的 | 验证双指点击缩放手势开关 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.isTwoFingerTapZoomEnabled` | 属性 | 双指点击缩放 | ✅ TC-001 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 双指点击缩放开关 | 切换 `switch_two_finger_tap_zoom` | `isTwoFingerTapZoomEnabled` 与 Switch 同步 | `isTwoFingerTapZoomEnabled` | UI+API | `toggleTwoFingerTapZoomSwitch` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapTwoFingerTapZoomScreenTest
```
