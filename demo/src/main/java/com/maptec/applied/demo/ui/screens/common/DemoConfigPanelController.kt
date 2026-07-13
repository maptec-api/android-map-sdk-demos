package com.maptec.applied.demo.ui.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class DemoConfigPanelController {
    var expanded by mutableStateOf(false)
    var onToggle by mutableStateOf<(() -> Unit)?>(null)
    private var onClose: (() -> Unit)? = null
    private var owner: Any? = null

    val available: Boolean
        get() = onToggle != null

    fun toggle() {
        onToggle?.invoke()
    }

    fun close() {
        onClose?.invoke()
    }

    fun register(owner: Any, expanded: Boolean, onToggle: () -> Unit, onClose: () -> Unit) {
        this.owner = owner
        this.expanded = expanded
        this.onToggle = onToggle
        this.onClose = onClose
    }

    fun unregister(owner: Any) {
        if (this.owner === owner) {
            expanded = false
            onToggle = null
            onClose = null
            this.owner = null
        }
    }
}

val LocalDemoConfigPanelController = staticCompositionLocalOf { DemoConfigPanelController() }

@Composable
fun DemoConfigPanelProvider(content: @Composable () -> Unit) {
    val controller = remember { DemoConfigPanelController() }
    CompositionLocalProvider(
        LocalDemoConfigPanelController provides controller,
        content = content,
    )
}
