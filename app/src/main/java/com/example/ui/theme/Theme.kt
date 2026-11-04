package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Jet Black, Royal Amethyst, Electric Cyan theme
private val AmethystCyanColorScheme = darkColorScheme(
    primary = RoyalAmethyst,
    onPrimary = Color.White,
    primaryContainer = RoyalAmethystContainer,
    onPrimaryContainer = RoyalAmethystGlow,
    
    secondary = ElectricCyan,
    onSecondary = Color.Black,
    secondaryContainer = ElectricCyanContainer,
    onSecondaryContainer = ElectricCyanNeon,

    tertiary = RoyalAmethystGlow,
    onTertiary = Color.Black,
    tertiaryContainer = RoyalAmethystDeep,
    onTertiaryContainer = Color.White,

    background = JetBlack,
    onBackground = Color(0xFFF1F5F9),
    
    surface = JetBlackElevated,
    onSurface = Color(0xFFF8FAFC),
    
    surfaceVariant = JetBlackCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    
    outline = JetBlackBorder,
    outlineVariant = JetBlackMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Both dark and light apply the signature JET BLACK, ROYAL AMETHYST, ELECTRIC CYAN theme
    MaterialTheme(
        colorScheme = AmethystCyanColorScheme,
        typography = Typography,
        content = content
    )
}
