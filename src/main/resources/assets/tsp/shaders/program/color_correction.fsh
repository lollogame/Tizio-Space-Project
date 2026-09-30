#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform int TonemapperMode;
uniform float Exposure;
uniform float Contrast;
uniform float Saturation;
uniform float VignetteIntensity;
uniform float Temperature;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec3 tonemapACES(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 hablePartial(vec3 x) {
    const float A = 0.15;
    const float B = 0.50;
    const float C = 0.10;
    const float D = 0.20;
    const float E = 0.02;
    const float F = 0.30;
    return ((x * (A * x + C * B) + D * E) / (x * (A * x + B) + D * F)) - E / F;
}

vec3 tonemapHable(vec3 x) {
    const float exposureBias = 2.0;
    vec3 curr = hablePartial(x * exposureBias);
    vec3 whiteScale = vec3(1.0) / hablePartial(vec3(11.2));
    return clamp(curr * whiteScale, 0.0, 1.0);
}

vec3 tonemapUnreal(vec3 x) {
    return clamp(x / (x + 0.155) * 1.019, 0.0, 1.0);
}

vec3 tonemapAgX(vec3 val) {

    const mat3 agxInset = mat3(
        0.84247928, 0.07843360, 0.07922372,
        0.04232824, 0.87846864, 0.07916612,
        0.04237565, 0.07843360, 0.87914297
    );

    const mat3 agxOutset = mat3(
        1.19682101, -0.09802088, -0.09879770,
        -0.05289685, 1.15190312, -0.09894273,
        -0.05297163, -0.09804345, 1.15196878
    );

    val = agxInset * val;

    const float minEv = -10.0;
    const float maxEv = +6.5;
    val = clamp(val, 1e-10, 1e10);
    val = clamp((log2(val) - minEv) / (maxEv - minEv), 0.0, 1.0);
    vec3 val2 = val * val;
    vec3 val4 = val2 * val2;
    val = 15.5 * val4 * val - 40.14 * val4 + 31.96 * val2 * val - 6.868 * val2 + 0.4298 * val + 0.0155;

    val = agxOutset * val;
    return clamp(val, 0.0, 1.0);
}

vec3 applyTonemap(vec3 color, int mode) {
    if (mode == 0) return tonemapHable(color);
    if (mode == 2) return tonemapUnreal(color);
    if (mode == 3) return tonemapAgX(color);
    return tonemapACES(color);
}

vec3 applyTemperature(vec3 color, float temperature) {
    float warm = max(temperature, 0.0);
    float cold = max(-temperature, 0.0);

    color.r += warm * 0.035 - cold * 0.025;
    color.g += warm * 0.015 + cold * 0.005;
    color.b -= warm * 0.025 - cold * 0.035;

    return max(color, vec3(0.0));
}

vec3 applyContrast(vec3 color, float contrast) {
    color = clamp(color, 0.0, 1.0);
    return clamp((color - 0.5) * contrast + 0.5, 0.0, 1.0);
}

vec3 applySaturation(vec3 color, float saturation) {
    float luminance = dot(color, LUMINANCE_WEIGHTS);
    float darkFactor = smoothstep(0.0, 0.25, luminance);
    float finalSaturation = mix(1.0, saturation, darkFactor);
    return mix(vec3(luminance), color, finalSaturation);
}

float calculateVignette(vec2 uv, float intensity) {
    float distanceFromCenter = distance(uv, vec2(0.5));
    float vignette = smoothstep(0.90, 0.35, distanceFromCenter);
    return mix(1.0, vignette, clamp(intensity, 0.0, 1.0) * 0.35);
}

void main() {

    vec4 baseScene = texture(DiffuseSampler, texCoord);
    vec3 rawColor = baseScene.rgb * Exposure;
    vec3 dominantBase = applyTonemap(rawColor, TonemapperMode);
    vec3 secondaryGraded = applyTemperature(dominantBase, Temperature);
    secondaryGraded = applyContrast(secondaryGraded, Contrast);

    vec3 compositeColor = mix(dominantBase, secondaryGraded, 0.75);

    compositeColor = applySaturation(compositeColor, Saturation);
    float vignette = calculateVignette(texCoord, VignetteIntensity);
    compositeColor *= vignette;

    fragColor = vec4(clamp(compositeColor, 0.0, 1.0), baseScene.a);
}