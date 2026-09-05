package digital.tonima.retroamp.player.ui

object Shaders {
    const val PSYCHEDELIC_SHADER = """
        uniform float uTime;
        uniform float uAmplitude;
        uniform vec2 uResolution;
        layout(color) uniform vec4 uColor1;
        layout(color) uniform vec4 uColor2;
        layout(color) uniform vec4 uColor3;

        half4 main(float2 fragCoord) {
            vec2 p = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
            
            float t = uTime * 0.5;
            float amp = max(uAmplitude, 0.05);
            
            vec3 color = vec3(0.0);
            
            float iterations = 3.0 + floor(amp * 5.0);
            
            for(float i = 1.0; i < 8.0; i += 1.0) {
                if (i > iterations) break;
                
                float freq = 3.0 + amp * 5.0;
                p.x += (0.3 / i) * sin(i * freq * p.y + t + amp * 10.0) + 0.5;
                p.y += (0.3 / i) * cos(i * freq * p.x + t + amp * 10.0) + 0.5;
                
                // Mix between skin colors instead of hardcoded rainbow
                float blend = 0.5 + 0.5 * sin(t + i);
                vec3 baseColor = mix(uColor1.rgb, uColor2.rgb, blend);
                baseColor = mix(baseColor, uColor3.rgb, 0.5 + 0.5 * cos(t * 0.7 + i));
                
                vec3 shift = baseColor * (0.8 + 0.2 * sin(t + i + amp));
                
                color += shift / (length(p) + 0.001); 
            }
            
            color /= iterations;
            color *= (0.5 + amp * 3.0);
            
            return half4(clamp(color, 0.0, 1.0), 1.0); 
        }
    """

    const val TUNNEL_SHADER = """
        uniform float uTime;
        uniform float uAmplitude;
        uniform vec2 uResolution;
        layout(color) uniform vec4 uColor1;
        layout(color) uniform vec4 uColor2;
        layout(color) uniform vec4 uColor3;

        half4 main(float2 fragCoord) {
            vec2 uv = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);
            float amp = max(uAmplitude, 0.05);
            
            float distortion = amp * 0.8;
            uv *= 1.0 + distortion * sin(length(uv) * 10.0 - uTime * 5.0);
            
            float a = atan(uv.y, uv.x); 
            float r = length(uv);
            
            float t = uTime * 0.8;
            
            vec2 st = vec2(a / 3.14159265, 0.1 / (r + 0.01) + t * (1.0 + amp * 2.0));
            
            float wave = 0.5 + 0.5 * sin(st.y * 10.0 + a + amp * 10.0);
            vec3 color = mix(uColor1.rgb, uColor2.rgb, wave);
            
            // Add uColor3 for dynamic highlights
            float highlight = smoothstep(0.8, 1.0, sin(st.y * 15.0 + a * 2.0));
            color = mix(color, uColor3.rgb, highlight * 0.5);
            
            float pulse = 0.5 + amp * 4.0;
            color *= r * pulse;
            
            if (amp > 0.7) {
                color += vec3((amp - 0.7) * 3.0); 
            }
            
            return half4(clamp(color, 0.0, 1.0), 1.0);
        }
    """

}
