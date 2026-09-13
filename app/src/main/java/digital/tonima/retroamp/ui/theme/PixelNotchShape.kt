package digital.tonima.retroamp.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A stair-stepped, "cut corner" outline reminiscent of low-resolution 8-bit
 * game UI chrome (NES/Game Boy style buttons and panels), as opposed to a
 * perfectly smooth rectangle or rounded corner - which is all a plain
 * [androidx.compose.ui.graphics.RectangleShape] with a different color can
 * ever look like.
 */
class PixelNotchShape(private val notchSizeDp: Float = 4f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val notch = with(density) { notchSizeDp.dp.toPx() }
            .coerceAtMost(minOf(size.width, size.height) / 3f)

        val path = Path().apply {
            moveTo(notch, 0f)
            lineTo(size.width - notch, 0f)
            lineTo(size.width - notch, notch)
            lineTo(size.width, notch)
            lineTo(size.width, size.height - notch)
            lineTo(size.width - notch, size.height - notch)
            lineTo(size.width - notch, size.height)
            lineTo(notch, size.height)
            lineTo(notch, size.height - notch)
            lineTo(0f, size.height - notch)
            lineTo(0f, notch)
            lineTo(notch, notch)
            close()
        }
        return Outline.Generic(path)
    }
}
