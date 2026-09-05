package digital.tonima.retroamp.player.ui

object Shaders {
    const val PSYCHEDELIC_SHADER = """
        uniform float uTime;
        uniform float uAmplitude;
        uniform vec2 uResolution; // Use vec2, vec3, vec4 padrão do GLSL

        half4 main(float2 fragCoord) {
            // No AGSL, vec2 e float2 são intercambiáveis, mas vec2 é o padrão do GLSL.
            vec2 p = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
            
            float t = uTime * 0.5;
            float amp = max(uAmplitude, 0.05); // Garante visibilidade mínima
            
            vec3 color = vec3(0.0);
            
            float iterations = 3.0 + floor(amp * 5.0);
            
            for(float i = 1.0; i < 8.0; i += 1.0) { // i += 1.0 é mais seguro em GLSL antigo
                if (i > iterations) break;
                
                float freq = 3.0 + amp * 5.0;
                p.x += (0.3 / i) * sin(i * freq * p.y + t + amp * 10.0) + 0.5;
                p.y += (0.3 / i) * cos(i * freq * p.x + t + amp * 10.0) + 0.5;
                
                vec3 shift = vec3(
                    0.5 + 0.5 * sin(t + i + amp), 
                    0.5 + 0.5 * cos(t + i + 2.0 + amp * 1.5), 
                    0.5 + 0.5 * sin(t + i + 4.0 + amp * 2.0)
                );
                
                // Evita divisão por zero se length(p) for muito perto de 0
                color += shift / (length(p) + 0.001); 
            }
            
            color /= iterations;
            color *= (0.5 + amp * 3.0);
            
            // clamped para evitar estourar o limite de cor e gerar artefatos em alguns displays HDR
            return half4(clamp(color, 0.0, 1.0), 1.0); 
        }
    """

    const val TUNNEL_SHADER = """
        uniform float uTime;
        uniform float uAmplitude;
        uniform vec2 uResolution; // Preferência por vec2

        half4 main(float2 fragCoord) {
            vec2 uv = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
            float amp = max(uAmplitude, 0.05);
            
            float distortion = amp * 0.8;
            uv *= 1.0 + distortion * sin(length(uv) * 10.0 - uTime * 5.0);
            
            // No AGSL, atan(y, x) é equivalente a atan2 e funciona para polares
            float a = atan(uv.y, uv.x); 
            float r = length(uv);
            
            float t = uTime * 0.8;
            
            // a / Pi converte o ângulo para o espaço de textura
            vec2 st = vec2(a / 3.14159265, 0.1 / (r + 0.01) + t * (1.0 + amp * 2.0));
            
            vec3 color = vec3(0.0);
            color.r = 0.5 + 0.5 * sin(st.y * 10.0 + a + amp * 10.0);
            color.g = 0.5 + 0.5 * sin(st.y * 11.0 + a * 1.5 + amp * 8.0);
            color.b = 0.5 + 0.5 * sin(st.y * 12.0 + a * 2.0 + amp * 6.0);
            
            float pulse = 0.5 + amp * 4.0;
            color *= r * pulse;
            
            if (amp > 0.7) {
                // Adiciona o brilho para baterias pesadas (Kicks/Snares)
                color += vec3((amp - 0.7) * 3.0); 
            }
            
            return half4(clamp(color, 0.0, 1.0), 1.0);
        }
    """
}
