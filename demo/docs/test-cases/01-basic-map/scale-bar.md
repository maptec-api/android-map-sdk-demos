# 比例尺

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 比例尺 |
| 导航路径 | 主页 → **基础地图能力** → **比例尺** |
| 路由 | `main` → `map` → `scale_bar` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/ScaleScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/ScaleScreenTest.kt`（3 个 `@Test`） |
| Demo 目的 | 验证比例尺开关、位置与最大宽度 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOptions.scaleBarEnabled` | 配置 | 初始化比例尺 | — | — |
| `UiSettings.isScaleBarEnabled` | 属性 | 比例尺开关 | ✅ TC-001 | — |
| `UiSettings.scaleBarGravity` | 属性 | 比例尺位置 | ✅ TC-002 | — |
| `ScaleView.setScaleBarMaxWidthPx` / `scaleBarMaxWidthPx` | 方法/属性 | 最大宽度 | ✅ TC-003 | — |
| `ScaleView.SCALE_BAR_MAX_WIDTH_*` | 常量 | 默认宽度 | ✅ TC-003 | — |
| `ScaleView.refreshScaleView` | 方法 | 刷新比例尺 | — | 低优先级 |
| 最大宽度滑条 | UI | `slider_scale_bar_max_width` | — | 未覆盖 |

## 关键 testTag

`mapView` · `switch_scale_enabled` · `dropdown_scale_gravity` · `slider_scale_bar_max_width`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 比例尺开关 | 切换 `switch_scale_enabled` | `isScaleBarEnabled` 同步 | `isScaleBarEnabled` | UI+API | `scaleScreen_toggleScaleBarEnabled` | P0 |
| TC-002 | 位置切换 | 选择 `dropdown_scale_gravity` 各项 | `scaleBarGravity` 更新 | `scaleBarGravity` | UI+API | `scaleScreen_changeScaleBarGravity` | P1 |
| TC-003 | 默认最大宽度 | 进入页面 | 与 SDK 默认一致 | `scaleBarMaxWidthPx` | API | `scaleScreen_defaultScaleBarMaxWidth_matchesSdkDefault` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.ScaleScreenTest
```
