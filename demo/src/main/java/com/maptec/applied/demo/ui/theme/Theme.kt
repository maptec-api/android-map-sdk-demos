package com.maptec.applied.demo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MapBlue80,
    secondary = MapTeal80,
    tertiary = MapAmber80,
    background = MapBackgroundDark,
    surface = MapSurfaceDark,
    surfaceVariant = MapSurfaceVariantDark,
    outline = MapOutlineDark,
    outlineVariant = MapOutlineDark,
    onSurface = MapOnSurfaceDark,
    onSurfaceVariant = MapOnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = MapBlue40,
    secondary = MapTeal40,
    tertiary = MapAmber40,
    background = MapBackgroundLight,
    surface = MapSurfaceLight,
    surfaceVariant = MapSurfaceVariantLight,
    outline = MapOutlineLight,
    outlineVariant = MapOutlineLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = MapOnSurfaceLight,
    onSurface = MapOnSurfaceLight,
    onSurfaceVariant = MapOnSurfaceVariantLight,
)

private val DemoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun MapEngineAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DemoShapes,
        content = content,
    )
}
