package com.maptec.applied.demo.ui.screens.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maptec.applied.demo.R
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun DemoPanelColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            content = content
        )
    }
}

/**
 * Demo page layout: main content plus a config panel that slides in from the right.
 * Not related to Material3 [androidx.compose.material3.BottomSheetScaffold].
 */
@Composable
fun DemoPanelScaffold(
    modifier: Modifier = Modifier,
    sheetWidthFraction: Float = 0.50f,
    sheetPanelMaxWidth: Dp = Dp.Unspecified,
    sheetPanelHeight: Dp = Dp.Unspecified,
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val panelOwner = remember { Any() }
    val configPanelController = LocalDemoConfigPanelController.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp
    val effectiveWidthFraction = if (isLandscape) min(sheetWidthFraction, 0.38f) else sheetWidthFraction
    val landscapePanelMaxWidth = 400.dp
    val currentDensity = LocalDensity.current
    val compactPanelDensity = Density(
        density = currentDensity.density * 0.64f,
        fontScale = currentDensity.fontScale,
    )
    val panelColorScheme = MaterialTheme.colorScheme.copy(
        primary = Color(0xFF5B5CE2),
        secondary = Color(0xFF6C63E8),
    )
    val requestedPanelWidth = screenWidth.value * effectiveWidthFraction
    val panelWidth = when {
        sheetPanelMaxWidth != Dp.Unspecified -> min(requestedPanelWidth, sheetPanelMaxWidth.value).dp
        isLandscape -> min(requestedPanelWidth, landscapePanelMaxWidth.value).dp
        else -> requestedPanelWidth.dp
    }
    val panelOffsetX by animateDpAsState(
        targetValue = if (expanded) 0.dp else panelWidth,
        animationSpec = tween(durationMillis = 280),
        label = "demoPanelOffset",
    )

    SideEffect {
        configPanelController.register(
            owner = panelOwner,
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onClose = { expanded = false },
        )
    }

    DisposableEffect(configPanelController, panelOwner) {
        onDispose {
            configPanelController.unregister(panelOwner)
        }
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        content()

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = panelOffsetX)
                .width(panelWidth)
                .then(
                    if (sheetPanelHeight == Dp.Unspecified) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier.height(sheetPanelHeight)
                    },
                ),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.demo_config_panel_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { expanded = false },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("demo_config_panel_close"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.demo_config_panel_collapse),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CompositionLocalProvider(LocalDensity provides compactPanelDensity) {
                    MaterialTheme(colorScheme = panelColorScheme) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            content = sheetContent,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoSheetHandle(
    scaffoldState: BottomSheetScaffoldState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val expanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (expanded) MaterialTheme.colorScheme.primary else Color(0xFFDDE5F2),
                modifier = Modifier.width(32.dp)
            ) {
                Spacer(modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.demo_config_panel_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (expanded) {
                            Unit
                        } else {
                            Unit
                        }
                    }
                },
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(
                    text = stringResource(
                        if (expanded) {
                            R.string.demo_config_panel_collapse
                        } else {
                            R.string.demo_config_panel_expand
                        }
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
