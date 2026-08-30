#version 150

uniform float PlanetRadius;
uniform float BloomRadius;
uniform vec3 SunTint;
uniform vec3 CameraLocalPos;

in  vec3 fragLocalPos;
out vec4 fragColor;

const float Density = 4.0;
const float ScatteringStrength = 1.0;
const int MARCH_STEPS = 3;
const float PI = 3.14159265358979;
const float DENSITY_CURVE = 0.97;
const vec3 COL_CORE = vec3(1.00, 0.98, 0.90);
const vec3 COL_MID = vec3(1.00, 0.65, 0.20);
const vec3 COL_OUTER = vec3(0.70, 0.20, 0.05);

struct Ray {
    vec3 origin;
    vec3 direction;
};

vec2 raySphere(Ray ray, float r) {
    float b = dot(ray.origin, ray.direction);
    float c = dot(ray.origin, ray.origin) - r * r;
    float d = b * b - c;
    if (d < 0.0) return vec2(-1.0);
    d = sqrt(d);
    vec2 t = vec2(-b - d, -b + d);
    if (t.y < 0.0) return vec2(-1.0);
    return max(t, 0.0);
}

float getDensityAtPoint(vec3 p, float inner, float outer, float falloff) {

    float height = clamp((length(p) - inner) / max(outer - inner, 0.0001), 0.0, 1.0);
    float k = falloff * 10.0;
    float denom = 1.0 + height * k;
    float curveFactor = pow(denom, DENSITY_CURVE);

    return (1.0 - height) / max(curveFactor, 0.0001);
}

vec3 acesToneMap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    if (!gl_FrontFacing) discard;

    Ray viewRay;
    viewRay.origin = CameraLocalPos;
    viewRay.direction = normalize(fragLocalPos - CameraLocalPos);

    float inner = PlanetRadius;
    float outer = max(BloomRadius, inner + 0.0001);

    vec2 coreHit = raySphere(viewRay, inner);
    vec2 bloomHit = raySphere(viewRay, outer);

    if (bloomHit.y < 0.0) discard;

    vec3 accumulatedGlow = vec3(0.0);
    float transmit = 1.0;

    float tStart = bloomHit.x;
    float tEnd = (coreHit.x >= 0.0) ? coreHit.x : bloomHit.y;

    if (tEnd > tStart) {

        float shellThickness = outer - inner;
        float stepSize = (tEnd - tStart) / float(MARCH_STEPS);
        float normalizedStep = stepSize / max(shellThickness, 0.0001);

        for (int i = 0; i < MARCH_STEPS; i++) {

            float t = tStart + (float(i) + 0.5) * stepSize;
            vec3  p = viewRay.origin + viewRay.direction * t;
            float h = clamp((length(p) - inner) / max(shellThickness, 0.0001), 0.0, 1.0);
            float d = getDensityAtPoint(p, inner, outer, Density);

            vec3 stepColor = mix(COL_MID, COL_OUTER, h);

            accumulatedGlow += stepColor * d * normalizedStep * ScatteringStrength * 16.0 * transmit;
            transmit *= exp(-d * normalizedStep * ScatteringStrength * 3.0);

            if (transmit < 0.001) break;
        }
    }

    vec3 finalColor = accumulatedGlow * SunTint;
    float finalAlpha = 1.0 - transmit;
    bool isCore = false;

    if (coreHit.x >= 0.0) {
        vec3  hitPos = viewRay.origin + viewRay.direction * coreHit.x;
        vec3  normal = normalize(hitPos);
        float mu = max(dot(-viewRay.direction, normal), 0.0);

        float ld = 0.35 + 0.65 * sqrt(mu);
        vec3 coreColor = mix(COL_MID, COL_CORE, ld) * 2.5 * SunTint;

        finalColor += coreColor;
        finalAlpha = 1.0;
        isCore = true;
    }

    if (finalAlpha < 0.0001) discard;

    vec3 outColor;
    if (isCore) {
        outColor = acesToneMap(finalColor * 1.4);
    } else {
        outColor = acesToneMap(finalColor);
    }

    fragColor = vec4(outColor, clamp(finalAlpha, 0.0, 1.0));
}