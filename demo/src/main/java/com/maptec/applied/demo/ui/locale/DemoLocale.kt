package com.maptec.applied.demo.ui.locale

import android.content.Context
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import java.util.Locale

/** BCP 47 UI language tags supported by demo string resources. */
const val DEMO_UI_LANG_ZH = "zh"
const val DEMO_UI_LANG_EN = "en"

val LocalDemoUiLanguageTag = staticCompositionLocalOf { DEMO_UI_LANG_ZH }

private const val DEMO_UI_LANGUAGE_PREFS = "demo_ui_language_prefs"
private const val DEMO_UI_LANGUAGE_KEY = "ui_language_tag"

fun Context.readDemoUiLanguage(): String {
    return normalizeUiLanguageTag(
        getSharedPreferences(DEMO_UI_LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .getString(DEMO_UI_LANGUAGE_KEY, DEMO_UI_LANG_ZH)
    )
}

fun Context.saveDemoUiLanguage(languageTag: String) {
    getSharedPreferences(DEMO_UI_LANGUAGE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEMO_UI_LANGUAGE_KEY, normalizeUiLanguageTag(languageTag))
        .apply()
}

fun toggleDemoUiLanguage(current: String): String {
    return if (current == DEMO_UI_LANG_EN) DEMO_UI_LANG_ZH else DEMO_UI_LANG_EN
}

@Composable
fun rememberDemoUiLanguage(): MutableState<String> {
    val context = LocalContext.current.applicationContext
    val state = remember { mutableStateOf(context.readDemoUiLanguage()) }
    LaunchedEffect(state.value) {
        context.saveDemoUiLanguage(state.value)
    }
    return state
}

fun normalizeUiLanguageTag(languageTag: String?): String {
    return if (languageTag == DEMO_UI_LANG_EN) DEMO_UI_LANG_EN else DEMO_UI_LANG_ZH
}

fun Context.withUiLocale(languageTag: String): Context {
    val locale = when (normalizeUiLanguageTag(languageTag)) {
        DEMO_UI_LANG_EN -> Locale.ENGLISH
        else -> Locale.SIMPLIFIED_CHINESE
    }
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/**
 * Wraps [content] with a [LocalContext] whose locale matches [languageTag],
 * so [androidx.compose.ui.res.stringResource] loads the correct values/values-en files.
 */
@Composable
fun DemoLanguageProvider(
    languageTag: String,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val normalizedTag = normalizeUiLanguageTag(languageTag)
    val localizedContext = androidx.compose.runtime.remember(baseContext, normalizedTag) {
        baseContext.withUiLocale(normalizedTag)
    }
    // Replacing LocalContext with createConfigurationContext() breaks composition locals
    // that resolve owners via LocalContext (e.g. rememberLauncherForActivityResult).
    val activityResultOwner = LocalActivityResultRegistryOwner.current
        ?: (baseContext as? ActivityResultRegistryOwner)
        ?: error("DemoLanguageProvider requires an Activity context")
    val backPressedOwner = LocalOnBackPressedDispatcherOwner.current
        ?: (baseContext as? OnBackPressedDispatcherOwner)
        ?: error("DemoLanguageProvider requires an Activity context")
    val savedStateOwner = LocalSavedStateRegistryOwner.current
        ?: (baseContext as? SavedStateRegistryOwner)
        ?: error("DemoLanguageProvider requires an Activity context")
    val lifecycleOwner = LocalLifecycleOwner.current

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalDemoUiLanguageTag provides normalizedTag,
        LocalLifecycleOwner provides lifecycleOwner,
        LocalActivityResultRegistryOwner provides activityResultOwner,
        LocalOnBackPressedDispatcherOwner provides backPressedOwner,
        LocalSavedStateRegistryOwner provides savedStateOwner,
    ) {
        content()
    }
}
