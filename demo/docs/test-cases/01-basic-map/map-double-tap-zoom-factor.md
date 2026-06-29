# 双击缩放系数

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 双击缩放系数 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **双击缩放系数** |
| 路由 | `main` → `map` → `map_gestures` → `map_double_tap_zoom_factor` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapDoubleTapZoomFactorScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapDoubleTapZoomFactorScreenTest.kt`（3 个 `@Test`） |
| Demo 目的 | 验证自定义双击缩放系数开关、滑条范围与 API 同步 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `UiSettings.isCustomDoubleTapZoomFactorEnabled` | 属性 | 自定义系数开关 | — | 未覆盖 |
| `UiSettings.doubleTapZoomFactor` | 属性 | 双击缩放系数 | ✅ TC-002、TC-003 | — |
| `SdkConstants.MIN/MAX/DEFAULT_DOUBLE_TAP_ZOOM_FACTOR` | 常量 | 系数范围与默认值 | ✅ TC-001、TC-003 | — |
| 双击手势缩放行为 | 手势 | 实际 zoom 增量 | — | 未覆盖 |

## 关键 testTag

`text_zoom_factor` · `slider_zoom_factor` · `text_zoom_level`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 默认系数显示 | 进入页面 | 文案显示默认系数值 | `DEFAULT_DOUBLE_TAP_ZOOM_FACTOR` | UI+API | `initialZoomFactorDisplaysDefaultValue` | P0 |
| TC-002 | 滑条同步 API | 拖动 `slider_zoom_factor` | `doubleTapZoomFactor` 与 UI 一致 | `doubleTapZoomFactor` | UI+API | `sliderChangesZoomFactorAndSyncsToApi` | P0 |
| TC-003 | 最小/最大钳制 | 滑条拖到两端 | 系数落在 MIN~MAX 范围内 | `MIN/MAX_DOUBLE_TAP_ZOOM_FACTOR` | UI+API | `sliderAtMinAndMaxClampsToRange` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapDoubleTapZoomFactorScreenTest
```
