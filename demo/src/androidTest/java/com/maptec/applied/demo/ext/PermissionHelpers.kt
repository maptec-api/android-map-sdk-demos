package com.maptec.applied.demo.ext

import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * 运行时权限授予（避免 [androidx.test.rule.GrantPermissionRule]）。
 *
 * GrantPermissionRule 每次评测都会 [Instrumentation.getUiAutomation] 抢连，
 * 在 Compose / 截屏销毁 UiAutomation 后易触发
 * `UiAutomationService already registered`，拖垮整次 instrumentation。
 *
 * 本助手：已授权则跳过；否则优先 shell `pm grant`，失败再走
 * [android.app.UiAutomation.grantRuntimePermission]，并在 already registered
 * 时 destroy 后重试一次。
 *
 * 需要危险权限的用例请用 [grantPermissionsRule] 作为 [org.junit.rules.RuleChain]
 * 的 outer rule，确保在 Compose Activity 启动前授予。
 */
fun grantPermissionsRule(vararg permissions: String): TestRule =
    TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                grantRuntimePermissionsIfNeeded(*permissions)
                base.evaluate()
            }
        }
    }

fun grantRuntimePermissionsIfNeeded(vararg permissions: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val pkg = context.packageName
    val pending = permissions.filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }
    if (pending.isEmpty()) return

    for (permission in pending) {
        if (tryShellGrant(instrumentation, pkg, permission)) continue
        grantViaUiAutomation(instrumentation, pkg, permission)
    }
}

private fun tryShellGrant(
    instrumentation: Instrumentation,
    pkg: String,
    permission: String,
): Boolean {
    return runCatching {
        val automation = instrumentation.uiAutomation
        val pfd = automation.executeShellCommand("pm grant $pkg $permission")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { stream ->
            // 读尽输出，避免管道阻塞
            stream.readBytes()
        }
        instrumentation.targetContext.checkSelfPermission(permission) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
}

private fun grantViaUiAutomation(
    instrumentation: Instrumentation,
    pkg: String,
    permission: String,
) {
    fun grant() {
        instrumentation.uiAutomation.grantRuntimePermission(pkg, permission)
    }
    try {
        grant()
    } catch (e: IllegalStateException) {
        destroyUiAutomationQuietly(instrumentation)
        Thread.sleep(300)
        grant()
    }
}

/** Instrumentation.destroyUiAutomation 为 @hide。 */
private fun destroyUiAutomationQuietly(instrumentation: Instrumentation) {
    runCatching {
        Instrumentation::class.java.getMethod("destroyUiAutomation").invoke(instrumentation)
    }
}
