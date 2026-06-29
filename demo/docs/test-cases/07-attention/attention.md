# 鉴权配置

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 鉴权配置 |
| 导航路径 | 主页 → **鉴权配置** |
| 路由 | `main` → `attention` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/AttentionScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/AttentionViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/AttentionScreenTest.kt` |
| Demo 目的 | 验证各 SDK 的 API Key / SHA1 配置与授权服务查询 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否验证 | 缺口类型 |
|---------|------|--------------|---------|---------|
| `MapSDK.setApiKey` / `setSignatureSha1` | 地图鉴权 | 地图 Key/SHA1 | ✅ TC-003 | — |
| `SearchService.setApiKey` / `setSignatureSha1` | 搜索鉴权 | 搜索 Key/SHA1 | — | — |
| `RouteService.setApiKey` / `setSignatureSha1` | 路径鉴权 | 路径 Key/SHA1 | — | — |
| `SearchService.getAuthStatus` | 方法 | 查询已授权服务 | ✅ TC-002 | — |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 核心元素展示 | 进入页面 | Key/SHA1、勾选、按钮可见 | — | UI | `allCoreElements_areDisplayed` | P1 |
| TC-002 | 查看已授权服务 | 点击查看 | JSON 卡片展示 | `getAuthStatus` | UI | `viewAuthorizedServices_showsJsonCard` | P0 |
| TC-003 | 应用 Key 后渲染 | 填入 Key/SHA1 并验证地图 | 地图可正常渲染 | `setApiKey`、`setSignatureSha1` | UI+API | `applyMaptecKeyAndSha1_thenNavigateToMap_andVerifyRender` | P0 |

> 已移除仅验证「可输入/可点击/空提交不崩溃」的冗余用例，保留有 API 或 E2E 价值的 3 项。

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.AttentionScreenTest
```
