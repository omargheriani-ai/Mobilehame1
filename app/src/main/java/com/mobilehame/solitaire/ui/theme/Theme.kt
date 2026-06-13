package com.mobilehame.solitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FeltGreen = Color(0xFF2D6A2D)
val FeltGreenDark = Color(0xFF1A3D1A)
val CardWhite = Color(0xFFFFFDE7)
val CardRed = Color(0xFFD32F2F)
val CardBlack = Color(0xFF212121)
val CardBack = Color(0xFF1565C0)
val CardBackPattern = Color(0xFF1976D2)
val FoundationEmpty = Color(0xFF3D8B3D)
val HighlightColor = Color(0xFFFFD700)

private val SolitaireColorScheme = darkColorScheme(
    primary = HighlightColor,
    secondary = CardWhite,
    background = FeltGreen,
    surface = FeltGreenDark,
    onPrimary = CardBlack,
    onSecondary = CardBlack,
    onBackground = CardWhite,
    onSurface = CardWhite
)

@Composable
fun SolitaireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SolitaireColorScheme,
        content = content
    )
}
