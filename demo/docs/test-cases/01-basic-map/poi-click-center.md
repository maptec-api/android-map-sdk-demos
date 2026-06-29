# Poi点击居中

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | Poi点击居中 |
| 导航路径 | 主页 → **基础地图能力** → **Poi点击居中** |
| 路由 | `main` → `map` → `poi_click_center` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/PoiClickScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/PoiClickViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/PoiClickScreenTest.kt`（3 个 `@Test`） |
| Demo 目的 | 验证 POI 点击查询、高亮标记与相机居中 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `addOnMapClickListener` | 监听 | 地图点击拾取 POI | ✅ TC-001 | — |
| `queryRenderedFeatures` | 方法 | 查询点击处要素 | ✅ TC-001 | — |
| `SymbolManager` / `Symbol` / `SymbolOptions` | 标注 | POI 高亮标记 | ✅ TC-001 | — |
| `animateCamera` / `CameraUpdateFactory` | 方法/工厂 | 相机移至 POI | ✅ TC-001 | — |
| `LatLng` | 类型 | POI 坐标 | ✅ TC-001 | — |
| 关闭开关后点击 | 行为 | 不移动相机 | — | 未覆盖（`testPoiClick_WhenDisabled_UpdatesUIButKeepsCamera` 已注释） |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 开启后点击 POI | 开启 `poi_center_switch`，点击地图 POI 区域 | UI 显示 POI 信息，相机移动 | `queryRenderedFeatures`、`animateCamera` | UI+API | `testPoiClick_WhenEnabled_UpdatesUIAndMovesCamera` | P0 |
| TC-002 | 清除按钮 | 有选中 POI 后点 `poi_clear_button` | POI 信息清空 | — | UI | `testClearButton_ResetsPoiInformation` | P1 |
| TC-003 | 开关清除选中 | 切换 `poi_center_switch` | 当前选中 POI 清除 | — | UI | `testToggleSwitch_ClearsCurrentSelection` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.PoiClickScreenTest
```

## 备注

- 测试依赖地图上存在可点击 POI 瓦片要素；无 POI 区域可能需调整点击坐标。
