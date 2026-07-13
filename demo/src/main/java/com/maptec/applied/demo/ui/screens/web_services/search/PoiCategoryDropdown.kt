package com.maptec.applied.demo.ui.screens.web_services.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.maptec.applied.demo.R
import com.maptec.applied.demo.data.PoiCategoryData

/**
 * POI分类下拉框组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiCategoryDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { PoiCategoryData.getAllSecondaryCategories() }
    var expanded by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.value == selectedValue }
    val displayText = selectedCategory?.displayName ?: stringResource(R.string.search_poi_type_optional)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("search_nearby_types_dropdown"),
            label = { Text(stringResource(R.string.search_poi_type_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            supportingText = { null } // 确保与其他输入框高度一致
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 添加"全部"选项（清空选择）
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_poi_all)) },
                onClick = {
                    onValueChange("")
                    expanded = false
                }
            )

            Divider()

            categories.forEachIndexed {index, category ->
                DropdownMenuItem(
                    text = { Text(category.displayName) },
                    onClick = {
                        onValueChange(category.value)
                        expanded = false
                    },
                    modifier= Modifier.testTag("drop_index_click_${index}")
                )
            }
        }
    }
}