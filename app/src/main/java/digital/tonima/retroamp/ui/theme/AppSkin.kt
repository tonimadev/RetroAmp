package digital.tonima.retroamp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily

sealed class AppSkin(
    val name: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val buttonShape: Shape,
    val fontFamily: FontFamily,
    val visualizerColors: List<Color>,
    val forceAllCaps: Boolean = false
) {
    data object Winamp : AppSkin(
        name = "WINAMP",
        primaryColor = RetroSilver,
        secondaryColor = RetroBlue,
        backgroundColor = RetroBlack,
        surfaceColor = RetroDarkGrey,
        textColor = RetroSilver,
        accentColor = RetroNeonGreen,
        buttonShape = RectangleShape,
        fontFamily = FontFamily.Monospace,
        visualizerColors = listOf(Color.Green, Color.Cyan, Color.Blue)
    )

    data object EightBit : AppSkin(
        name = "8-BIT",
        primaryColor = Color(0xFFE40058), // NES Red
        secondaryColor = Color(0xFF8C8C8C), // NES Grey
        backgroundColor = Color.Black,
        surfaceColor = Color(0xFF333333),
        textColor = Color.White,
        accentColor = Color(0xFFF8B800), // NES Gold/Yellow
        buttonShape = RectangleShape,
        fontFamily = FontFamily.Monospace,
        visualizerColors = listOf(Color.Red, Color.Yellow, Color.White),
        forceAllCaps = true
    )
}
