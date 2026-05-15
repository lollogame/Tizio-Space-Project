#version 150

uniform float PlanetRadius;
uniform vec3 LightDirection;
uniform vec3 CameraLocalPos;
uniform float TerminatorSoftness;
uniform float UseNightTexture;
uniform float EmissiveStrength;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec3 fragLocalPos;

out vec4 fragColor;

const float NIGHT_BRIGHTNESS    = 0.10;
const vec3  NIGHT_COLOR_TINT    = vec3(0.55, 0.65, 1.00);
const float NIGHT_TINT_STRENGTH = 0.35;
const float SHADOW_SMOOTHNESS   = 0.40;

struct Ray {
    vec3 origin;
    vec3 direction;
};

vec2 raySphereIntersect(Ray ray, float radius) {
    float a = dot(ray.direction, ray.direction);
    float b = 2.0 * dot(ray.origin, ray.direction);
    float c = dot(ray.origin, ray.origin) - radius * radius;
    float disc = b * b - 4.0 * a * c;
    if (disc < 0.0) return vec2(-1.0);
    vec2 t = (vec2(-1.0, 1.0) * sqrt(disc) - b) / (2.0 * a);
    if (t.y < 0.0) return vec2(-1.0);
    return max(t, 0.0);
}

vec2 sphericalUV(vec3 n) {
    float u = 0.5 + atan(n.z, n.x) / (2.0 * 3.14159265358979);
    float v = 0.5 - asin(clamp(n.y, -1.0, 1.0)) / 3.14159265358979;
    return vec2(u, v);
}

void main() {
    if (!gl_FrontFacing) discard;

    Ray viewRay;
    viewRay.origin = CameraLocalPos;
    viewRay.direction = normalize(fragLocalPos - CameraLocalPos);

    vec2 hits = raySphereIntersect(viewRay, PlanetRadius);
    if (hits.x < 0.0) discard;
    if (length(CameraLocalPos) < PlanetRadius) discard;

    vec3 hitPos = viewRay.origin + viewRay.direction * hits.x;
    vec3 normal = normalize(hitPos);

    vec2 uv = sphericalUV(normal);

    vec3 sunDir = normalize(LightDirection);
    float NdotL = dot(normal, sunDir);

    float softness = max(TerminatorSoftness, SHADOW_SMOOTHNESS);
    float dayBlend = smoothstep(-softness, softness, NdotL);

    vec4 dayColor = texture(Sampler0, uv);

    vec4 nightColor;
    if (UseNightTexture > 0.5) {
        nightColor = texture(Sampler1, uv);
    } else {
        vec3 tintedNight = dayColor.rgb * mix(vec3(1.0), NIGHT_COLOR_TINT, NIGHT_TINT_STRENGTH) * NIGHT_BRIGHTNESS;
        nightColor = vec4(tintedNight, dayColor.a);
    }

    vec4 surfaceColor = mix(nightColor, dayColor, dayBlend);

    float diffuse = max(NdotL, 0.0);
    float lighting = mix(1.0, 1.0, dayBlend) * (1.0 - (1.0 - diffuse) * (1.0 - dayBlend) * 0.4);

    vec3 finalColor = surfaceColor.rgb * max(lighting, max(EmissiveStrength, 0.0));
    fragColor = vec4(finalColor, surfaceColor.a);
}