package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintTealLight,
    onPrimary = MintTealDark,
    secondary = MintTealLight,
    onSecondary = MintTealDark,
    tertiary = StarGold,
    background = Color(0xFF111414), // Dark sage black
    surface = Color(0xFF1E2222),
    onBackground = SoftBeigeBg,
    onSurface = SoftBeigeBg,
    outline = GrayDivider,
    surfaceVariant = LightGrayContainer
)

private val LightColorScheme = lightColorScheme(
    primary = MintTealPrimary,
    onPrimary = Color.White,
    secondary = MintTealLight,
    onSecondary = MintTealDark,
    tertiary = StarGold,
    background = SoftBeigeBg,
    surface = Color.White,
    onBackground = OffBlack,
    onSurface = OffBlack,
    outline = GrayDivider,
    surfaceVariant = LightGrayContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce branding
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
        content = content
    )
}
