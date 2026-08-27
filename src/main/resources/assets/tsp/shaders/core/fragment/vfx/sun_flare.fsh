#version 150

in vec2 texCoord;
in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec3 SunTint;
uniform float Time;
uniform float FlareAlpha;

out vec4 fragColor;

void main() {
    vec2 uv = (texCoord - vec2(0.5)) * 2.0;
    float dist = length(uv);

    if (dist > 1.0) {
        discard;
    }

    float angle = atan(uv.y, uv.x);

    float core = exp(-dist * 12.0) * 3.0;

    float pulse = sin(Time * 1.2) * 0.04;
    float halo = exp(-dist * 3.5) * (0.7 + pulse);

    float spikeMod1 = 0.85 + 0.15 * sin(angle * 2.0 + Time * 0.3);
    float spikeMod2 = 0.75 + 0.25 * cos(angle * 3.0 - Time * 0.2);

    float primarySpikes = pow(abs(cos(angle * 2.0 + Time * 0.04)), 40.0) * exp(-dist * 2.5) * 0.7 * spikeMod1;
    float secondarySpikes = pow(abs(cos(angle * 2.0 - Time * 0.07)), 16.0) * exp(-dist * 4.0) * 0.4 * spikeMod2;

    float mask = smoothstep(1.0, 0.6, dist);

    vec3 coreColor = mix(vec3(1.0), SunTint, smoothstep(0.0, 0.35, dist));
    vec3 finalRGB = (coreColor * core) + (SunTint * (halo + primarySpikes + secondarySpikes));

    finalRGB *= mask;

    float alpha = max(finalRGB.r, max(finalRGB.g, finalRGB.b)) * FlareAlpha * vertexColor.a * ColorModulator.a;

    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(finalRGB, alpha);
}