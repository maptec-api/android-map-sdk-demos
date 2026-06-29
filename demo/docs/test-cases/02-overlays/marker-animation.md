# Marker动画

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | Marker动画 |
| 导航路径 | 主页 → **业务图层** → **Marker动画** |
| 路由 | `main` → `overlays` → `marker_animation` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/MarkerAnimationScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/MarkerAnimationViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/MarkerAnimationScreenTest.kt` |
| Demo 目的 | 验证 Marker 入场/消失/选中动画及 Overlay 动画引擎 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `MapOverlayEngine.addMarker` / `deleteMarker` | 方法 | Marker 生命周期 | ✅ TC-003 ~ TC-006 |
| `Circle` / `CircleOptions` | 对象 | 脉动光晕 | — |
| `AlphaAnimation` / `OffsetAnimation` / `ScaleAnimation` | 动画 | 入场动画 | ✅ TC-010 ~ TC-012 |
| `PulsatingAnimation` | 动画 | 脉动/选中 | ✅ TC-013、TC-014 |
| `AnimationSet` | 组合 | 动画集合 | ✅ TC-010 |
| `engine.startAnimation` | 方法 | 启动动画 | ✅ TC-010 |
| `OnMapClickListener` | 监听 | 点击添加/触发动画 | ✅ TC-006、TC-013 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 页面展示 | 进入页面 | 地图、顶栏、面板可见 | — | UI | `testScreenDisplayed` | P0 |
| TC-002 | 默认入场模式 | 进入页面 | 顶栏显示入场相关按钮 | — | UI | `testTopBar_defaultEnterMode_showsEnterButtons` | P1 |
| TC-003 | 添加 Marker | 点击添加 | API 验证 Marker 存在 | `addMarker` | UI+API | `testAddMarker_apiVerified` | P0 |
| TC-004 | 多次添加 | 添加两次 | 计数增加 | `addMarker` | UI+API | `testAddMarker_twice_increasesCount` | P1 |
| TC-005 | 清除 | 点击清除 | Marker 移除 | `deleteMarker` | UI+API | `testClearAll_removesMarkers` | P0 |
| TC-006 | 地图点击添加 | 点击地图 | 新增 Marker | `OnMapClickListener` | UI | `testMapClick_addsMarker` | P1 |
| TC-007 | 切换消失模式 | 切换主模式 | 顶栏按钮变化 | — | UI | `testMainAnimationMode_switchToDisappear_changesTopBarButtons` | P1 |
| TC-008 | 入场区启用 | 入场模式 | 入场配置区可用 | — | UI | `testMainAnimationMode_enterMode_enablesEnterSection` | P1 |
| TC-009 | 消失区启用 | 消失模式 | 消失配置区可用 | — | UI | `testMainAnimationMode_disappearMode_enablesDisappearSection` | P1 |
| TC-010 | 淡入动画 | 选 FadeIn 并播放 | 无崩溃 | `AlphaAnimation` | UI | `testEnterAnimation_selectFadeIn_startAndEnd_noCrash` | P0 |
| TC-011 | 无 Marker 播放入场 | 不添加直接播放 | 无崩溃 | `startAnimation` | UI | `testEnterAnimation_startWithoutMarkers_noCrash` | P2 |
| TC-012 | 掉落动画 | 选 Drop 并播放 | 无崩溃 | `OffsetAnimation` | UI | `testEnterAnimation_selectDrop_start_noCrash` | P1 |
| TC-013 | 选中 Bounce | 选 Bounce 后点地图 | 无崩溃 | `PulsatingAnimation` | UI | `testSelectAnimation_selectBounce_mapClick_noCrash` | P1 |
| TC-014 | 选中 Pulse | 选 Pulse 后点地图 | 无崩溃 | 同上 | UI | `testSelectAnimation_selectPulse_mapClick_noCrash` | P1 |
| TC-015 | 时长输入 | 修改入场时长 | 输入框接受数值 | — | UI | `testEnterDurationInput_acceptsValue` | P2 |
| TC-016 | 重复增删 | 多次添加清除再添加 | 流程成功 | `addMarker` | UI | `testMultipleAddClearAndReadd_succeeds` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.MarkerAnimationScreenTest
```

## 备注

- 部分消失动画用例在源码中已注释，文档未收录。
