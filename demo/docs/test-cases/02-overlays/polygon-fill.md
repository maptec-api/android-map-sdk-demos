# 多边形完整示例

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | 多边形 → 多边形完整示例 |
| 导航路径 | 主页 → **业务图层** → **多边形** |
| 路由 | `main` → `overlays` → `polygon_full` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/polygon/FillScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/FillScreenTest.kt`（6 个 `@Test`） |
| Demo 目的 | 验证 Fill 多边形绘制、自定义参数与输入校验 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOverlayEngine.addPolygon` | 方法 | 多边形绘制 | ✅ TC-001 ~ TC-003 | — |
| `Fill` / `FillOptions` | 对象/配置 | 颜色/坐标 | ✅ TC-002、TC-003 | — |
| `CameraUpdateFactory` | 工厂 | 绘制后移动相机 | ✅ TC-001 ~ TC-003 | — |
| 输入校验 | UI 逻辑 | 非法输入禁用绘制 | ✅ TC-005 | — |
| `OnOverlayClick/Drag/LongClickListener` | 监听 | 多边形交互 | — | 未覆盖 |
| `FillOptions` pattern / blendMode | 配置 | 图案与混合 | — | 未覆盖 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 默认参数绘制 | 点击绘制 | 相机移动 | `addPolygon` | UI+API | `drawWithDefaultValues_movesCamera` | P0 |
| TC-002 | 自定义颜色 | 修改颜色后绘制 | 相机移动 | `FillOptions` | UI+API | `drawWithCustomColors_movesCamera` | P0 |
| TC-003 | 自定义坐标 | 输入多边形坐标后绘制 | 相机移动 | `addPolygon` | UI+API | `drawWithSimpleCoords_movesCamera` | P0 |
| TC-004 | 返回导航 | 按返回 | 回到列表/主页 | — | UI | `back_returnsToOverlayList`、`doubleBack_returnsToMainScreen` | P1 |
| TC-005 | 非法输入 | 输入非法颜色 | 绘制按钮禁用 | 校验逻辑 | UI | `drawWithInvalidInput_drawButtonDisabled` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.FillScreenTest
```
