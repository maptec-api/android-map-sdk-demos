package com.maptec.applied.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maptec.applied.demo.ui.screens.AttentionScreen
import com.maptec.applied.demo.ui.screens.GeocodeScreen
import com.maptec.applied.demo.ui.screens.MainScreen
import com.maptec.applied.demo.ui.screens.MapItemScreen
import com.maptec.applied.demo.ui.screens.OverlayItemScreen
import com.maptec.applied.demo.ui.screens.RouteOverlayScreen
import com.maptec.applied.demo.ui.screens.SearchScreen
import com.maptec.applied.demo.ui.screens.common.ScreenItem
import com.maptec.applied.demo.ui.theme.MapEngineAndroidTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MapEngineAndroidTheme {
                PermissionHandler { hasPermission ->
                    if (hasPermission) {
                        val navController = rememberNavController()
                        // 内层返回优先。凡带嵌套 NavHost 的页面（MapItemScreen、OverlayItemScreen）通过
                        // registerBackHandler 注册；Toolbar/系统返回时先执行 nestedBackHandler，再 navigateUp。
                        // 新增同类页面时：入参增加 registerBackHandler，DisposableEffect 注册/onDispose 清空，并加 BackHandler。
                        var nestedBackHandler by remember { mutableStateOf<(() -> Boolean)?>(null) }
                        Scaffold(
                            topBar = {
                                DemoAppBar(
                                    currentTitle = stringResource(id = R.string.app_title),
                                    canNavigateBack = true,
                                    navigateUp = {
                                        if (navController.currentDestination?.route == "main") {
                                            finish()
                                        } else if (nestedBackHandler?.invoke() == true) {
                                            // 内层栈已处理（如 LocationScreen -> MapItemScreen 列表，FillScreen -> OverlayItemScreen 列表）
                                        } else {
                                            navController.navigateUp()
                                        }
                                    })
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "main",
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable("main") {
                                    val items = listOf(
                                        ScreenItem(stringResource(id = R.string.screen_item_map), "map"),
                                        ScreenItem(stringResource(id = R.string.screen_item_overlay), "overlays"),
                                        ScreenItem(stringResource(id = R.string.screen_item_unified_search), "unifiedSearch"),
                                        ScreenItem(stringResource(id = R.string.screen_item_geocode), "geocode"),
                                        ScreenItem(stringResource(id = R.string.screen_item_route), "route_overlay"),
                                        ScreenItem(stringResource(id = R.string.screen_item_attention), "attention"),
                                    )
                                    MainScreen(items, { item ->
                                        navController.navigate(item.route)
                                    })
                                }
                                composable("map") {
                                    MapItemScreen(registerBackHandler = { nestedBackHandler = it })
                                }
                                composable("overlays") {
                                    OverlayItemScreen(registerBackHandler = { nestedBackHandler = it })
                                }
                                composable("unifiedSearch") {
                                    SearchScreen()
                                }
                                composable("geocode") {
                                    GeocodeScreen()
                                }
                                composable("route_overlay") {
                                    RouteOverlayScreen()
                                }
                                composable("attention") {
                                    AttentionScreen(navController = navController)
                                }
                            }
                        }

                } else {
                    Text(
                        text = stringResource(id = R.string.permission_required_message)
                    )
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoAppBar(
    currentTitle: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(currentTitle) },
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
                    Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                }
            }
        }
    )
}

@Composable
fun PermissionHandler(content: @Composable (Boolean) -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }

    // 根据API级别动态获取权限列表
    val permissionList = remember {
        buildList {
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.ACCESS_NETWORK_STATE)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    val context = LocalContext.current

    // 检查是否已有权限
    val allPermissionsGranted = permissionList.all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 更新初始权限状态
    LaunchedEffect(allPermissionsGranted) {
        hasPermission = allPermissionsGranted
    }

    // 创建权限请求器
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasPermission = granted
    }

    // 如果没有权限，则请求权限
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            permissionLauncher.launch(permissionList.toTypedArray())
        }
    }

    content(hasPermission)
}
