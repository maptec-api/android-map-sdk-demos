# 样式切换

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | 样式切换 |
| 导航路径 | 主页 → **基础地图能力** → **样式切换** |
| 路由 | `main` → `map` → `map_style` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/MapStyleScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/MapStyleScreenTest.kt` |
| Demo 目的 | 验证样式预加载与运行时切换 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `MaptecMap.preLoadStyles` | 方法 | 预加载样式列表 | ✅ TC-003 |
| `MaptecMap.setStyle(StyleOption, StyleStatusCallback)` | 方法 | 切换样式 | ✅ TC-004 |
| `StyleOption.url` / `StyleOption.id` | 配置 | 样式参数 | ✅ TC-002、TC-004 |
| `StyleStatusCallback.onStyleRendered` | 回调 | 切换后重新渲染 | ✅ TC-004 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | UI 元素展示 | 进入页面 | 样式 ID 输入、预加载、切换按钮可见 | — | UI | `testUIElementsDisplayed` | P1 |
| TC-002 | 样式 ID 可编辑 | 修改 `style_id_input` | 文本可输入 | `StyleOption.id` | UI | `testStyleIdInputModifiable` | P1 |
| TC-003 | 预加载按钮 | 点击 `preload_button` | 无崩溃，预加载流程触发 | `preLoadStyles` | UI | `testPreloadButtonClickable` | P0 |
| TC-004 | 切换样式 | 点击 `switch_button` | `onStyleRendered` 再次触发，地图重绘 | `setStyle` | UI | `testSwitchStyleTriggersRendered` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.MapStyleScreenTest
```
