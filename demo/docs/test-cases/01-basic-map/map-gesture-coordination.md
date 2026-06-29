# 手势协调（互斥/同时）

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 手势协调 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **手势协调（互斥/同时）** |
| 路由 | `main` → `map` → `map_gestures` → `map_gesture_coordination` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapGestureCoordinationScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapGestureCoordinationScreenTest.kt` |
| Demo 目的 | 验证多手势同时允许与互斥分组配置 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.getGestures()` | 方法 | 获取 `GestureSettings` | ✅ 全部 |
| `GestureSettings.setSimultaneousGesturesAllowed` / `isSimultaneousGesturesAllowed` | 方法/属性 | 允许多 Progressive 手势同时 | ✅ TC-001 ~ TC-005 |
| `GestureSettings.setMutuallyExclusiveGestures` / `getMutuallyExclusiveGestures` | 方法 | 设置互斥手势分组 | ✅ TC-006 ~ TC-013 |
| `MapGestureType` | 枚举 | Scale/Rotate/Shove 等 | ✅ TC-006 ~ TC-013 |

---

## 测试用例

### 同时手势

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 同时手势默认开启 | 进入页面查看 Switch | `switch_simultaneous_gestures` 默认 ON | `GestureSettings.setSimultaneousGesturesAllowed` | UI | `testSimultaneousSwitch_DefaultOn` | P0 |
| TC-002 | 关闭同时手势 | 关闭 Switch | API 同步为 false | 同上 | UI+API | `testSimultaneousSwitch_ToggleOff` | P0 |
| TC-003 | 关闭时互斥 UI 禁用 | 关闭同时手势 | 复选框与确定按钮不可用 | — | UI | `testSimultaneousSwitch_CheckboxesAndButtonDisabledWhenOff` | P1 |
| TC-004 | 开启时互斥 UI 可用 | 开启同时手势 | 复选框与确定按钮可用 | — | UI | — | P1 |
| TC-005 | 关闭时确认不生效 | 选互斥后关同时手势再确认 | 互斥配置不写入 | `GestureSettings.setMutuallyExclusiveGestures` | UI+API | `testSimultaneousSwitch_ToggleOff_ExcludesMutualExclusionConfirmation` | P1 |

### 互斥分组

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-006 | 选择缩放+旋转互斥 | 勾选 Scale、Rotate 后确定 | 互斥组写入 API | `GestureSettings.setMutuallyExclusiveGestures` | UI+API | `testMutualExclusion_SelectScaleAndRotate` | P0 |
| TC-007 | 全选手势互斥 | 勾选全部手势后确定 | 互斥组包含所有选手势 | 同上 | UI+API | `testMutualExclusion_SelectAllGestures` | P1 |
| TC-008 | 开关切换保留选择 | 关→开同时手势 | 复选框选择保留 | — | UI | `testMutualExclusion_ToggleOnThenOffThenOn_PreservesSelection` | P2 |
| TC-009 | 清空选择 | 取消全部勾选 | 复选框均为未选 | — | UI | `testMutualExclusion_ClearSelection` | P2 |
| TC-010 | 空选确定清空互斥 | 不选手势点确定 | 互斥组清空 | `GestureSettings.setMutuallyExclusiveGestures` | UI+API | `testMutualExclusion_EmptyConfirmClearsMutualExclusion` | P1 |
| TC-011 | 多次确定替换分组 | 先后确认不同分组 | 以最后一次为准 | 同上 | UI+API | `testMutualExclusion_MultipleConfirmsReplacesGroup` | P1 |
| TC-012 | 取消勾选后确定 | 取消部分勾选再确定 | 分组更新 | 同上 | UI+API | `testMutualExclusion_UncheckAndConfirmReplacesGroup` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapGestureCoordinationScreenTest
```
