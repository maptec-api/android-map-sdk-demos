# 地图渲染

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 地图渲染 |
| 导航路径 | 主页 → **基础地图能力** → **地图渲染** |
| 路由 | `main` → `map` → `map_render` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/MapRenderScreen.kt`（`MapScreen`） |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapRenderScreenTest.kt`（9 个 `@Test`） |
| Demo 目的 | 验证地图初始化渲染、样式加载、天空盒、标签语言、调试模式 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapView` / `getMapAsync` / `setStyle` | 地图 | 初始化与样式加载 | ✅ TC-001 | — |
| `StyleStatusCallback.onStyleRendered` | 回调 | 样式渲染完成 | ✅ TC-001 | — |
| `MaptecMap.setSkyEnabled` / `isSkyEnabled` | 方法/属性 | 天空盒 | ✅ TC-002 ~ TC-004 | — |
| `MaptecMap.setLanguage` / `language` | 方法/属性 | 标签语言 | ✅ TC-005 ~ TC-008 | — |
| `MaptecMap.setDebugActive` / `isDebugActive` | 方法/属性 | 调试模式 | ✅ TC-009 | — |
| `MapView.addOnMapHttpErrorListener` | 方法 | HTTP 错误 Toast | — | 环境依赖 |
| `StyleStatusCallback.onFailed` | 回调 | 样式失败 | — | 环境依赖 |

## 关键 testTag

`mapView` · `map_switch_sky_enabled` · `map_language_dropdown` · `map_btn_debug_toggle` · `mapRendered`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 地图正常渲染 | 进入地图渲染页 | `mapView` 可见，样式渲染完成 | `MapView`、`setStyle` | UI | `testMapRendered_success` | P0 |
| TC-002 | 天空盒默认关闭 | 展开面板查看 Switch | OFF；`isSkyEnabled == false` | `isSkyEnabled` | UI+API | `testSkySwitch_defaultOff_apiVerified` | P0 |
| TC-003 | 开启天空盒 | 打开天空盒 Switch | ON；`isSkyEnabled == true` | `setSkyEnabled` | UI+API | `testSkySwitch_toggleOn_updatesApi` | P0 |
| TC-004 | 关闭天空盒 | 再次关闭 Switch | OFF；`isSkyEnabled == false` | `setSkyEnabled` | UI+API | `testSkySwitch_toggleOff_updatesApi` | P0 |
| TC-005 | 默认语言 | 读取 `map.language` | 默认 `zh` | `language` | API | `testLanguageDropdown_defaultIsEngineDefault` | P1 |
| TC-006 | 切换英语 | 下拉选英语 | `language == "en"` | `setLanguage` | UI+API | `testLanguageSelect_english_updatesApi` | P0 |
| TC-007 | 切换中文 | 下拉选中文 | `language == "zh"` | `setLanguage` | UI+API | `testLanguageSelect_chinese_updatesApi` | P0 |
| TC-008 | 恢复默认语言 | 下拉选默认 | `language == ""` | `setLanguage` | UI+API | `testLanguageSelect_backToDefault_updatesApi` | P1 |
| TC-009 | 调试模式 | 点击调试按钮 | `isDebugActive` 取反 | `setDebugActive` | UI+API | `testDebugToggle_changesApi` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapRenderScreenTest
```

## 备注

- 底部 `BottomSheet` 需 `SemanticsActions.Expand` 展开后再操作。
- 语言下拉操作期间需暂停 `mainClock.autoAdvance` 避免 idle 死锁。
