# 手势阈值

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 三级入口 | 地图手势 → 手势阈值 |
| 导航路径 | 主页 → **基础地图能力** → **地图手势** → **手势阈值** |
| 路由 | `main` → `map` → `map_gestures` → `map_gesture_threshold` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/gesture/MapGestureThresholdScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapGestureThresholdScreenTest.kt` |
| Demo 目的 | 验证平移/旋转/倾斜手势的像素阈值与阻力系数 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `gesturesManager.movePixelThreshold` | 属性 | 平移触发阈值 | ✅ TC-001 |
| `gesturesManager.rotatePixelThreshold` | 属性 | 旋转触发阈值 | ✅ TC-002 |
| `gesturesManager.rotationResistance` | 属性 | 旋转阻力 | ✅ TC-002 |
| `gesturesManager.shovePixelThreshold` | 属性 | 倾斜触发阈值 | ✅ TC-003 |
| `gesturesManager.shoveResistance` | 属性 | 倾斜阻力 | ✅ TC-003 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 平移阈值 | 拖动 `slider_move_threshold` | `movePixelThreshold` 更新 | `movePixelThreshold` | UI+API | `testMoveGesture_ThresholdSliderUpdatesApi` | P0 |
| TC-002 | 旋转阈值与阻力 | 拖动旋转相关滑条 | `rotatePixelThreshold`、`rotationResistance` 更新 | 旋转相关属性 | UI+API | `testRotateGesture_SlidersUpdateApi` | P0 |
| TC-003 | 倾斜阈值与阻力 | 拖动倾斜相关滑条 | `shovePixelThreshold`、`shoveResistance` 更新 | 倾斜相关属性 | UI+API | `testShoveGesture_SlidersUpdateApi` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapGestureThresholdScreenTest
```
