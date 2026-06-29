package com.maptec.applied.demo.ui.screens.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * 带内层返回注册的 NavController。与 MainActivity 的 [nestedBackHandler] 配合使用：
 * 返回时先尝试 pop 内层栈，再由主 NavController.navigateUp()。
 *
 * @param registerBackHandler 向 MainActivity 注册的 (() -> Boolean)?，返回 true 表示已消费
 * @return 内层 NavController，可直接用于 NavHost
 */
@Composable
fun rememberNestedNavController(
    registerBackHandler: ((() -> Boolean)?) -> Unit
): NavController {
    val navController = rememberNavController()
    DisposableEffect(navController) {
        registerBackHandler {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
                true
            } else {
                false
            }
        }
        onDispose {
            registerBackHandler(null)
        }
    }
    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.popBackStack()
    }
    return navController
}
