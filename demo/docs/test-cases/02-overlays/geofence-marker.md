# 地理围栏(Marker)

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | 地理围栏(Marker) |
| 导航路径 | 主页 → **业务图层** → **地理围栏(Marker)** |
| 路由 | `main` → `overlays` → `geofence_marker` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/GeofenceMarkerScreen.kt` |
| 自动化测试 | —（**唯一**尚无 `*ScreenTest` 的 Demo，建议补充 `GeofenceMarkerScreenTest`） |
| Demo 目的 | 在围栏基础上验证 Marker 与围栏命中联动 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOverlayEngine.addPolygon` / `addCircle` | 方法 | 围栏几何 | — | 未覆盖 |
| `Fill.contains(LatLng)` | 方法 | 多边形命中 | — | 未覆盖 |
| Circle 半径距离判断 | 逻辑 | Point 围栏命中 | — | 未覆盖 |
| `addMarker` / `Marker.remove()` | 方法 | Marker 投放与清除 | — | 未覆盖 |
| `OnMapClickListener` | 监听 | 点击地图 | — | 未覆盖 |

*API 与 [地理围栏](./geofence.md) 相同，本页侧重 Marker 命中反馈。实现时可参照 `GeofenceScreenTest` 的地图点击与围栏断言模式。*

---

## 计划覆盖场景（待 `GeofenceMarkerScreenTest`）

| 编号 | 场景 | 预期 |
|------|------|------|
| S-001 | 页面加载 | 默认围栏与地图正常显示 |
| S-002 | 围栏内点击 | Marker 添加，状态提示围栏内 |
| S-003 | 围栏外点击 | 提示围栏外或拒绝添加 |
| S-004 | 清除 Marker | Marker 移除 |
| S-005 | 切换围栏类型 | Point/Polygon 下命中逻辑正确 |

---

## 自动化测试执行

```bash
# 待补充 GeofenceMarkerScreenTest 后启用
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.GeofenceMarkerScreenTest
```
