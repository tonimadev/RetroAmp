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
            vec2 uv = (fragCoord.xy * 2.0 - uResolution.xy) / min(uResolution.x, uResolution.y);

            float amp = max(uAmplitude, 0.05);
            float t = uTime * (0.4 + amp * 0.6);
            float freq = 2.0 + amp * 4.0;
            float iterations = 3.0 + floor(amp * 4.0);

            // Classic domain-warp plasma: each iteration nudges p by a
            // shrinking, purely oscillating amount, so p stays bounded near
            // the original pixel instead of drifting off to infinity (the
            // previous version added a stray "+ 0.5" every step, which
            // pushed p far from the screen after only a couple of
            // iterations and collapsed the whole effect into a thin band).
            vec2 p = uv;
            for (float i = 1.0; i < 8.0; i += 1.0) {
                if (i > iterations) break;
                p.x += (0.35 / i) * sin(i * freq * p.y + t);
                p.y += (0.35 / i) * cos(i * freq * p.x + t);
            }

            // Smooth, singularity-free color field driven by the warped
            // coordinates - no 1/length(p) term, so brightness can't spike
            // to white when p happens to pass near zero.
            float field = sin(p.x * 3.0 + t) + cos(p.y * 3.0 - t * 0.7);
            float blend1 = 0.5 + 0.5 * sin(field + t);
            float blend2 = 0.5 + 0.5 * cos(field * 1.3 - t * 0.5);

            vec3 color = mix(uColor1.rgb, uColor2.rgb, blend1);
            color = mix(color, uColor3.rgb, blend2 * 0.6);

            // Brightness breathes with the music but is capped well before
            // clamp() would flatten it to solid white.
            color *= (0.55 + amp * 0.65);

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

            float distortion = amp * 0.5;
            uv *= 1.0 + distortion * sin(length(uv) * 10.0 - uTime * 5.0);

            float a = atan(uv.y, uv.x);
            float r = length(uv);

            float t = uTime * 0.8;

            vec2 st = vec2(a / 3.14159265, 0.12 / (r + 0.05) + t * (1.0 + amp * 1.5));

            float wave = 0.5 + 0.5 * sin(st.y * 10.0 + a + amp * 6.0);
            vec3 color = mix(uColor1.rgb, uColor2.rgb, wave);

            // Add uColor3 for dynamic highlights
            float highlight = smoothstep(0.85, 1.0, sin(st.y * 15.0 + a * 2.0));
            color = mix(color, uColor3.rgb, highlight * 0.5);

            // Depth shading: darker toward the vanishing point, brighter
            // toward the rim - clamped to [0, 1] up front so loud passages
            // make it glow instead of blowing the whole frame out to solid
            // white (the previous "* pulse" here had no ceiling and easily
            // exceeded 1.0 across most of the screen).
            float depth = clamp(r * (0.7 + amp * 0.5), 0.0, 1.0);
            color *= depth;

            return half4(clamp(color, 0.0, 1.0), 1.0);
        }
    """

}
