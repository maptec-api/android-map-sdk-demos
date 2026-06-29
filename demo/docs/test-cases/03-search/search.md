# 搜索服务

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 搜索服务 |
| 导航路径 | 主页 → **搜索服务** |
| 路由 | `main` → `unifiedSearch` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/SearchScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/SearchViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/SearchScreenTest.kt` |
| Demo 目的 | 验证文本搜索、附近搜索、建议搜索、地点详情及地图模式 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `SearchService.getInstance` | 单例 | 搜索服务入口 | ✅ 全部 API 用例 |
| `SearchService.textSearch` | 方法 | 文本搜索 | ✅ TC-001 ~ TC-003 |
| `SearchService.nearbySearch` | 方法 | 附近搜索 | ✅ TC-004、TC-005 |
| `SearchService.suggest` | 方法 | 建议/自动补全 | ✅ TC-006 ~ TC-008、TC-010 |
| `SearchService.getPlaceDetail` | 方法 | 地点详情 | ✅ TC-008、TC-009 |
| `MapView` / `MaptecMap` | 地图 | 地图模式展示结果 | ✅ TC-013 |
| `SymbolManager` / `CircleManager` / `FillWithOutlineManager` | 标注 | 结果标注 | ✅ TC-013 |
| `CameraUpdateFactory` | 工厂 | 相机移动 | — |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 文本搜索 | 输入关键词搜索 | JSON 响应卡片展示结果 | `textSearch` | UI+API | `textSearch_query_showsApiResponse` | P0 |
| TC-002 | 文本搜索高级参数 | 展开高级参数后搜索 | 响应包含高级参数效果 | `textSearch` | UI+API | `textSearch_withAdvancedParams_showsApiResponse` | P1 |
| TC-003 | 文本搜索位置偏置 | 配置偏置后搜索 | 响应正常 | `textSearch` | UI+API | `textSearch_withLocationBias_showsApiResponse` | P1 |
| TC-004 | 附近搜索 | 填写中心与半径搜索 | 响应展示附近 POI | `nearbySearch` | UI+API | `nearbySearch_showsApiResponse` | P0 |
| TC-005 | 附近搜索类型 | 配置 types 后搜索 | 响应正常 | `nearbySearch` | UI+API | `nearbySearch_withTypes_showsApiResponse` | P1 |
| TC-006 | 建议搜索 | 执行建议搜索 | 响应展示建议列表 | `suggest` | UI+API | `suggestSearch_showsApiResponse` | P0 |
| TC-007 | 建议搜索类型 | 配置 types 后搜索 | 响应正常 | `suggest` | UI+API | `suggestSearch_withTypes_showsApiResponse` | P1 |
| TC-008 | 地点详情（Tab） | 在详情 Tab 请求 | 响应展示详情 | `getPlaceDetail` | UI+API | `placeDetail_fromTab_showsApiResponse` | P0 |
| TC-009 | 地点详情（结果） | 从搜索结果点进详情 | 响应展示详情 | `getPlaceDetail` | UI+API | `placeDetail_fromSearchResult_showsApiResponse` | P0 |
| TC-010 | 输入自动建议 | 输入时触发 | 建议列表出现 | `suggest` | UI+API | `autoSuggest_onTyping_showsSuggestions` | P1 |
| TC-011 | 点击建议填充 | 点击建议项 | 查询框被填充 | `suggest` | UI | `textSearch_autoSuggest_fillsQueryOnClick` | P1 |
| TC-012 | Tab 切换 | 切换各 Tab | 无崩溃 | — | UI | `tabSwitching_noCrash` | P1 |
| TC-013 | 地图模式切换 | 点击 FAB 切换地图 | 列表/地图视图切换正常 | `MapView` | UI | `mapView_toggle_works` | P0 |
| TC-014 | 返回主页 | 按返回 | 回到主页列表 | — | UI | `backNavigation_returnsToMainScreen` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.SearchScreenTest
```
