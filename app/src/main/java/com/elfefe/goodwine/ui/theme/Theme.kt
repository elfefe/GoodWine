package com.elfefe.goodwine.ui.theme

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
import androidx.core.view.ViewCompat

private val DarkColorScheme = darkColorScheme(
    primary = LightNormal,
    secondary = LightGrey,
    tertiary = LightClear,
    onPrimary = Color.White,
    onPrimaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightNormal,
    secondary = LightGrey,
    tertiary = LightClear,
    onPrimary = Color.Black,
    onPrimaryContainer = Color.White
)

@Composable
fun GoodWineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Le thème dynamique d'Android 12+ remplace toute la palette par celle du fond d'écran :
    // le bordeaux de l'app disparaissait, et les couleurs choisies pour le texte des fiches
    // n'avaient plus le contraste prévu. On garde la palette du projet.
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}