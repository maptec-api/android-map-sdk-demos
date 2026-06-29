# MarkerView (信息窗口) 测试

## 对应页面
业务图层 → 信息窗口

## 测试文件
`demo/src/androidTest/java/com/maptec/applied/demo/MarkerViewScreenTest.kt`

## 测试用例

### 1. 添加气泡（基本）
| 用例 | 操作 | 验证 |
|------|------|------|
| `testAddBubble_showsIndicator` | 点击「添加」按钮 | `markerview_has_bubble` 指示器出现 |
| `testAddBubble_verifyBubbleViewExists` | 点击「添加」按钮 | BubbleCalloutView 存在于视图层级，line1="24 min", line2="7.9 km" |

### 2. 添加富文本气泡
| 用例 | 操作 | 验证 |
|------|------|------|
| `testAddRichTextBubble_showsIndicator` | 点击「添加富文本」按钮 | `markerview_has_bubble` 指示器出现 |
| `testAddRichTextBubble_verifyBubbleViewExists` | 点击「添加富文本」按钮 | BubbleCalloutView 存在于视图层级 |
| `testAddRichText_withCustomHtml_works` | 输入自定义 HTML，点击添加富文本 | BubbleCalloutView 存在 |

### 3. 移除气泡
| 用例 | 操作 | 验证 |
|------|------|------|
| `testRemoveBubble_removesIndicator` | 添加 → 移除 | `markerview_has_bubble` 消失 |
| `testRemoveBubble_removesBubbleView` | 添加 → 移除 | BubbleCalloutView 从视图层级移除 |

### 4. 添加/移除/再添加
| 用例 | 操作 | 验证 |
|------|------|------|
| `testAddThenRemoveThenReAdd_works` | 添加 → 移除 → 再添加 | 最终指示器存在 |
| `testMultipleAddRemove_cycles_noCrash` | 3 次添加/移除循环 | 最终指示器不存在，无崩溃 |

### 5. 更新气泡属性
| 用例 | 操作 | 验证 |
|------|------|------|
| `testUpdateBubbleLine1_changesBubbleText` | 添加 → 修改 line1 | BubbleCalloutView.line1Text 更新 |
| `testUpdateBubbleLine2_changesBubbleText` | 添加 → 修改 line2 | BubbleCalloutView.line2Text 更新 |
| `testUpdateBubbleBgColor_updatesBubble` | 添加 → 修改背景色 | BubbleCalloutView.bubbleBackgroundColor 更新 |
| `testUpdateBubbleBorderColor_updatesBubble` | 添加 → 修改边框色 | BubbleCalloutView.borderColor 更新 |

### 6. 按钮状态
| 用例 | 操作 | 验证 |
|------|------|------|
| `testRemoveButton_disabled_whenNoBubble` | 无气泡时 | 移除按钮 disabled |
| `testAddButton_enabled_whenMapReady` | 地图就绪 | 添加按钮 enabled |

### 7. 输入控件状态
| 用例 | 操作 | 验证 |
|------|------|------|
| `testInputFields_disabled_whenNoBubble` | 无气泡时 | 所有属性输入/滑块 disabled |
| `testInputFields_enabled_whenBubbleExists` | 添加气泡后 | line1/line2/bgColor/borderColor 输入 enabled |

### 8. 自动关闭切换
| 用例 | 操作 | 验证 |
|------|------|------|
| `testToggleAutoClose_changesButtonText` | 切换自动关闭按钮 | 按钮文本在开/关之间切换 |

### 9. API 验证
| 用例 | 操作 | 验证 |
|------|------|------|
| `testAddBubble_apiVerification_viaOverlayEngine` | 添加气泡后 | OverlayEngine 可用且非空 |

## Test Tags

| Tag | 元素 |
|-----|------|
| `markerview_screen` | BottomSheetScaffold |
| `markerview_has_bubble` | 气泡存在指示器 Box |
| `markerview_btn_add` | 添加按钮 |
| `markerview_btn_remove` | 移除按钮 |
| `markerview_btn_auto_close` | 自动关闭切换按钮 |
| `markerview_btn_add_rich` | 添加富文本按钮 |
| `markerview_input_line1` | 第一行文案输入框 |
| `markerview_input_line2` | 第二行文案输入框 |
| `markerview_input_bg_color` | 背景色输入框 |
| `markerview_input_border_color` | 边框色输入框 |
| `markerview_input_rich_text` | 富文本 HTML 输入框 |
| `markerview_slider_corner_radius` | 圆角半径滑块 |
| `markerview_slider_max_width` | 最大宽度滑块 |
| `markerview_slider_border_width` | 边框宽度滑块 |
| `markerview_bubble` | BubbleCalloutView 的 View tag |

## 涉及的核心 API

- `MaptecMap.getOverlayEngine().addInfoWindow(InfoWindowOptions)` — 添加信息窗口
- `InfoWindow.remove()` — 移除信息窗口
- `InfoWindow.setAutoCloseOnMapClick(Boolean)` — 设置点击地图自动关闭
- `InfoWindow.updatePosition()` — 更新信息窗口位置
- `InfoWindow.addOnCloseListener()` — 关闭监听
- `InfoWindowOptions.withLatLng()` / `withContentView()` / `withAnchor()` / `withAutoCloseOnMapClick()`
- `BubbleCalloutView` — 自定义气泡信息窗口内容 View
