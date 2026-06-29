# 定位控件

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 定位控件 |
| 导航路径 | 主页 → **基础地图能力** → **定位控件** |
| 路由 | `main` → `map` → `location` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/LocationScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/LocationScreenViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/LocationScreenTest.kt` |
| Demo 目的 | 验证定位组件激活、相机/渲染模式、定位按钮 UI 及移动到当前位置 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `LocationComponent` / `LocationComponentOptions` | 组件 | 定位蓝点 | ✅ TC-003 ~ TC-009 |
| `LocationComponentActivationOptions` | 配置 | 激活参数 | ✅ TC-003 |
| `activateLocationComponent` / `applyStyle` | 方法 | 激活与样式 | ✅ TC-006 |
| `LocationComponent.cameraMode` | 属性 | 相机跟踪模式 | ✅ TC-010 |
| `LocationComponent.renderMode` | 属性 | 渲染模式 | ✅ TC-011 |
| `isLocationComponentEnabled` | 属性 | 组件开关 | ✅ TC-006、TC-007 |
| `CameraMode` / `RenderMode` | 枚举 | 模式选项 | ✅ TC-010、TC-011 |
| `UiSettings.setLocationViewEnabled` | 方法 | 定位按钮显示 | ✅ TC-012 |
| `UiSettings.locationViewGravity` 等 | 属性 | 按钮位置/边距/尺寸 | ✅ TC-013 |
| `map.animateCamera` | 方法 | 移动到当前位置 | ✅ TC-014 |
| `LocationEngine` / `LocationEngineDefault` | 服务 | 定位数据源 | —（需真机 GPS） |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 底部面板展示 | 进入页面 | `location_bottom_sheet` 可见 | — | UI | `bottomSheet_isDisplayed` | P1 |
| TC-002 | 面板控件齐全 | 展开面板 | 开关、滑条、下拉框均可见 | — | UI | `allPanelElements_areDisplayed` | P1 |
| TC-003 | 定位组件默认状态 | 进入页面 | 默认 TRACKING/COMPASS，组件已启用 | `cameraMode`、`isLocationComponentEnabled` | API | `locationComponent_defaultState` | P0 |
| TC-004 | 定位选项默认值 | 进入页面 | 脉冲、精度圈透明度为默认 | `LocationComponentOptions` | API | `locationComponentOptions_defaultValues` | P1 |
| TC-005 | 定位按钮默认状态 | 进入页面 | 定位按钮默认配置正确 | `UiSettings` | API | `locationView_defaultState` | P1 |
| TC-006 | 定位组件开关 | 切换 `switch_location_component_enabled` | 组件禁用后可重新启用 | `isLocationComponentEnabled` | UI+API | `toggleLocationComponent_disablesAndReenables` | P0 |
| TC-007 | 开关 UI 状态 | 切换开关 | Switch 状态与 API 一致 | 同上 | UI | `toggleLocationComponent_switchUiState` | P1 |
| TC-008 | 脉冲动画 | 切换 `switch_pulse_animation` | 脉冲样式 API 更新 | `applyStyle` | UI+API | `togglePulseAnimation_changesApi` | P1 |
| TC-009 | 精度圈透明度 | 拖动 `slider_accuracy_alpha` | 透明度 API 更新 | `LocationComponentOptions` | UI+API | `accuracyAlphaSlider_updatesApi` | P1 |
| TC-010 | 相机模式 | 选择相机模式下拉项 | `cameraMode` 更新 | `cameraMode` | UI+API | `cameraModeDropdown_selectModes` | P0 |
| TC-011 | 渲染模式 | 选择渲染模式下拉项 | `renderMode` 更新 | `renderMode` | UI+API | `renderModeDropdown_selectModes` | P0 |
| TC-012 | 定位按钮开关 | 切换 `switch_location_view_enabled` | `UiSettings` 定位按钮状态更新 | `setLocationViewEnabled` | UI+API | `toggleLocationViewEnabled_changesUiSettings` | P1 |
| TC-013 | 定位按钮位置 | 选择重力下拉项 | `locationViewGravity` 更新 | `locationViewGravity` | UI+API | `locationViewGravityDropdown_changesApi` | P1 |
| TC-014 | 移动到当前位置 | 点击 `button_move_to_current` | 按钮可见且可点击 | `animateCamera` | UI | `moveToCurrentLocationButton_isDisplayedAndEnabled` | P0 |
| TC-015 | 返回导航 | 按返回 | 回到地图子列表 / 主页 | — | UI | `back_returnsToMapItemList`、`doubleBack_returnsToMainScreen` | P1 |
| TC-016 | 重新进入 | 退出再进入 | 控件状态恢复 | — | UI | `reenter_allElementsRestored` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.LocationScreenTest
```

## 备注

- 需授予 `ACCESS_FINE_LOCATION`（测试通过 `GrantPermissionRule` 处理）。
- 内层 NavHost 页面，返回需先处理嵌套栈（见 `MainActivity` 注释）。
