package com.maptec.applied.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.maptec.applied.demo.ui.PermissionHandler
import com.maptec.applied.demo.ui.components.DemoAppBar
import com.maptec.applied.demo.ui.locale.DemoLanguageProvider
import com.maptec.applied.demo.ui.locale.rememberDemoUiLanguage
import com.maptec.applied.demo.ui.locale.toggleDemoUiLanguage
import com.maptec.applied.demo.ui.screens.auth.AttentionScreen
import com.maptec.applied.demo.ui.screens.catalog.CatalogRoutes
import com.maptec.applied.demo.ui.screens.catalog.catalogRoutes
import com.maptec.applied.demo.ui.screens.catalog.catalogTopBarTitle
import com.maptec.applied.demo.ui.screens.catalog.MainCatalogScreen
import com.maptec.applied.demo.ui.screens.common.DemoConfigPanelProvider
import com.maptec.applied.demo.ui.screens.common.LocalDemoConfigPanelController
import com.maptec.applied.demo.ui.theme.MapEngineAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            var uiLanguageTag by rememberDemoUiLanguage()

            PermissionHandler { hasPermission ->
                DemoLanguageProvider(uiLanguageTag) {
                    DemoConfigPanelProvider {
                        MapEngineAndroidTheme {
                            if (hasPermission) {
                                val navController = rememberNavController()
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentRoute = navBackStackEntry?.destination?.route ?: "main"
                                var nestedBackHandler by remember { mutableStateOf<(() -> Boolean)?>(null) }
                                val configPanelController = LocalDemoConfigPanelController.current
                                val appTitle = stringResource(id = R.string.app_title)

                                LaunchedEffect(currentRoute) {
                                    nestedBackHandler = null
                                }

                                val navigateUpAction: () -> Unit = {
                                    if (configPanelController.expanded) {
                                        configPanelController.close()
                                    }
                                    if (navController.currentDestination?.route == "main") {
                                        finish()
                                    } else if (nestedBackHandler?.invoke() == true) {
                                        Unit
                                    } else {
                                        navController.navigateUp()
                                    }
                                }

                                BackHandler(enabled = configPanelController.expanded) {
                                    navigateUpAction()
                                }

                                Scaffold(
                                    topBar = {
                                        DemoAppBar(
                                            currentTitle = catalogTopBarTitle(
                                                route = currentRoute,
                                                appTitle = appTitle,
                                            ),
                                            currentLanguage = uiLanguageTag,
                                            canNavigateBack = currentRoute != "main",
                                            navigateUp = navigateUpAction,
                                            configPanelAvailable = configPanelController.available,
                                            configPanelExpanded = configPanelController.expanded,
                                            onConfigPanelToggle = { configPanelController.toggle() },
                                            onLanguageToggle = {
                                                uiLanguageTag = toggleDemoUiLanguage(uiLanguageTag)
                                            },
                                        )
                                    }
                                ) { innerPadding ->
                                    NavHost(
                                        navController = navController,
                                        startDestination = "main",
                                        modifier = Modifier.padding(innerPadding),
                                        enterTransition = { EnterTransition.None },
                                        exitTransition = { ExitTransition.None },
                                        popEnterTransition = {
                                            slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 4 })
                                        },
                                        popExitTransition = {
                                            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                                        },
                                    ) {
                                        composable("main") {
                                            MainCatalogScreen(
                                                onItemClicked = { item ->
                                                    navController.navigate(item.route)
                                                },
                                            )
                                        }
                                        composable(CatalogRoutes.AUTH) {
                                            AttentionScreen(navController = navController)
                                        }
                                        catalogRoutes(
                                            navController = navController,
                                            registerNestedBackHandler = { nestedBackHandler = it },
                                        )
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
    }
}
