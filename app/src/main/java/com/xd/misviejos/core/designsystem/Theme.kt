package com.xd.misviejos.core.designsystem

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

private val LightColorScheme = lightColorScheme(
    primary = TerracotaCalido,
    onPrimary = Color.White,
    secondary = VerdeEucalipto,
    onSecondary = Color.White,
    tertiary = AlertaMiel,
    onTertiary = TextoCarbon,
    background = FondoAvena,
    onBackground = TextoCarbon,
    surface = SuperficieBento,
    onSurface = TextoCarbon,
    surfaceVariant = SuperficieBentoGris,
    onSurfaceVariant = TextoGrisPlomo
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracotaNoche,
    onPrimary = FondoNoche,       // [JUGADA 1]: En oscuro, botón brillante + texto oscuro = Legibilidad absoluta.
    secondary = VerdeSageNoche,
    onSecondary = FondoNoche,
    tertiary = AlertaMiel,
    onTertiary = FondoNoche,
    background = FondoNoche,
    onBackground = TextoAvena,
    surface = SuperficieBentoNoche,
    onSurface = TextoAvena,
    surfaceVariant = SuperficieBentoGrisNoche,
    onSurfaceVariant = TextoGrisPlomo
)

@Composable
fun MisViejosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // [JUGADA 2]: Forzamos dynamicColor a FALSE por defecto.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // [JUGADA 3]: Inmersión nativa de la barra de estado superior (StatusBar)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // <-- Conectado a tu Plus Jakarta Sans de Type.kt
        content = content
    )
}