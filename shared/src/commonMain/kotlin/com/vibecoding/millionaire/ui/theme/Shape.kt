package com.vibecoding.millionaire.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * The pointed "answer plate" of the studio set: a rectangle with cut left/right ends.
 * [cut] is used both for the outline and for the inner text padding.
 */
class HexagonShape(private val cut: Dp = 14.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cutX = minOf(with(density) { cut.toPx() }, size.width / 2f, size.height / 2f)
        val path = Path().apply {
            moveTo(cutX, 0f)
            lineTo(size.width - cutX, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width - cutX, size.height)
            lineTo(cutX, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Rounded panel used for the question and for cards. */
val MillionaireShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)