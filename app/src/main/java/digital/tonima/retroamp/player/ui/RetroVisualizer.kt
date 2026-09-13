package digital.tonima.retroamp.player.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import digital.tonima.retroamp.ui.theme.AppSkin
import kotlin.math.sin

@Composable
fun RetroVisualizer(
    amplitudeProvider: () -> Float,
    mode: Int,
    onToggleMode: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    skin: AppSkin = AppSkin.Winamp,
    isAnimating: Boolean = true,
    batterySaverMode: Boolean = false
) {
    // Battery saver forces the cheap bar renderer even on devices that support
    // the AGSL shader path, since the per-pixel shader is by far the heaviest
    // part of the visualizer.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !batterySaverMode) {
        AgslVisualizer(amplitudeProvider, mode, onToggleMode, onToggleFullScreen, modifier, skin, isAnimating)
    } else {
        FallbackVisualizer(amplitudeProvider, mode, onToggleMode, onToggleFullScreen, modifier, skin, isAnimating)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslVisualizer(
    amplitudeProvider: () -> Float,
    mode: Int,
    onToggleMode: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    skin: AppSkin = AppSkin.Winamp,
    isAnimating: Boolean = true
) {
    // While paused/stopped there is nothing new to visualize: not subscribing
    // to the infinite animation at all (instead of merely ignoring its value)
    // stops Compose from re-evaluating and redrawing the shader every frame,
    // which is where most of the visualizer's battery/GPU cost comes from.
    val frozenTime = remember { mutableFloatStateOf(0f) }
    val timeValue: Float
    if (isAnimating) {
        val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
        val animatedTime by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing)
            ),
            label = "time"
        )
        frozenTime.floatValue = animatedTime
        timeValue = animatedTime
    } else {
        timeValue = frozenTime.floatValue
    }

    val shaderString = when (mode) {
        0 -> Shaders.PSYCHEDELIC_SHADER
        else -> Shaders.TUNNEL_SHADER
    }
    
    val shader = remember(shaderString) { RuntimeShader(shaderString) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onToggleMode,
                onLongClick = onToggleFullScreen
            )
            .drawWithCache {
                val brush = ShaderBrush(shader)
                val color1 = skin.visualizerColors.getOrElse(0) { Color.Green }
                val color2 = skin.visualizerColors.getOrElse(1) { color1 }
                val color3 = skin.visualizerColors.getOrElse(2) { color2 }
                
                onDrawBehind {
                    shader.setFloatUniform("uTime", timeValue)
                    shader.setFloatUniform("uAmplitude", amplitudeProvider())
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    
                    shader.setColorUniform("uColor1", color1.toArgb())
                    shader.setColorUniform("uColor2", color2.toArgb())
                    shader.setColorUniform("uColor3", color3.toArgb())
                    
                    drawRect(brush)
                }
            }
    ) {
        // Handled by drawWithCache
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FallbackVisualizer(
    amplitudeProvider: () -> Float,
    mode: Int,
    onToggleMode: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    skin: AppSkin = AppSkin.Winamp,
    isAnimating: Boolean = true
) {
    // Same reasoning as AgslVisualizer: don't subscribe to the infinite
    // animation while paused/stopped so the canvas stops redrawing.
    val frozenTime = remember { mutableFloatStateOf(0f) }
    val time: Float
    if (isAnimating) {
        val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
        val animatedTime by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing)
            ),
            label = "time"
        )
        frozenTime.floatValue = animatedTime
        time = animatedTime
    } else {
        time = frozenTime.floatValue
    }

    // Simple bar visualizer for older APIs
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onToggleMode,
                onLongClick = onToggleFullScreen
            )
    ) {
        val barCount = 20
        val spacing = 4.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * spacing) / barCount
        
        val barColor = if (mode < skin.visualizerColors.size) {
            skin.visualizerColors[mode]
        } else {
            skin.visualizerColors.firstOrNull() ?: Color.Green
        }

        val timeOffset = (time * 2.0 * Math.PI)
        for (i in 0 until barCount) {
            val variation = 0.7f + 0.3f * sin(timeOffset + i).toFloat()
            val currentAmplitude = amplitudeProvider() // Leitura deferida
            val h = size.height * (currentAmplitude * variation).coerceAtLeast(0.05f)
            drawRect(
                color = barColor,
                topLeft = Offset(i * (barWidth + spacing), size.height - h),
                size = Size(barWidth, h)
            )
        }
    }
}
