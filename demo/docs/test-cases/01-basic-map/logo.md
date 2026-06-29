# logo控件

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 基础地图能力 |
| 二级入口 | logo控件 |
| 导航路径 | 主页 → **基础地图能力** → **logo控件** |
| 路由 | `main` → `map` → `logo` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/map/LogoScreen.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/map/LogoScreenTest.kt` |
| Demo 目的 | 验证地图 Logo 的 9 种重力位置与边距 API |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `UiSettings.logoGravity` | 属性 | Logo 对齐位置 | ✅ TC-001 |
| `UiSettings.logoMarginLeft/Top/Right/Bottom` | 属性 | Logo 边距 | — |
| `UiSettings.setLogoGravity` | 方法 | 设置位置 | ✅ TC-001 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 全部重力位置 | 依次选择 `dropdown_logo_gravity` 9 个选项 | 每次 `logoGravity` 与选项一致 | `logoGravity`、`setLogoGravity` | UI+API | `selectAllGravityOptions` | P0 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.map.LogoScreenTest
```
