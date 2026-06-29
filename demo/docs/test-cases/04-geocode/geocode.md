# 地理编码

## 功能概述

| 属性 | 值 |
|------|-----|
| 一级入口 | 地理编码 |
| 导航路径 | 主页 → **地理编码** |
| 路由 | `main` → `geocode` |
| 源码 | `src/main/java/com/maptec/applied/demo/ui/screens/GeocodeScreen.kt`、`src/main/java/com/maptec/applied/demo/viewmodel/GeocodeViewModel.kt` |
| 自动化测试 | `src/androidTest/java/com/maptec/applied/demo/GeocodeScreenTest.kt` |
| Demo 目的 | 验证正向地理编码与逆向地理编码服务调用 |

## 覆盖 API 清单

| SDK API | 类型 | Demo 调用说明 | 是否在用例中验证 |
|---------|------|--------------|----------------|
| `GeocodeService.getInstance` | 单例 | 地理编码服务 | ✅ 全部 |
| `GeocodeService.geocode` | 方法 | 地址 → 坐标 | ✅ TC-001 ~ TC-003 |
| `GeocodeService.reverseGeocode` | 方法 | 坐标 → 地址 | ✅ TC-004、TC-005 |
| `LocationBias.Rectangle` | 类型 | 正向偏置区域 | ✅ TC-003 |

---

## 测试用例

| 编号 | 用例名称 | 操作步骤 | 预期结果 | 覆盖 API | 验证方式 | 自动化 | 优先级 |
|------|---------|---------|---------|---------|---------|--------|--------|
| TC-001 | 正向基础 | 输入地址提交 | JSON 响应展示坐标 | `geocode` | UI+API | `forwardGeocode_basic_showsApiResponse` | P0 |
| TC-002 | 正向地址与组件 | 填写 address/components | 响应正常 | `geocode` | UI+API | `forwardGeocode_withAddressAndComponents_showsApiResponse` | P1 |
| TC-003 | 正向偏置矩形 | 配置 bias rectangle | 响应正常 | `geocode` | UI+API | `forwardGeocode_withBiasRectangle_showsApiResponse` | P1 |
| TC-004 | 逆向基础 | 提交默认坐标 | JSON 响应展示地址 | `reverseGeocode` | UI+API | `reverseGeocode_basic_showsApiResponse` | P0 |
| TC-005 | 逆向全参数 | 填写 language/region 等 | 响应正常 | `reverseGeocode` | UI+API | `reverseGeocode_withAllParams_showsApiResponse` | P1 |
| TC-006 | Tab 切换 | 正向/逆向 Tab 切换 | 无崩溃 | — | UI | `tabSwitching_noCrash` | P1 |
| TC-007 | 返回主页 | 按返回 | 回到主页列表 | — | UI | `backNavigation_returnsToMainScreen` | P1 |

---

## 自动化测试执行

```bash
./gradlew :demo:connectedOpenglDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.maptec.applied.demo.GeocodeScreenTest
```
