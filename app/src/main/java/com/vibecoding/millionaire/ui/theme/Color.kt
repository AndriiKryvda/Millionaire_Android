package com.vibecoding.millionaire.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Studio palette: deep navy/purple stage with gold accents.
val NavyDark = Color(0xFF08082E)
val Navy = Color(0xFF101048)
val PurpleDeep = Color(0xFF241A63)
val Purple = Color(0xFF3A2A8C)
val Gold = Color(0xFFF5C542)
val GoldDark = Color(0xFFB8860B)
val GoldSoft = Color(0xFFFFE08A)
val PlateBlue = Color(0xFF1C2C78)
val PlateBlueLight = Color(0xFF2A3E9E)
val PlateSelected = Color(0xFFE08A00)
val PlateCorrect = Color(0xFF1E8E3E)
val PlateWrong = Color(0xFFB3261E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB9BEE8)
val Danger = Color(0xFFFF8A80)

val BackgroundGradient = Brush.verticalGradient(
    0f to NavyDark,
    0.45f to Navy,
    1f to PurpleDeep,
)

val PlateGradient = Brush.verticalGradient(
    0f to PlateBlueLight,
    1f to PlateBlue,
)