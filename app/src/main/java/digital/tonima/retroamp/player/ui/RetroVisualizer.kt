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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun RetroVisualizer(
    amplitudeProvider: () -> Float,
    mode: Int,
    onToggleMode: () -> Unit,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslVisualizer(amplitudeProvider, mode, onToggleMode, onToggleFullScreen, modifier)
    } else {
        FallbackVisualizer(amplitudeProvider, mode, onToggleMode, onToggleFullScreen, modifier)
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
    val timeState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "time"
    )

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
                onDrawBehind {
                    shader.setFloatUniform("uTime", timeState.value)
                    shader.setFloatUniform("uAmplitude", amplitudeProvider())
                    shader.setFloatUniform("uResolution", size.width, size.height)
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "time"
    )

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
        
        val barColor = when (mode) {
            0 -> Color.Green
            else -> Color.Cyan
        }

        val timeOffset = (time * 2.0 * Math.PI) // Calculado 1x por frame
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
