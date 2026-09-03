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

private const val PSYCHEDELIC_SHADER = """
    uniform float uTime;
    uniform float uAmplitude;
    uniform float2 uResolution;

    half4 main(float2 fragCoord) {
        float2 p = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
        
        float t = uTime * 0.5;
        float amp = uAmplitude * 2.0;
        
        float3 color = float3(0.0);
        
        for(float i = 1.0; i < 4.0; i++) {
            p.x += 0.3 / i * sin(i * 3.0 * p.y + t + amp) + 0.5;
            p.y += 0.3 / i * cos(i * 3.0 * p.x + t + amp) + 0.5;
            color += float3(0.5 + 0.5 * sin(t + i), 0.5 + 0.5 * cos(t + i + 2.0), 0.5 + 0.5 * sin(t + i + 4.0)) / length(p);
        }
        
        color /= 3.0;
        color *= (0.3 + amp * 0.7);
        
        return half4(color, 1.0);
    }
"""

@Composable
fun RetroVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslVisualizer(amplitude, modifier)
    } else {
        FallbackVisualizer(amplitude, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizerTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "time"
    )

    val shader = remember { RuntimeShader(PSYCHEDELIC_SHADER) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uAmplitude", amplitude)
                shader.setFloatUniform("uResolution", size.width, size.height)
                val brush = ShaderBrush(shader)
                onDrawBehind {
                    drawRect(brush)
                }
            }
    ) {
        // Handled by drawWithCache
    }
}

@Composable
private fun FallbackVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    // Simple bar visualizer for older APIs
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 20
        val spacing = 4.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * spacing) / barCount
        
        for (i in 0 until barCount) {
            val h = size.height * (amplitude * (0.5f + Math.random().toFloat() * 0.5f))
            drawRect(
                color = Color.Green,
                topLeft = Offset(i * (barWidth + spacing), size.height - h),
                size = Size(barWidth, h)
            )
        }
    }
}
