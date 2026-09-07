#version 150

uniform float Time;
uniform float CloudWindSpeed;
uniform float CloudCoverage;
uniform float CloudNoiseScale;
uniform vec4 CloudColor;
uniform vec3 LightDirection;

uniform vec3 CameraPos;
uniform float MaxDistance;

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

const float NIGHT_BRIGHTNESS    = 0.10;
const vec3  NIGHT_COLOR_TINT    = vec3(0.55, 0.65, 1.00);
const float NIGHT_TINT_STRENGTH = 0.35;

vec2 rotate2D(vec2 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

float sampleTexValue(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return (c.a > 0.0 && c.a < 1.0) ? c.a : max(c.r, max(c.g, c.b));
}

float sampleCloudNoise2D(vec2 p, float time, float speed, float coverage) {
    if (coverage <= 0.001) return 0.0;

    float wind = time * (clamp(speed, -1.0, 1.0) * 0.0050);
    float sc = max(CloudNoiseScale, 0.001) * 0.002;

    vec2 p1 = rotate2D(p, wind * 0.5) * (1.25 * sc);
    float noise1 = sampleTexValue(p1);

    vec2 p2 = rotate2D(p, -wind * 0.85) * (2.6 * sc) + vec2(37.2, 11.4);
    float noise2 = sampleTexValue(p2);

    vec2 p3 = rotate2D(p, wind * 1.3) * (5.2 * sc) + vec2(91.7, 5.3);
    float noise3 = sampleTexValue(p3);

    float combined = noise1 * 0.52 + noise2 * 0.32 + noise3 * 0.16;

    float threshold = 1.0 - clamp(coverage, 0.0, 1.0);
    float edgeSoftness = 0.18;
    float cloudDensity = smoothstep(threshold - edgeSoftness * 0.5, threshold + edgeSoftness * 0.5, combined);

    return clamp(cloudDensity, 0.0, 1.0);
}

void main() {
    float dist = length(texCoord0 - CameraPos.xz);
    float fade = 1.0 - smoothstep(MaxDistance * 0.60, MaxDistance * 0.95, dist);

    float density = sampleCloudNoise2D(texCoord0, Time, CloudWindSpeed, CloudCoverage);
    density *= fade;

    if (density <= 0.001) discard;

    float eps = 2.5;
    float dR = sampleCloudNoise2D(texCoord0 + vec2(eps, 0.0), Time, CloudWindSpeed, CloudCoverage);
    float dL = sampleCloudNoise2D(texCoord0 - vec2(eps, 0.0), Time, CloudWindSpeed, CloudCoverage);
    float dT = sampleCloudNoise2D(texCoord0 + vec2(0.0, eps), Time, CloudWindSpeed, CloudCoverage);
    float dB = sampleCloudNoise2D(texCoord0 - vec2(0.0, eps), Time, CloudWindSpeed, CloudCoverage);

    float bumpiness = 2.0;
    vec3 normal = normalize(vec3((dL - dR) * bumpiness, 1.0, (dB - dT) * bumpiness));

    vec3 sunDir = normalize(LightDirection);
    vec3 viewDir = normalize(vec3(texCoord0.x - CameraPos.x, 150.0, texCoord0.y - CameraPos.z));

    float wrap = 0.4;
    float NdotL = max(0.0, (dot(normal, sunDir) + wrap) / (1.0 + wrap));

    float viewDotSun = max(0.0, dot(viewDir, sunDir));
    float sss = pow(viewDotSun, 3.0) * (1.0 - density * 0.5);

    vec2 lightOffset = sunDir.xz * 4.0;
    float densityTowardsSun = sampleCloudNoise2D(texCoord0 + lightOffset, Time, CloudWindSpeed, CloudCoverage);
    float opticalDepth = max(0.0, densityTowardsSun - density * 0.1);
    float shadow = mix(0.65, 1.0, exp(-opticalDepth * 1.5));

    float diffuse = NdotL * shadow;
    float finalLighting = clamp((0.35 + 0.65 * diffuse) + sss * 0.8, 0.35, 1.2);

    float dayBlend = smoothstep(-0.15, 0.15, sunDir.y);

    vec3 litColor = CloudColor.rgb * finalLighting;
    vec3 nightColor = CloudColor.rgb * mix(vec3(1.0), NIGHT_COLOR_TINT, NIGHT_TINT_STRENGTH) * NIGHT_BRIGHTNESS;
    vec3 finalColor = mix(nightColor, litColor, dayBlend);

    fragColor = vec4(finalColor, density * clamp(CloudColor.a, 0.0, 1.0));
}