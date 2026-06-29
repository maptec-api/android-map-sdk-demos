# 双击延迟

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 双击延迟 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **双击延迟** |
| 路由 | `main` → `map` → `map_gestures` → `map_double_tap_delay` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapDoubleTapDelayScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapDoubleTapDelayScreenTest.kt` |
| Demo 目的 | 验证自定义双击检测开关与双击延时滑条 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.isCustomDoubleTapDetectionEnabled` | 属性 | 自定义双击检测 | ✅ TC-002 |
| `UiSettings.doubleTapDelay` | 属性 | 双击间隔（ms） | ✅ TC-003 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 地图渲染 | 进入页面等待加载 | 地图正常显示 | `MapView` | UI | `renderMapSuccess` | P0 |
| TC-002 | 自定义双击开关 | 切换 `switch_custom_double_tap` | `isCustomDoubleTapDetectionEnabled` 同步 | `UiSettings` | UI+API | `toggleCustomDoubleTapSwitch` | P0 |
| TC-003 | 延时滑条 | 拖动 `slider_double_tap_delay` | 文案更新；`doubleTapDelay > 300` | `doubleTapDelay` | UI+API | `changeDoubleTapDelayViaSlider` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapDoubleTapDelayScreenTest
```
