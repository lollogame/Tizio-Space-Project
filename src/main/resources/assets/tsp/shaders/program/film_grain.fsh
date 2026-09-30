#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform float Intensity;
uniform float GrainSize;
uniform float GrainTime;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float GRAIN_FPS = 24.0;

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p, float frame) {

    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash13(vec3(i, frame));
    float b = hash13(vec3(i + vec2(1.0, 0.0), frame));
    float c = hash13(vec3(i + vec2(0.0, 1.0), frame));
    float d = hash13(vec3(i + vec2(1.0, 1.0), frame));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);

}

void main() {

    vec4 scene = texture(DiffuseSampler, texCoord);
    float cell = max(GrainSize, 0.25) * max(OutSize.y, 1.0) / 1080.0;
    vec2 p = texCoord * OutSize / cell;
    float frame = floor(GrainTime * GRAIN_FPS);
    float grain = (valueNoise(p, frame) + valueNoise(p * 1.73 + 19.19, frame + 7.0)) * 0.5 - 0.5;
    float luminance = dot(scene.rgb, LUMINANCE_WEIGHTS);
    float midtones = clamp(4.0 * luminance * (1.0 - luminance), 0.0, 1.0);
    float mask = 0.3 + 0.7 * midtones;
    vec3 result = scene.rgb + vec3(grain * clamp(Intensity, 0.0, 1.0) * mask);
    fragColor = vec4(clamp(result, 0.0, 1.0), scene.a);
}
