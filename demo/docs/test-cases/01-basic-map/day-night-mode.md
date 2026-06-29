# 日夜模式

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 日夜模式 |
| 导航路径 | 主页 → **基础地图能力** → **日夜模式** |
| 路由 | `main` → `map` → `day_night_mode` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/DayNightScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/DayNightScreenTest.kt` |
| Demo 目的 | 验证内置日夜模式开关、控件位置及样式联动 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `MapOptions.dayNightModeEnabled` | 配置 | 初始化日夜模式 | — |
| `UiSettings.isDayNightModeEnabled` / `setDayNightModeEnabled` | 属性/方法 | 日夜模式开关 | ✅ TC-002 |
| `UiSettings.dayNightModeGravity` / `setDayNightModeGravity` | 属性/方法 | 切换按钮位置 | ✅ TC-004 |
| `MaptecMap.setStyle` | 方法 | 日夜样式联动 | ✅ TC-003 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 控件展示 | 进入页面 | 开关、位置下拉、标题栏可见 | — | UI | `testAllControlsDisplayed` | P1 |
| TC-002 | 日夜开关 | 切换 `switch_day_night_enabled` | `isDayNightModeEnabled` 同步 | `setDayNightModeEnabled` | UI+API | `testDayNightSwitch_TogglesApi` | P0 |
| TC-003 | 样式联动 | 切换内置日夜模式 | 地图样式随模式变化 | `setStyle` | UI+API | `testBuiltInDayNightToggle_ChangesMapStyle` | P0 |
| TC-004 | 位置下拉 | 选择 `dropdown_day_night_gravity` | `dayNightModeGravity` 更新 | `setDayNightModeGravity` | UI+API | `testGravityDropdown_SelectsAndUpdatesApi` | P1 |
| TC-005 | 面板折叠 | 点击 `day_night_title_bar` | 面板折叠/展开 | — | UI | `testTitleBar_CollapsesAndExpandsPanel` | P2 |
| TC-006 | 返回导航 | 按返回 | 回到子列表/主页 | — | UI | `testBack_ReturnsToMapItemList`、`testDoubleBack_ReturnsToMainScreen` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.DayNightScreenTest
```
