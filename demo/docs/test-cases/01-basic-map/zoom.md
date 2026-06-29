# 缩放控件

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 缩放控件 |
| 导航路径 | 主页 → **基础地图能力** → **缩放控件** |
| 路由 | `main` → `map` → `zoom` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/ZoomScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/ZoomScreenTest.kt` |
| Demo 目的 | 验证缩放按钮、级别显示、位置/精度/尺寸等 UI 配置 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.setZoomButtonsEnabled` / `isZoomButtonsEnabled` | 方法/属性 | 缩放按钮开关 | ✅ TC-002 |
| `UiSettings.isZoomLevelVisible` / `setZoomLevelVisible` | 属性/方法 | 级别文字显示 | ✅ TC-004 |
| `UiSettings.zoomButtonsGravity` | 属性 | 按钮位置 | ✅ TC-003 |
| `UiSettings.setZoomButtonsSize` / `zoomButtonsSize` | 方法/属性 | 按钮尺寸 | ✅ TC-005 |
| `UiSettings.setZoomLevelFormat` / `zoomLevelFormat` | 方法/属性 | 小数精度 | ✅ TC-006 |
| `UiSettings.setZoomLevelPosition` / `zoomLevelPosition` | 方法/属性 | 级别文字位置 | ✅ TC-007 |
| `ZoomButtonsView.ZOOM_LEVEL_POSITION_*` | 常量 | 级别位置枚举 | ✅ TC-007 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 控件展示 | 进入页面滚动面板 | 全部 Switch/下拉/滑条可见 | — | UI | `testAllControlsDisplayed` | P1 |
| TC-002 | 缩放按钮开关 | 切换 `switch_zoom_enabled` | `isZoomButtonsEnabled` 同步 | `setZoomButtonsEnabled` | UI+API | `toggleEnableZoomButtons` | P0 |
| TC-003 | 按钮位置 | 遍历 `dropdown_zoom_gravity` 全部选项 | `zoomButtonsGravity` 逐项更新 | `zoomButtonsGravity` | UI+API | `selectAllGravityOptions` | P1 |
| TC-004 | 级别可见性 | 切换 `switch_zoom_level_visible` | `isZoomLevelVisible` 同步 | `setZoomLevelVisible` | UI+API | `toggleZoomLevelVisible_updatesApi` | P0 |
| TC-005 | 按钮尺寸 | 拖动 `slider_zoom_button_size` | `zoomButtonsSize` 更新 | `setZoomButtonsSize` | UI+API | `sliderZoomButtonSize_updatesApi` | P1 |
| TC-006 | 缩放精度 | 选择 `dropdown_zoom_precision` | `zoomLevelFormat` 更新 | `setZoomLevelFormat` | UI+API | `selectZoomPrecision_updatesApi` | P1 |
| TC-007 | 级别位置 | 选择 `dropdown_zoom_level_position` | `zoomLevelPosition` 更新 | `setZoomLevelPosition` | UI+API | `selectZoomLevelPosition_updatesApi` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.ZoomScreenTest
```
