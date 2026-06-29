# 地图事件监听

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 地图事件监听 |
| 导航路径 | 主页 → **基础地图能力** → **地图事件监听** |
| 路由 | `main` → `map` → `map_event_listener` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/MapEventListenerScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapEventListenerScreenTest.kt`（6 个 `@Test`） |
| Demo 目的 | 验证地图点击、长按、相机移动与 idle 等事件监听日志 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `addOnMapClickListener` | 监听 | 地图点击 | ✅ TC-003 | — |
| `addOnMapLongClickListener` | 监听 | 地图长按 | ✅ TC-004 | — |
| `addOnCameraMoveListener` | 监听 | 相机移动 | ✅ TC-005 | — |
| `addOnCameraIdleListener` | 监听 | 相机静止 | ✅ TC-006 | — |
| `addOnCameraMoveStartedListener` | 监听 | 相机开始移动 | — | 未覆盖 |
| `addOnFlingListener` | 监听 | 惯性滑动 | — | 未覆盖 |

## 关键 testTag

`switch_map_click` · `switch_map_long_click` · `switch_camera_move` · `switch_camera_idle` · `btn_clear_logs`

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 开关默认关闭 | 进入页面 | 各事件 Switch 均为 OFF | — | UI | `testMapReadyAndSwitchesInitiallyOff` | P1 |
| TC-002 | 清除日志 | 有点击日志后清除 | 日志列表清空 | — | UI | `testClearLogs_removesEntries` | P1 |
| TC-003 | 点击事件 | 开启点击并 tap 地图 | 日志含 Map Click | `OnMapClickListener` | UI | `testMapClick_enabled_generatesLogEntry` | P0 |
| TC-004 | 长按事件 | 开启长按并 long-press | 日志含 Map Long Click | `OnMapLongClickListener` | UI | `testMapLongClick_enabled_generatesLogEntry` | P0 |
| TC-005 | 相机移动 | 开启 move 并拖动地图 | 日志含 Camera Move | `OnCameraMoveListener` | UI | `testCameraMove_enabled_generatesLogEntry` | P1 |
| TC-006 | 相机 idle | 开启 idle 并拖动 | 日志含 Camera Idle | `OnCameraIdleListener` | UI | `testCameraIdle_enabled_generatesLogEntry` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapEventListenerScreenTest
```
