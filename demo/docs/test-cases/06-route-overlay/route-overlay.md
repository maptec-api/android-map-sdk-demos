# 路线规划(Overlay)

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 路线规划(Overlay) |
| 导航路径 | 主页 → **路线规划(Overlay)** |
| 路由 | `main` → `route_overlay` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/RouteOverlayScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/RouteOverlayViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/RouteOverlayScreenTest.kt`（2 个 `@Test`） |
| Demo 目的 | 使用 Overlay 引擎绘制导航线（`NavigationLine`）而非 Annotation `LineManager` |

## 与路线规划（Annotation）对比

| 维度 | [路线规划](../05-route/route.md) | 本页 (Overlay) |
|------|----------------------------------|----------------|
| 路由 | `main` → `route` | `main` → `route_overlay` |
| 算路 API | `RouteService.calculateRoute` | 相同 |
| 结果绘制 | `LineManager` | `addNavigationLine` |
| 自动化断言 | `route_summary_card` | `route_summary_card` + `route_overlay_has_navigation_lines` |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `RouteService.calculateRoute` | 方法 | 算路 | ✅ TC-002 | — |
| `MaptecMap.getOverlayEngine()` | 方法 | Overlay 引擎 | ✅ TC-002 | — |
| `engine.addNavigationLine(NavigationLineOptions)` | 方法 | 导航线 | ✅ TC-002 | — |
| `engine.deleteAllNavigationLines` | 方法 | 清除路线 | — | 未覆盖 |
| `CameraUpdateFactory` | 工厂 | 路线全览 | — | 低优先级 |
| 货车模式 / 途经点 | 配置 | `RouteRequest` 扩展参数 | — | 未覆盖 |

## 关键 testTag

`route_config_sheet` · `start_route_button` · `route_summary_card` · `route_overlay_has_navigation_lines`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 页面加载 | 进入页面 | 配置面板与地图可见 | `MapView` | UI | `testNavigationAndMapLoads` | P0 |
| TC-002 | 标准驾车算路 | 点击算路 | 汇总卡片 + 导航线指示器 | `calculateRoute`、`addNavigationLine` | UI | `testRouteCalculation_showsSummaryAndNavigationLine` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.RouteOverlayScreenTest
```
