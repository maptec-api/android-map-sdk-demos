package com.maptec.applied.demo.ui.screens.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/**
 * 校验 "lat,lng" 格式的经纬度字符串。
 * @param input 用户输入，如 "1.45, 103.80"
 * @param required 是否必填；为 false 时空字符串视为合法
 * @return 错误提示，null 表示合法
 */
fun validateLatLng(input: String, required: Boolean = true): String? {
    val t = input.trim()
    if (t.isBlank()) return if (required) "请输入 lat,lng" else null
    val parts = t.split(",").map { it.trim().toDoubleOrNull() }
    if (parts.size < 2 || parts[0] == null || parts[1] == null) return "格式错误，示例：1.45,103.80"
    val lat = parts[0]!!
    val lng = parts[1]!!
    return when {
        lat !in -90.0..90.0 -> "纬度应在 -90～90"
        lng !in -180.0..180.0 -> "经度应在 -180～180"
        else -> null
    }
}

/**
 * 带经纬度校验的 [OutlinedTextField]，支持错误/占位提示与 testTag。
 * @param value 当前输入
 * @param onValueChange 输入回调
 * @param label 标签文案
 * @param modifier 修饰符
 * @param singleLine 是否单行
 * @param hint 无错误时底部提示文案（灰色）
 * @param required 是否必填，用于空串是否算错误
 * @param testTag 测试用 tag
 */
@Composable
fun LatLngOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    hint: String = "1.45,103.80",
    required: Boolean = true,
    testTag: String? = null
) {
    val error = remember(value) { validateLatLng(value, required) }
    val modifierWithTag = testTag?.let { modifier.testTag(it) } ?: modifier
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            ?: { Text(hint, color = Color.Gray) },
        isError = error != null,
        modifier = modifierWithTag,
        singleLine = singleLine
    )
}
