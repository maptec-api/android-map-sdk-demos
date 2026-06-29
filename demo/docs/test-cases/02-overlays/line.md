# 线

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 业务图层 |
| 二级入口 | 线 |
| 导航路径 | 主页 → **业务图层** → **线** |
| 路由 | `main` → `overlays` → `line` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/overlays/LineScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/LineScreenTest.kt` |
| Demo 目的 | 验证折线绘制、端点样式（Round/Arrow/Custom）、发光与相机联动 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapOverlayEngine.addPolyline` / `deleteAllLines` | 方法 | 折线增删 | ✅ TC-001 ~ TC-004 | — |
| `Line` / `LineOptions` | 对象/配置 | 颜色/宽度/透明度/虚线 | ✅ TC-002、TC-003 | — |
| `LineCapType` | 枚举 | 端点类型 | ✅ TC-006 ~ TC-014 | — |
| `addMarker(MarkerOptions)` | 方法 | Arrow/Custom 端点标记 | ✅ TC-009 ~ TC-011 | — |
| `OnOverlayClick/Drag/LongClickListener` | 监听 | 线交互 | — | 未覆盖 |
| `CameraUpdateFactory` | 工厂 | 绘制后移动相机 | ✅ TC-001 | — |
| `BitmapUtils` | 工具 | 线型图案 | — | 低优先级 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 合法输入相机移动 | 输入合法折线坐标并绘制 | 相机移动 | `addPolyline` | UI+API | `testDraw_validInput_cameraMoves` | P0 |
| TC-002 | 自定义参数绘制 | 修改颜色/宽度后绘制 | 相机移动 | `LineOptions` | UI+API | `testDraw_customValues_cameraMoves` | P0 |
| TC-003 | 发光绘制 | 开启发光并绘制 | 相机移动 | `LineOptions` | UI+API | `testDraw_glowEnabled_cameraMoves` | P1 |
| TC-004 | 多次绘制 | 连续绘制两次 | 相机持续更新 | `addPolyline` | UI+API | `testDraw_multipleDraws_cameraUpdates` | P1 |
| TC-005 | 发光条件 UI | 切换发光开关 | 发光参数区显隐 | `LineOptions` | UI | `testGlowConditionalUI` | P1 |
| TC-006 | 空输入绘制 | 空坐标点击绘制 | 不崩溃 | — | UI | `testDraw_emptyInput_noCrash` | P2 |
| TC-007 | 端点下拉默认 | 进入页面 | 起止端点默认 None | `LineCapType` | UI | `testLineCapDropdowns_displayed_defaultNone` | P1 |
| TC-008 | Square 端点 | 选择 Square | 端点类型更新 | `LineCapType` | UI+API | `testLineCap_selectSquare_endCapUpdates` | P1 |
| TC-009 | Round 绘制 | Round 端点绘制 | 相机移动 | `addPolyline` | UI+API | `testLineCap_roundDraw_cameraMoves` | P0 |
| TC-010 | 双 Arrow | 起止均为 Arrow 并绘制 | 绘制成功 | `addMarker` | UI+API | `testLineCap_arrowBothEnds_drawSuccess` | P0 |
| TC-011 | 双 Custom | 起止均为 Custom 并绘制 | 绘制成功 | `addMarker` | UI+API | `testLineCap_customBothEnds_drawSuccess` | P0 |
| TC-012 | 混合端点 | Arrow + Custom 并绘制 | 绘制成功 | `LineCapType` | UI+API | `testLineCap_mixedStartArrowEndCustom_drawSuccess` | P1 |
| TC-013 | 端点切换重绘 | Arrow 改 Round 后重绘 | 不崩溃 | `LineCapType` | UI | `testLineCap_arrowThenRoundRedraw_noCrash` | P1 |
| TC-014 | Custom 改色 | 修改 Custom 颜色后重绘 | 重绘成功 | `LineOptions` | UI | `testLineCap_customWithColorChange_redrawOk` | P2 |
| TC-015 | 单点 Arrow | 单点坐标 + Arrow 端点 | 不崩溃 | `LineCapType` | UI | `testLineCap_arrowOnSinglePointInput_noCrash` | P2 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.LineScreenTest
```
