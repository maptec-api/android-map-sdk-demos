# Marker(Overlay)

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | Marker(Overlay) |
| 导航路径 | 主页 → **业务图层** → **Marker(Overlay)** |
| 路由 | `main` → `overlays` → `marker_overlay` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/MarkerLayerScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/MarkerLayerViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/MarkerLayerScreenTest.kt` |
| Demo 目的 | 验证 Overlay 引擎 `MapOverlayEngine.addMarker` 与 Marker 属性 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOverlayEngine.addMarker(MarkerOptions)` | 方法 | 添加 Marker | ✅ TC-002、TC-003 |
| `Marker` | 对象 | 图标/文字/透明度/拖拽 | ✅ TC-002、TC-007 |
| `MarkerOptions.withIcon` | 配置 | 图标类型 | ✅ TC-002 |
| `OnMarkerDragListener` | 监听 | 拖拽 | — | 未覆盖 |
| `Property.ICON_ANCHOR_*` | 常量 | 锚点 | — | 低优先级 |
| `BitmapUtils` | 工具 | 图标资源 | ✅ TC-004 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 页面展示 | 进入页面 | 地图与关键控件可见 | — | UI | `testScreenDisplayed` | P0 |
| TC-002 | 按类型添加 | 点击添加 | 引擎中 Marker 属性符合默认 | `addMarker` | UI+API | `testAddMarkerByType_apiVerified` | P0 |
| TC-003 | 多次添加 | 连续添加两次 | 计数增加 | `addMarker` | UI+API | `testAddMarkerByType_twice_increasesCount` | P1 |
| TC-004 | SDF 图标色 | 添加 SDF Marker | 图标颜色正确 | `MarkerOptions` | API | `testAddMarkerBySdf_iconColorVerified` | P1 |
| TC-005 | 清除全部 | 点击清除 | 引擎 Marker 清空 | `deleteMarker` | UI+API | `testClearAll_removesMarkers` | P0 |
| TC-006 | 非法坐标 | 输入无效 lat/lng | 添加按钮禁用 | 校验逻辑 | UI | `testInvalidLatLng_addButtonsDisabled` | P1 |
| TC-007 | 自定义坐标 | 输入合法坐标添加 | Marker 落在指定位置 | `Marker.getPosition` | API | `testCustomLatLng_markerPlacedAtPosition` | P0 |
| TC-008 | 图标尺寸 | 修改 icon size | 应用到 Marker | `Marker` 尺寸 | API | `testCustomIconSize_appliedToMarker` | P1 |
| TC-009 | 随缩放级别 | 开启 icon scale with zoom | 显示 min/max zoom 字段 | — | UI | `testIconScaleWithZoom_showsZoomFields` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.MarkerLayerScreenTest
```
