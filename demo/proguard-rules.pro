#
# ===========================================================================
# 防误杀防御级 ProGuard / R8 规则
#
# 问题背景：
#   release 构建使用 proguard-android-optimize.txt，R8 会对内联类 (Value Class)、
#   协程状态机、Compose 运行时底层进行激进的拆箱 / 内联优化，导致测试 APK 运行时
#   找不到主 APK 中被剥离的类和方法 (NoClassDefFoundError / NoSuchMethodError)。
#
# 解决策略：
#   为测试强依赖的所有底层基础设施类颁发"免死金牌"，防止 R8 将其移除或拆解。
# ===========================================================================

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ===========================================================================
# 1. Kotlin 标准库全家桶 —— 全部保留
# ===========================================================================
# Compose 测试框架会引用 Kotlin stdlib 中任何一个工具类（collections、
# comparisons、text、math、time、ranges、enums、TuplesKt、ExceptionsKt
# 等），R8 optimize 模式会将未在主工程源码中直接引用的 stdlib 类剥离。
# 与其逐个追加，直接兜底整个 kotlin 包，彻底杜绝 NoClassDefFoundError。
-keep class kotlin.** { *; }

# ===========================================================================
# 4. Kotlin 协程全家桶
# ===========================================================================
# R8 会对协程状态机做激进的 rewrite，Continuation 接口被大量内联/重写。
# test APK 运行时若找不到对应协程底层类直接崩溃。
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# ===========================================================================
# 5. Compose 测试框架 (UI Test)
# ===========================================================================
-keep class androidx.compose.ui.test.** { *; }
-keep class androidx.compose.ui.test.junit4.** { *; }

# ===========================================================================
# 6. Compose 运行时 (Runtime)
# ===========================================================================
# MonotonicFrameClock、InfiniteAnimationPolicy 等同步/时钟机制被拆解后，
# ComposeTestRule 的 waitForIdle 等同步方法无法正常工作。
-keep class androidx.compose.runtime.** { *; }

# ===========================================================================
# 7. Compose UI 全家桶 —— 测试框架每层都可能引用
# ===========================================================================
# Compose 测试框架 (FiltersKt.hasText, OutputKt.getUnclippedGlobalBounds,
# AndroidSynchronization 等) 会通过语义节点访问 ui.text, ui.semantics,
# ui.geometry, ui.node, ui.layout, ui.unit, ui.platform 等各个子包。
# 逐个子包添加 keep 规则效率太低，直接兜底整个 androidx.compose.ui。
-keep class androidx.compose.ui.** { *; }

# ===========================================================================
# 8. Compose 动画 (Animation)
# ===========================================================================
# 测试中常用的 advanceTimeBy 等底层动画驱动接口。
-keep class androidx.compose.animation.** { *; }

# ===========================================================================
# 9. Compose 基础 (Foundation)
# ===========================================================================
# 测试中常用的 click、assertTextEquals 等语义操作的底层实现。
-keep class androidx.compose.foundation.** { *; }

# ===========================================================================
# 10. AndroidX 高性能集合库 (Collection)
# ===========================================================================
# TestContext 初始化时使用 mutableIntObjectMapOf 等集合，
# 优化模式下对应 ScatterMap 类被移除导致方法找不到异常。
-keep class androidx.collection.** { *; }

# ===========================================================================
# 11. AndroidX Lifecycle —— Compose 测试框架强依赖
# ===========================================================================
# ComposeRootRegistry$StateChangeHandler.onViewAttachedToWindow 需要
# 反射查找 ViewTreeLifecycleOwner，R8 剥离后直接崩溃。
-keep class androidx.lifecycle.ViewTreeLifecycleOwner { *; }
-keep class androidx.lifecycle.** { *; }

# ===========================================================================
# 12. AndroidX Tracing
# ===========================================================================
# AndroidJUnitRunner.onCreate 等初始回调依赖 Trace API。
-keep class androidx.tracing.Trace { *; }

# ===========================================================================
# 13. Guava 并发库 —— Espresso 强依赖
# ===========================================================================
# Espresso IdlingResource 空闲等待机制的核心接口，
# R8 优化后可能被移除导致测试无法等待异步操作完成。
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

# ===========================================================================
# 14. 全局保命规则 —— Kotlin object 单例字段
# ===========================================================================
# R8 optimize 会移除 Kotlin object 的 INSTANCE 字段（内联所有引用），
# 但 test APK 编译时仍通过该字段引用单例，运行时崩溃。
# 这条规则全局保留所有 Kotlin object 的 INSTANCE 字段。
-keepclassmembers class * {
    public static *** INSTANCE;
}

# ===========================================================================
# 15. Dontwarn —— 屏蔽测试库中的非必需引用警告
# ===========================================================================
-dontwarn androidx.compose.ui.test.**
-dontwarn org.mockito.**
-dontwarn io.mockk.**
