# 罗盘控件

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 罗盘控件 |
| 导航路径 | 主页 → **基础地图能力** → **罗盘控件** |
| 路由 | `main` → `map` → `compass` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/CompassScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/CompassScreenTest.kt` |
| Demo 目的 | 验证罗盘显示、朝北淡出、位置/尺寸及方位旋转 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.isCompassEnabled` / `setCompassEnabled` | 属性/方法 | 罗盘开关 | ✅ TC-002 |
| `UiSettings.isCompassFadeWhenFacingNorth` / `setCompassFadeFacingNorth` | 属性/方法 | 朝北淡出 | ✅ TC-003 |
| `UiSettings.getCompassGravity` / `setCompassGravity` | 方法 | 罗盘位置 | ✅ TC-004 |
| `UiSettings.setCompassViewSize` | 方法 | 罗盘尺寸 | — |
| `CameraUpdateFactory` / `animateCamera` | 工厂/方法 | 旋转地图方位 | ✅ TC-005 |
| `cameraPosition.bearing` | 属性 | 当前方位角 | ✅ TC-005 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 控件展示 | 进入页面 | 开关、下拉、旋转按钮可见 | — | UI | `testAllControlsDisplayed` | P1 |
| TC-002 | 罗盘开关 | 切换 `compass_switch` | `isCompassEnabled` 同步 | `setCompassEnabled` | UI+API | `testCompassSwitch_TogglesApi` | P0 |
| TC-003 | 朝北淡出 | 切换 `compass_fade_switch` | `isCompassFadeWhenFacingNorth` 同步 | `setCompassFadeFacingNorth` | UI+API | `testFadeWhenFacingNorth_TogglesApi` | P1 |
| TC-004 | 位置下拉 | 选择 `compass_gravity_dropdown` 各项 | `getCompassGravity` 更新 | `setCompassGravity` | UI+API | `testGravityDropdown_SelectsAndUpdatesApi` | P1 |
| TC-005 | 旋转按钮 | 点击 `compass_rotate_button` | bearing 改变 | `animateCamera` | UI+API | `testRotateButton_ChangesBearing` | P0 |
| TC-006 | 返回导航 | 按返回 | 回到子列表/主页 | — | UI | `testBack_ReturnsToMapItemList`、`testDoubleBack_ReturnsToMainScreen` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.CompassScreenTest
```
