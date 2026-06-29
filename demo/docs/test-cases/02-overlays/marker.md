# Marker（Symbol 图层）

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | Marker |
| 导航路径 | 主页 → **业务图层** → **Marker** |
| 路由 | `main` → `overlays` → `marker` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/SymbolLayerScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/SymbolLayerViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/SymbolLayerScreenTest.kt`（5 个 `@Test`） |
| Demo 目的 | 验证 Symbol 标注图层增删、点击、拖拽与缩放配置 |

> 与 [Marker(Overlay)](./marker-overlay.md) 区别：本页使用 **Annotation `SymbolManager`**；Overlay 页使用 **`MapOverlayEngine.addMarker`**。

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `SymbolManager` | 管理器 | Symbol 图层管理 | ✅ TC-001、TC-004 | — |
| `Symbol` / `SymbolOptions` | 对象/配置 | 创建标注 | ✅ TC-001、TC-003 | — |
| `OnSymbolClickListener` | 监听 | 点击选中 | — | 未覆盖 |
| `OnSymbolDragListener` | 监听 | 拖拽移动 | — | 未覆盖 |
| `Property.ICON_ANCHOR_*` | 常量 | 图标锚点 | — | 低优先级 |
| `BitmapUtils` | 工具 | 图标位图 | ✅ TC-003 | — |
| `LatLng` | 类型 | 坐标 | ✅ TC-005 | — |

## 关键 testTag

`symbol_btn_add_by_type` · `symbol_btn_add_sdf` · `symbol_btn_clear_all` · `symbol_layer_has_markers`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 按类型添加 | 点击按类型添加 | `symbol_layer_has_markers` 出现 | `SymbolManager` | UI | `testAddSymbolByType_showsMarkerIndicator` | P0 |
| TC-002 | 多次添加 | 连续添加两次 | 指示器保持存在 | `SymbolManager` | UI | `testAddSymbolByType_twice_increasesIndicator` | P0 |
| TC-003 | SDF 添加 | 设置颜色后添加 SDF | 指示器出现 | `SymbolOptions` | UI | `testAddSymbolBySdf_showsMarkerIndicator` | P0 |
| TC-004 | 清除全部 | 添加后点击清除 | 指示器消失 | `SymbolManager.delete` | UI | `testClearAll_removesMarkerIndicator` | P0 |
| TC-005 | 非法坐标 | 输入无效 lat/lng | 添加按钮禁用 | 校验逻辑 | UI | `testInvalidLatLng_addButtonsDisabled` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.SymbolLayerScreenTest
```
