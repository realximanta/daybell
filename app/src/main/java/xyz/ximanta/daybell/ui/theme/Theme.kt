package xyz.ximanta.daybell.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = NeonTeal,
    onPrimary = Graphite,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealOnContDark,
    secondary = SoftViolet,
    onSecondary = Graphite,
    secondaryContainer = VioletContDark,
    onSecondaryContainer = VioletOnContDark,
    background = Graphite,
    onBackground = CloudText,
    surface = Graphite,
    onSurface = CloudText,
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = MutedTextDark,
    outline = GraphiteOutline,
    outlineVariant = OutlineVariantDark,
)

private val LightColors = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = TealOnContLight,
    secondary = VioletLight,
    onSecondary = Color.White,
    secondaryContainer = VioletContLight,
    onSecondaryContainer = VioletOnContLight,
    background = SurfaceLight,
    onBackground = Graphite,
    surface = SurfaceRaised,
    onSurface = Graphite,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = GraphiteMuted,
    outline = OutlineLight,
)

@Composable
fun DaybellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep brand identity — dynamic color is off by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DaybellTypography,
        content = content
    )
}
