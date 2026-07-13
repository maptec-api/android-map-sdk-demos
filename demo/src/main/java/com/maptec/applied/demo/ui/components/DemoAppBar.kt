package com.maptec.applied.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maptec.applied.demo.R
import com.maptec.applied.demo.ui.locale.DEMO_UI_LANG_EN
import com.maptec.applied.demo.ui.locale.DEMO_UI_LANG_ZH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoAppBar(
    currentTitle: String,
    currentLanguage: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    onLanguageToggle: () -> Unit,
    configPanelAvailable: Boolean = false,
    configPanelExpanded: Boolean = false,
    onConfigPanelToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = navigateUp,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                }
            }
        },
        actions = {
            if (configPanelAvailable && !configPanelExpanded) {
                TextButton(
                    onClick = onConfigPanelToggle,
                    modifier = Modifier
                        .testTag("demo_config_panel_toggle")
                        .padding(horizontal = 0.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.demo_config_panel_toggle),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            DemoLanguageToggle(
                currentLanguage = currentLanguage,
                onToggle = onLanguageToggle,
            )
        }
    )
}

@Composable
fun DemoLanguageToggle(
    currentLanguage: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_language_24),
                contentDescription = stringResource(
                    if (currentLanguage == DEMO_UI_LANG_EN) {
                        R.string.ui_lang_switch_to_zh
                    } else {
                        R.string.ui_lang_switch_to_en
                    }
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageIndicatorLabel(
                    text = "CN",
                    selected = currentLanguage == DEMO_UI_LANG_ZH,
                )
                Text(
                    text = "/",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                )
                LanguageIndicatorLabel(
                    text = "EN",
                    selected = currentLanguage == DEMO_UI_LANG_EN,
                )
            }
        }
    }
}

@Composable
private fun LanguageIndicatorLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        fontSize = 9.sp,
        lineHeight = 10.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
    )
}
