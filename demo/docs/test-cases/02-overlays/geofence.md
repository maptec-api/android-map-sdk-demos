# 地理围栏

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | 地理围栏 |
| 导航路径 | 主页 → **业务图层** → **地理围栏** |
| 路由 | `main` → `overlays` → `geofence` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/GeofenceScreen.kt`、`src/main/java/com/maptec/applied/demo/data/GeofenceData.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/GeofenceScreenTest.kt` |
| Demo 目的 | 验证 Point/Polygon 围栏加载、命中检测与 Marker 投放 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `MapOverlayEngine.addPolygon` / `addCircle` | 方法 | 围栏几何 | ✅ TC-002 ~ TC-004 |
| `deleteAllFills` / `deleteAllCircles` | 方法 | 清除围栏 | ✅ TC-005 |
| `Fill.contains(LatLng)` | 方法 | 多边形内点判断 | ✅ TC-006 |
| `FillOptions` / `CircleOptions` | 配置 | 围栏样式 | ✅ TC-002、TC-003 |
| `addMarker` / `Marker.remove()` | 方法 | 点击投放 Marker | ✅ TC-006 |
| `OnMapClickListener` | 监听 | 地图点击 | ✅ TC-006 |
| 要素查询 | `queryOverlayHitsFromPoint` + `resolveHitTarget` | — |
| `CameraUpdateFactory` | 工厂 | 加载后相机 | — |
| GeoJSON `Feature`/`Point`/`Polygon` | 数据 | 围栏数据解析 | ✅ TC-002、TC-003 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 页面展示 | 进入页面 | 地图、状态文案可见 | — | UI | `testScreenDisplayed` | P0 |
| TC-002 | 默认多边形围栏 | 进入页面 | 默认已加载 Polygon 围栏 | `addPolygon` | UI+API | `testDefaultPolygonFence_loaded` | P0 |
| TC-003 | 加载 Point 围栏 | 点击加载 Point | 增加 Circle 围栏 | `addCircle` | UI+API | `testLoadPointFence_addsCircle` | P0 |
| TC-004 | 加载 Polygon 围栏 | 点击加载 Polygon | 多边形围栏保持 | `addPolygon` | UI+API | `testLoadPolygonFence_keepsFillAtInsidePoint` | P1 |
| TC-005 | 清除全部 | 点击清除 | 围栏移除 | `deleteAll*` | UI+API | `testClearAll_removesFences` | P0 |
| TC-006 | 多边形内点击 | 在多边形内点击地图 | 状态更新；可投放 Marker | `Fill.contains` | UI | `testMapClick_insidePolygon_updatesStatus` | P0 |
| TC-007 | 清除 Marker 按钮 | 进入页面 | 清除 Marker 按钮可见 | — | UI | `testClearMarker_buttonDisplayed` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.GeofenceScreenTest
```
