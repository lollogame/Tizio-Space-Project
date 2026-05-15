#version 150

uniform float PlanetRadius;
uniform float SunRadius;
uniform float BloomRadius;
uniform float Density;
uniform float ScatteringStrength;
uniform float Time;
uniform vec3  SunTint;
uniform vec3  CameraLocalPos;
uniform sampler2D Sampler0;

in  vec3 fragLocalPos;
out vec4 fragColor;

const int   MARCH_STEPS = 24;
const float PI          = 3.14159265358979;

const vec3 COL_CORE   = vec3(1.0, 1.0, 1.0);
const vec3 COL_INNER  = vec3(1.0, 1.0, 1.0);
const vec3 COL_MID    = vec3(1.0, 1.0, 1.0);
const vec3 COL_CORONA = vec3(1.0, 1.0, 1.0);
const vec3 COL_OUTER  = vec3(1.0, 1.0, 1.0);

struct Ray { vec3 origin; vec3 direction; };

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

vec2 sphericalUV(vec3 n) {
    return vec2(
        0.5 + atan(n.z, n.x) / (2.0 * PI),
        0.5 - asin(clamp(n.y, -1.0, 1.0)) / PI
    );
}

float sampleNoise(vec3 n) {
    vec2 uv = sphericalUV(n);
    float t1 = Time * 0.012;
    float t2 = Time * 0.028;
    float n1 = texture(Sampler0, uv + vec2(t1, t1 * 0.4)).r;
    float n2 = texture(Sampler0, uv * 2.3 + vec2(-t2 * 0.6, t2)).r;
    float n3 = texture(Sampler0, uv * 4.7 + vec2(t1 * 0.3, -t2 * 0.5)).r;
    return n1 * 0.55 + n2 * 0.30 + n3 * 0.15;
}

float limbDarkening(float mu) {
    return 0.3 + 0.7 * pow(mu, 0.5);
}

vec3 coronaColor(float h) {
    vec3 c = COL_CORE;
    c = mix(c, COL_INNER,  smoothstep(0.00, 0.15, h));
    c = mix(c, COL_MID,    smoothstep(0.10, 0.45, h));
    c = mix(c, COL_CORONA, smoothstep(0.35, 0.75, h));
    c = mix(c, COL_OUTER,  smoothstep(0.65, 1.00, h));
    return c;
}

float coronaDensity(vec3 p, float inner, float outer) {
    float h    = clamp((length(p) - inner) / max(outer - inner, 0.0001), 0.0, 1.0);
    float n    = sampleNoise(normalize(p)) * 0.5;
    float base = exp(-h * Density * 1.4) * pow(1.0 - h, 1.4);
    return max(base + n * (1.0 - h) * 0.7 - 0.02, 0.0);
}

vec3 acesToneMap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    if (!gl_FrontFacing) discard;

    Ray viewRay;
    viewRay.origin    = CameraLocalPos;
    viewRay.direction = normalize(fragLocalPos - CameraLocalPos);

    float inner = PlanetRadius;
    float outer = max(SunRadius,   inner + 0.0001);
    float bloom = max(BloomRadius, outer + 0.0001);

    vec2 coreHit  = raySphere(viewRay, inner);
    vec2 outerHit = raySphere(viewRay, outer);
    vec2 bloomHit = raySphere(viewRay, bloom);

    if (bloomHit.y < 0.0) discard;

    vec3  finalColor = vec3(0.0);
    float finalAlpha = 0.0;
    bool  isCore     = false;

    // BLOOM radiale analitico
    if (outerHit.x < 0.0) {
            float proj = dot(viewRay.origin, viewRay.direction);
            if (proj < 0.0) {
                float closestDist = length(viewRay.origin - viewRay.direction * proj);
                float h           = clamp(closestDist / bloom, 0.0, 1.0);
                float glow        = pow(1.0 - h, 5.0) * 1.5;
                vec3  glowC       = mix(COL_INNER, COL_CORONA, h * h) * SunTint;
                finalColor       += glowC * glow;
                finalAlpha        = max(finalAlpha, glow * 0.92);
            }
        }

    // CORONA volumetrica
    if (outerHit.y >= 0.0) {
        float tStart = outerHit.x;
        float tEnd   = (coreHit.x >= 0.0) ? min(outerHit.y, coreHit.x) : outerHit.y;
        float shellThickness = max(outer - inner, 0.0001);

        if (tEnd > tStart) {
            float stepSize    = (tEnd - tStart) / float(MARCH_STEPS);
            float normalizedStep = stepSize / shellThickness;
            float transmit    = 1.0;
            vec3  accumulated = vec3(0.0);

            for (int i = 0; i < MARCH_STEPS; i++) {
                float t = tStart + (float(i) + 0.5) * stepSize;
                vec3  p = viewRay.origin + viewRay.direction * t;
                float h = clamp((length(p) - inner) / max(outer - inner, 0.0001), 0.0, 1.0);
                float d = coronaDensity(p, inner, outer);

                accumulated += coronaColor(h) * d * normalizedStep * ScatteringStrength * 24.0 * transmit;
                transmit    *= exp(-d * normalizedStep * ScatteringStrength * 10.0);
                if (transmit < 0.001) break;
            }

            accumulated  *= SunTint;
            float coronaA = 1.0 - transmit;
            finalColor    = finalColor * (1.0 - coronaA) + accumulated;
            finalAlpha    = max(finalAlpha, coronaA);
        }
    }

    // CORE sfera solida
    if (coreHit.x >= 0.0) {
        vec3  hitPos = viewRay.origin + viewRay.direction * coreHit.x;
        vec3  normal = normalize(hitPos);
        float mu     = max(dot(-viewRay.direction, normal), 0.0);
        float ld     = limbDarkening(mu);

        float noise  = sampleNoise(normal);
        float gran   = smoothstep(0.35, 0.65, noise);

        vec3 coreC   = mix(COL_INNER, COL_CORE, ld);
        coreC        = mix(coreC, COL_INNER * 0.75, gran * (1.0 - ld) * 0.35);
        coreC       *= SunTint;

        float bright = dot(coreC, vec3(0.299, 0.587, 0.114));
        coreC        = mix(coreC, vec3(1.0), smoothstep(0.65, 1.0, bright) * 0.7);

        finalColor   = coreC;
        finalAlpha   = 1.0;
        isCore       = true;
    }

    if (finalAlpha < 0.001) discard;

    vec3 outColor;
    if (isCore) {
        outColor  = clamp(finalColor * 1.3, 0.0, 1.0);
        float lum = dot(outColor, vec3(0.299, 0.587, 0.114));
        outColor  = mix(outColor, vec3(1.0), smoothstep(0.60, 0.95, lum));
    } else {
        outColor = acesToneMap(finalColor * 2.0);
    }

    fragColor = vec4(outColor, clamp(finalAlpha, 0.0, 1.0));
}
