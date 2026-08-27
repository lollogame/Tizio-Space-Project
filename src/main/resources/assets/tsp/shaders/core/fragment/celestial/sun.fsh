#version 150

uniform float PlanetRadius;
uniform float BloomRadius;
uniform float Density;
uniform float ScatteringStrength;
uniform float Time;
uniform vec3  SunTint;
uniform vec3  CameraLocalPos;
uniform sampler2D Sampler0;

in  vec3 fragLocalPos;
out vec4 fragColor;

const int   MARCH_STEPS = 3;
const float PI          = 3.14159265358979;
const float SUN_RADIUS_SCALE = 9.35;

const vec3 COL_CORE  = vec3(1.00, 0.95, 0.85);
const vec3 COL_MID   = vec3(1.00, 0.55, 0.20);
const vec3 COL_OUTER = vec3(0.55, 0.16, 0.08);

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
    float t = Time * 0.02;
    float n1 = texture(Sampler0, uv + vec2(t, t * 0.4)).r;
    float n2 = texture(Sampler0, uv * 2.5 - vec2(t * 0.5, t)).r;
    return n1 * 0.65 + n2 * 0.35;
}

float limbDarkening(float mu) {
    return 0.3 + 0.7 * sqrt(mu);
}

vec3 coronaColor(float h) {
    vec3 c = mix(COL_CORE, COL_MID, smoothstep(0.0, 0.7, h));
    return mix(c, COL_OUTER, smoothstep(0.3, 1.0, h));
}

float coronaDensity(vec3 p, float inner, float outer) {
    float h    = clamp((length(p) - inner) / max(outer - inner, 0.0001), 0.0, 1.0);
    float n    = sampleNoise(normalize(p));
    float base = exp(-h * Density * 1.3) * pow(1.0 - h, 1.2);
    float rays = base * (0.55 + n * 0.9);
    return max(rays, base * 0.3);
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
    float outer = max(inner * SUN_RADIUS_SCALE, inner + 0.0001);
    float bloom = max(BloomRadius, outer + 0.0001);

    vec2 coreHit  = raySphere(viewRay, inner);
    vec2 outerHit = raySphere(viewRay, outer);
    vec2 bloomHit = raySphere(viewRay, bloom);

    if (bloomHit.y < 0.0) discard;

    vec3  finalColor = vec3(0.0);
    float finalAlpha = 0.0;
    bool  isCore     = false;

    if (outerHit.x < 0.0) {
        float proj = dot(viewRay.origin, viewRay.direction);
        if (proj < 0.0) {
            float closestDist = length(viewRay.origin - viewRay.direction * proj);
            float h    = clamp(closestDist / bloom, 0.0, 1.0);
            float glow = pow(1.0 - h, 5.0) * 1.5;
            finalColor += mix(COL_CORE, COL_OUTER, h * h) * SunTint * glow;
            finalAlpha  = max(finalAlpha, glow * 0.92);
        }
    }

    if (outerHit.y >= 0.0) {
        float tStart = outerHit.x;
        float tEnd   = (coreHit.x >= 0.0) ? min(outerHit.y, coreHit.x) : outerHit.y;
        float shellThickness = max(outer - inner, 0.0001);

        if (tEnd > tStart) {
            float stepSize       = (tEnd - tStart) / float(MARCH_STEPS);
            float normalizedStep = stepSize / shellThickness;
            float transmit       = 1.0;
            vec3  accumulated    = vec3(0.0);

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

    if (coreHit.x >= 0.0) {
        vec3  hitPos = viewRay.origin + viewRay.direction * coreHit.x;
        vec3  normal = normalize(hitPos);
        float mu     = max(dot(-viewRay.direction, normal), 0.0);
        float ld     = limbDarkening(mu);

        float gran = smoothstep(0.35, 0.65, sampleNoise(normal));

        vec3 coreC = mix(COL_MID, COL_CORE, ld);
        coreC      = mix(coreC, COL_MID * 0.75, gran * ld * 0.3);
        float rim  = 1.0 - smoothstep(0.0, 0.35, mu);
        coreC      += COL_MID * rim * rim * 0.9;
        coreC      *= SunTint;

        finalColor = coreC;
        finalAlpha = 1.0;
        isCore     = true;
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
