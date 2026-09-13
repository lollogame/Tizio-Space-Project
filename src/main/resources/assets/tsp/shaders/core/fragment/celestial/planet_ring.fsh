#version 150

uniform float PlanetRadius;
uniform float RingInnerRadius;
uniform float RingOuterRadius;
uniform vec3 LightDirection;
uniform int LightCount;
uniform vec3 LightDirection0;
uniform vec3 LightDirection1;
uniform vec3 LightDirection2;
uniform vec3 LightDirection3;
uniform vec3 CameraLocalPos;
uniform sampler2D Sampler0;

in vec3 fragLocalPos;
out vec4 fragColor;

const float ShadowSoftness = 0.015;
const float AmbientLight   = 0.10;
const float RingAmbient    = 0.55;

const float DISSOLVE_WIDTH_RATIO = 0.25;
const float DISSOLVE_SOFTNESS    = 0.5;

struct Ray {
    vec3 origin;
    vec3 direction;
};

float raySphereIntersectNearest(Ray ray, float radius) {
    float a = dot(ray.direction, ray.direction);
    float b = 2.0 * dot(ray.origin, ray.direction);
    float c = dot(ray.origin, ray.origin) - radius * radius;
    float disc = b * b - 4.0 * a * c;
    if (disc < 0.0) return -1.0;
    float sqrtDisc = sqrt(disc);
    float t0 = (-b - sqrtDisc) / (2.0 * a);
    float t1 = (-b + sqrtDisc) / (2.0 * a);
    if (t1 < 0.0) return -1.0;
    return (t0 >= 0.0) ? t0 : t1;
}

float proximityFade(vec3 p, vec3 cameraPos) {
    float ringWidth = max(RingOuterRadius - RingInnerRadius, 0.0001);

    float fadeRadius  = ringWidth * DISSOLVE_WIDTH_RATIO;
    float innerRadius = fadeRadius * (1.0 - DISSOLVE_SOFTNESS);
    float dist = length(p - cameraPos);

    return smoothstep(innerRadius, fadeRadius, dist);
}

void main() {

    if (!gl_FrontFacing) discard;


    Ray viewRay;
    viewRay.origin    = CameraLocalPos;
    viewRay.direction = normalize(fragLocalPos - CameraLocalPos);

    vec3 ringNormal = vec3(0.0, 1.0, 0.0);
    float denom = dot(viewRay.direction, ringNormal);
    if (abs(denom) < 0.00001) discard;

    float t = -dot(viewRay.origin, ringNormal) / denom;
    if (t < 0.0) discard;

    if (dot(CameraLocalPos, CameraLocalPos) >= PlanetRadius * PlanetRadius) {
        float sphereHit = raySphereIntersectNearest(viewRay, PlanetRadius);
        if (sphereHit >= 0.0 && sphereHit < t - max(PlanetRadius * 0.0005, 0.0001)) {
            discard;
        }
    }

    vec3 hitPos = viewRay.origin + viewRay.direction * t;
    float r = length(hitPos);

    if (r < RingInnerRadius || r > RingOuterRadius) discard;

    float v = (r - RingInnerRadius) / max(RingOuterRadius - RingInnerRadius, 0.0001);
    float angle = atan(hitPos.z, hitPos.x) / (2.0 * 3.14159265358979) + 0.5;
    vec2 uv = vec2(fract(angle), v);

    const int MAX_LIGHTS = 4;
    vec3 sunDirs[MAX_LIGHTS];
    sunDirs[0] = normalize(LightDirection0);
    sunDirs[1] = normalize(LightDirection1);
    sunDirs[2] = normalize(LightDirection2);
    sunDirs[3] = normalize(LightDirection3);

    float softness = max(ShadowSoftness, 0.001) * PlanetRadius;
    float directAccum = 0.0;

    if (LightCount <= 0) {
        vec3 sunDir = normalize(LightDirection);
        float tCA = -dot(hitPos, sunDir);
        float s = 1.0;
        if (tCA > 0.0) {
            vec3 closest = hitPos + sunDir * tCA;
            float dist = length(closest);
            s = smoothstep(PlanetRadius - softness, PlanetRadius + softness, dist);
        }
        directAccum = s;
    } else {
        for (int i = 0; i < MAX_LIGHTS; ++i) {
            if (i >= LightCount) break;
            vec3 sunDir = sunDirs[i];
            float tCA = -dot(hitPos, sunDir);
            float s = 1.0;
            if (tCA > 0.0) {
                vec3 closest = hitPos + sunDir * tCA;
                float dist = length(closest);
                s = smoothstep(PlanetRadius - softness, PlanetRadius + softness, dist);
            }
            directAccum += s;
        }
        directAccum /= float(LightCount);
    }

    vec4 ringColor = texture(Sampler0, uv);
    if (ringColor.a < 0.001) discard;

    float lighting = mix(AmbientLight, RingAmbient, directAccum);
    float fade = proximityFade(hitPos, viewRay.origin);
    float alpha = ringColor.a * fade;
    if (alpha < 0.001) discard;

    fragColor = vec4(ringColor.rgb * lighting, alpha);
}