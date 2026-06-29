# 地图 SDK Demo 测试用例总览

> 本文档描述 Demo App 测试用例体系。**快速查找请用 [INDEX.md](./INDEX.md)**。

## 覆盖率仪表盘

| 指标 | 数量 |
|------|------|
| Demo 功能点 | 36 |
| 有 `*ScreenTest` | **35** |
| 暂无仪器测试 | **1**（地理围栏 Marker） |
| 自动化测试类 | 35 |
| `@Test` 方法 | **~202** |
| 子文档 | 36 篇 |

> **原则**：子文档主表只列已有 `@Test` 对应用例；未覆盖 API 仅在 API 清单「缺口类型」标注，不单独铺「手工用例表」。

## 快速入口

| 文档 | 用途 |
|------|------|
| **[INDEX.md](./INDEX.md)** | 功能索引 + **方法名反查** |
| **[01-basic-map/map-render.md](./01-basic-map/map-render.md)** | 子文档标准模板 |

## 测试执行

在 **mapSdkDemo** 工程根目录、已连接设备时执行：

```bash
# 全量仪器测试（发版前）
./gradlew :demo:connectedOpenglDebugAndroidTest

# 单类
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapRenderScreenTest
```

### 回归策略

| 级别 | 范围 | 时机 |
|------|------|------|
| 冒烟 | `MapRenderScreenTest` + 搜索/图层/路线/鉴权各 1 类 | 每日 / PR |
| 全量 | 全部 35 个 `*ScreenTest`（~202 方法） | 发版前 |

## 前置条件

| 类型 | 要求 |
|------|------|
| 通用 | `INTERNET`、`ACCESS_NETWORK_STATE`；Android 9− 需存储权限 |
| 地图类 | 可访问样式服务；设备/模拟器可联网 |
| 定位 | `ACCESS_FINE_LOCATION`（`LocationScreenTest` 内授权） |
| 搜索 / 地理编码 / 路线 / 鉴权 | 有效 API Key |
| POI 点击 | 高 zoom 区域存在可点击 POI |

### 嵌套导航

- 基础地图能力 → 地图手势（11 子页）
- 业务图层 → 圆（8 子页）

仪器测试按文案 `performClick` 逐级进入；返回用 `onBackPressed` 或 TopBar。

## 图例

| 列名 | 含义 |
|------|------|
| **验证方式** | `UI` / `API` / `UI+API`（均为仪器测试） |
| **自动化** | `*ScreenTest` 中的 `@Test` 方法名 |
| **缺口类型**（API 表） | `未覆盖` / `环境依赖`（需 Mock 或特殊网络，暂不写测试） |

## 子文档标准模板

功能概述含：`源码` · `自动化测试` · `Demo 目的`

**测试用例表 8 列**：`编号` · `用例名称` · `操作步骤` · `预期结果` · `覆盖 API` · `验证方式` · `自动化` · `优先级`

**API 清单 5 列**：`SDK API` · `类型` · `Demo 说明` · `是否验证` · `缺口类型`

---

## 文档目录

### 01-basic-map（22 篇）

[map-render](./01-basic-map/map-render.md) · [map-control](./01-basic-map/map-control.md) · [map-gesture-control](./01-basic-map/map-gesture-control.md) · [map-gesture-coordination](./01-basic-map/map-gesture-coordination.md) · [map-double-tap-delay](./01-basic-map/map-double-tap-delay.md) · [map-zoom-center-mode](./01-basic-map/map-zoom-center-mode.md) · [map-double-tap-zoom-factor](./01-basic-map/map-double-tap-zoom-factor.md) · [map-gesture-threshold](./01-basic-map/map-gesture-threshold.md) · [map-rotate-bearing-range](./01-basic-map/map-rotate-bearing-range.md) · [map-fling-duration](./01-basic-map/map-fling-duration.md) · [map-zoom-animation-duration](./01-basic-map/map-zoom-animation-duration.md) · [map-two-finger-tap-zoom](./01-basic-map/map-two-finger-tap-zoom.md) · [map-double-tap-tilt-reset](./01-basic-map/map-double-tap-tilt-reset.md) · [location](./01-basic-map/location.md) · [compass](./01-basic-map/compass.md) · [logo](./01-basic-map/logo.md) · [zoom](./01-basic-map/zoom.md) · [scale-bar](./01-basic-map/scale-bar.md) · [map-event-listener](./01-basic-map/map-event-listener.md) · [map-style](./01-basic-map/map-style.md) · [day-night-mode](./01-basic-map/day-night-mode.md) · [poi-click-center](./01-basic-map/poi-click-center.md)

### 02-overlays（9 篇）

[marker](./02-overlays/marker.md) · [marker-overlay](./02-overlays/marker-overlay.md) · [marker-animation](./02-overlays/marker-animation.md) · [line](./02-overlays/line.md) · [polygon-fill](./02-overlays/polygon-fill.md) · [circle](./02-overlays/circle.md) · [markerview](./02-overlays/markerview.md) · [geofence](./02-overlays/geofence.md) · [geofence-marker](./02-overlays/geofence-marker.md)

### 03–07 服务类（5 篇）

[search](./03-search/search.md) · [geocode](./04-geocode/geocode.md) · [route](./05-route/route.md) · [route-overlay](./06-route-overlay/route-overlay.md) · [attention](./07-attention/attention.md)
