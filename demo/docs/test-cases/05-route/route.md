# 路线规划

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 路线规划 |
| 导航路径 | 主页 → **路线规划** |
| 路由 | `main` → `route` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/RouteScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/RouteViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/RouteScreenTest.kt` |
| Demo 目的 | 验证驾车/货车算路、途经点、规避区域与结果展示（LineManager） |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `RouteService.getInstance` | 单例 | 路径规划服务 | ✅ TC-005 | — |
| `RouteService.calculateRoute(RouteRequest)` | 方法 | 算路请求 | ✅ TC-005 | — |
| `RouteRequest` 及子字段 | 配置 | 起终点/策略/规避/货车参数 | ✅ TC-002 ~ TC-005 | — |
| `MapView` / `MaptecMap` / `setStyle` | 地图 | 路线底图 | ✅ TC-001 | — |
| `LineManager` | 标注 | 路线折线（多 zoom + 箭头） | ✅ TC-005 | — |
| `GeoJsonOptions` | 配置 | 线样式 | — | 低优先级 |
| `PolylineUtils.decode` | 工具 | 解码路线几何 | ✅ TC-005 | — |
| `CameraUpdateFactory.newLatLngBounds` | 工厂 | 路线全览 | ✅ TC-005 | — |
| `OnLineClickListener` | 监听 | 备选路线切换 | — | 未覆盖 |
| 规避收费/高速策略 | 配置 | `RouteRequest` 规避项 | — | 未覆盖 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 导航与地图加载 | 进入页面 | 地图加载成功 | `MapView` | UI | `testNavigationAndMapLoads` | P0 |
| TC-002 | 自定义区域规避 | 切换区域规避开关 | UI 状态更新 | `RouteRequest` | UI | `testCustomAreaToggle` | P1 |
| TC-003 | 货车模式 | 切换货车模式 | 货车参数区显示 | `RouteRequest` | UI | `testTruckModeToggle` | P1 |
| TC-004 | 途经点格式错误 | 输入非法途经点格式 | 算路被阻止/提示错误 | 校验逻辑 | UI | `testWaypointsInput_invalidFormat_blocksRouting` | P1 |
| TC-005 | 算路结果验证 | 填写起终点点击算路 | API 返回路线，地图绘制折线 | `calculateRoute`、`LineManager` | UI+API | `testRouteCalculation_verifyApiResult` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.RouteScreenTest
```
