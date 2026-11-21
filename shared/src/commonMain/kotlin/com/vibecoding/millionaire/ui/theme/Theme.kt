package com.vibecoding.millionaire.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MillionaireColors = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDark,
    primaryContainer = GoldDark,
    onPrimaryContainer = NavyDark,
    secondary = GoldSoft,
    onSecondary = NavyDark,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = Navy,
    onSurface = TextPrimary,
    surfaceVariant = PurpleDeep,
    onSurfaceVariant = TextSecondary,
    outline = Purple,
    error = Danger,
    onError = NavyDark,
)

private val MillionaireTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun MillionaireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MillionaireColors,
        typography = MillionaireTypography,
        shapes = MillionaireShapes,
        content = content,
    )
}

/** The signature navy-to-purple gradient behind every screen (edge-to-edge safe). */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Material3 does not provide a default content color through MaterialTheme, so any Text
    // without an explicit color would fall back to black and be unreadable on the dark stage.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .safeDrawingPadding(),
        ) {
            content()
        }
    }
}