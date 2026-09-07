#version 150

uniform float PlanetRadius;
uniform vec3 LightDirection;
uniform vec3 CameraLocalPos;
uniform float TerminatorSoftness;
uniform float UseNightTexture;
uniform float EmissiveStrength;

uniform float Time;
uniform float CloudsEnabled;
uniform float CloudHeight;
uniform float CloudCoverage;
uniform float CloudWindSpeed;
uniform vec4 CloudColor;
uniform float CloudNoiseScale;
uniform float SurfaceRotation;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform int ShadowPlanetCount;
uniform vec4 ShadowPlanet0;
uniform vec4 ShadowPlanet1;
uniform vec4 ShadowPlanet2;
uniform vec4 ShadowPlanet3;

in vec3 fragLocalPos;

out vec4 fragColor;

const float NIGHT_BRIGHTNESS    = 0.10;
const vec3  NIGHT_COLOR_TINT    = vec3(0.55, 0.65, 1.00);
const float NIGHT_TINT_STRENGTH = 0.35;
const float SHADOW_SMOOTHNESS   = 0.15;

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
    float u = 0.5 - atan(n.z, n.x) / (2.0 * 3.14159265358979);
    float v = 0.5 - asin(clamp(n.y, -1.0, 1.0)) / 3.14159265358979;
    return vec2(u, v);
}

vec3 rotateY(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * v.x + s * v.z, v.y, -s * v.x + c * v.z);
}

vec3 rotateX(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(v.x, c * v.y - s * v.z, s * v.y + c * v.z);
}

vec3 rotateZ(vec3 v, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(c * v.x - s * v.y, s * v.x + c * v.y, v.z);
}

float sampleTexValue(sampler2D samp, vec2 uv) {
    vec4 c = texture(samp, uv);
    return (c.a > 0.0 && c.a < 1.0) ? c.a : max(c.r, max(c.g, c.b));
}

float sampleTriplanar(sampler2D samp, vec3 p, float scale) {
    vec3 w = pow(abs(p), vec3(4.0));
    w /= (w.x + w.y + w.z);

    float nx = sampleTexValue(samp, p.yz * scale);
    float ny = sampleTexValue(samp, p.zx * scale);
    float nz = sampleTexValue(samp, p.xy * scale);

    return nx * w.x + ny * w.y + nz * w.z;
}

float sampleCloudNoise(vec3 n, float time, float speed, float coverage) {
    if (coverage <= 0.001) return 0.0;

    vec3 rotN = (SurfaceRotation != 0.0) ? rotateY(n, -SurfaceRotation) : n;
    float wind = time * (clamp(speed, -1.0, 1.0) * 0.0050);
    float sc = max(CloudNoiseScale, 0.001);

    vec3 p1 = rotateY(rotN, wind * 0.5);
    float noise1 = sampleTriplanar(Sampler2, p1, 1.25 * sc);

    vec3 p2 = rotateY(rotN, -wind * 0.85);
    p2 = rotateX(p2, 0.35);
    float noise2 = sampleTriplanar(Sampler2, p2, 2.6 * sc);

    vec3 p3 = rotateY(rotN, wind * 1.3);
    p3 = rotateZ(p3, -0.25);
    float noise3 = sampleTriplanar(Sampler2, p3, 5.2 * sc);

    float combined = noise1 * 0.52 + noise2 * 0.32 + noise3 * 0.16;

    float threshold = 1.0 - clamp(coverage, 0.0, 1.0);
    float edgeSoftness = 0.18;
    float cloudDensity = smoothstep(threshold - edgeSoftness * 0.5, threshold + edgeSoftness * 0.5, combined);

    return clamp(cloudDensity, 0.0, 1.0);
}

float calculateEclipseShadow(vec3 hitPos, vec3 sunDir) {
    if (ShadowPlanetCount <= 0) return 1.0;

    vec4 shadowPlanets[4];
    shadowPlanets[0] = ShadowPlanet0;
    shadowPlanets[1] = ShadowPlanet1;
    shadowPlanets[2] = ShadowPlanet2;
    shadowPlanets[3] = ShadowPlanet3;

    float totalShadow = 1.0;
    for (int i = 0; i < 4; ++i) {
        if (i >= ShadowPlanetCount) break;
        vec3 center = shadowPlanets[i].xyz;
        float radius = shadowPlanets[i].w;

        vec3 V = center - hitPos;
        float t = dot(V, sunDir);
        if (t > 0.0) {
            float distSq = dot(V, V) - t * t;
            float penumbra = max(radius * 0.15, t * 0.015);
            float shadowOuter = radius + penumbra;
            float shadowOuterSq = shadowOuter * shadowOuter;

            if (distSq < shadowOuterSq) {
                float dist = sqrt(max(distSq, 0.0));
                float shadowInner = max(radius - penumbra, 0.0);
                float s = smoothstep(shadowInner, shadowOuter, dist);
                totalShadow = min(totalShadow, s);
            }
        }
    }
    return totalShadow;
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

    vec3 surfNormal = (SurfaceRotation != 0.0) ? rotateY(normal, -SurfaceRotation) : normal;
    vec2 uv = sphericalUV(surfNormal);

    vec3 sunDir = normalize(LightDirection);
    float NdotL = dot(normal, sunDir);

    float softness = max(TerminatorSoftness, SHADOW_SMOOTHNESS);
    float dayBlend = smoothstep(-softness, softness, NdotL);

    float eclipseShadow = calculateEclipseShadow(hitPos, sunDir);
    float effectiveDayBlend = dayBlend * eclipseShadow;

    float groundCloudShadowFactor = 1.0;
    if (CloudsEnabled > 0.5) {
        vec3 shadowSamplePos = normalize(hitPos + sunDir * (PlanetRadius * max(CloudHeight, 0.01) * 2.0));
        float cloudShadowDensity = sampleCloudNoise(shadowSamplePos, Time, CloudWindSpeed, CloudCoverage);
        groundCloudShadowFactor = 1.0 - cloudShadowDensity * 0.45 * effectiveDayBlend;
    }

    vec4 dayColor = texture(Sampler0, uv);
    vec3 finalSurface;
    float surfaceAlpha = dayColor.a;

    if (UseNightTexture > 0.5) {
        vec4 nightColor = texture(Sampler1, uv);
        float diffuse = max(NdotL, 0.0);
        vec3 litDayColor = dayColor.rgb * (0.35 + 0.65 * diffuse) * groundCloudShadowFactor;
        vec3 blendedColor = mix(nightColor.rgb, litDayColor, effectiveDayBlend);
        finalSurface = mix(blendedColor, dayColor.rgb, clamp(EmissiveStrength, 0.0, 1.0));
    } else {
        vec3 tintedNight = dayColor.rgb * mix(vec3(1.0), NIGHT_COLOR_TINT, NIGHT_TINT_STRENGTH) * NIGHT_BRIGHTNESS;
        vec4 nightColor = vec4(tintedNight, dayColor.a);

        vec4 surfaceColor = mix(nightColor, dayColor, effectiveDayBlend);
        float diffuse = max(NdotL, 0.0) * eclipseShadow;
        float lighting = mix(NIGHT_BRIGHTNESS, 1.0, effectiveDayBlend) * (1.0 - (1.0 - diffuse) * (1.0 - effectiveDayBlend) * 0.4) * groundCloudShadowFactor;

        finalSurface = surfaceColor.rgb * max(lighting, max(EmissiveStrength, 0.0));
        surfaceAlpha = surfaceColor.a;
    }

    if (CloudsEnabled > 0.5) {
        float cloudRadius = PlanetRadius * (1.0 + max(CloudHeight, 0.0));
        vec2 cloudHits = raySphereIntersect(viewRay, cloudRadius);
        vec3 cloudHitPos = (cloudHits.x >= 0.0) ? (viewRay.origin + viewRay.direction * cloudHits.x) : hitPos;
        vec3 cloudNormal = normalize(cloudHitPos);

        float cloudDensity = sampleCloudNoise(cloudNormal, Time, CloudWindSpeed, CloudCoverage);

        if (cloudDensity > 0.001) {

            float eps = 0.02;
            vec3 t1 = normalize(cross(cloudNormal, vec3(0.0, 1.0, 0.0) + vec3(0.001)));
            vec3 t2 = cross(cloudNormal, t1);

            float dR = sampleCloudNoise(normalize(cloudNormal + t1 * eps), Time, CloudWindSpeed, CloudCoverage);
            float dL = sampleCloudNoise(normalize(cloudNormal - t1 * eps), Time, CloudWindSpeed, CloudCoverage);
            float dT = sampleCloudNoise(normalize(cloudNormal + t2 * eps), Time, CloudWindSpeed, CloudCoverage);
            float dB = sampleCloudNoise(normalize(cloudNormal - t2 * eps), Time, CloudWindSpeed, CloudCoverage);

            float bumpiness = 2.0;
            vec3 perturbedNormal = normalize(cloudNormal + (t1 * (dL - dR) + t2 * (dB - dT)) * bumpiness);

            float wrap = 0.4;
            float rawNdotL = dot(perturbedNormal, sunDir);
            float cloudNdotL = max(0.0, (rawNdotL + wrap) / (1.0 + wrap));

            vec3 viewDir = -viewRay.direction;
            float viewDotSun = max(0.0, dot(viewDir, sunDir));
            float sss = pow(viewDotSun, 3.0) * (1.0 - cloudDensity * 0.5);

            vec3 shadowSamplePos = normalize(cloudNormal + sunDir * 0.05);
            float densityTowardsSun = sampleCloudNoise(shadowSamplePos, Time, CloudWindSpeed, CloudCoverage);
            float opticalDepth = max(0.0, densityTowardsSun - cloudDensity * 0.1);
            float shadow = mix(0.65, 1.0, exp(-opticalDepth * 1.5)) * eclipseShadow;

            float cloudDayBlend = smoothstep(-softness, softness, dot(cloudNormal, sunDir)) * eclipseShadow;
            float diffuse = cloudNdotL * shadow;
            float finalLighting = clamp((0.35 + 0.65 * diffuse) + sss * 0.8, 0.35, 1.2);

            vec3 cloudLitColor = CloudColor.rgb * finalLighting;
            vec3 cloudNightColor = CloudColor.rgb * mix(vec3(1.0), NIGHT_COLOR_TINT, NIGHT_TINT_STRENGTH) * (NIGHT_BRIGHTNESS * 0.85);

            vec3 cloudColorBlended = mix(cloudNightColor, cloudLitColor, cloudDayBlend);
            cloudColorBlended = mix(cloudColorBlended, CloudColor.rgb, clamp(EmissiveStrength, 0.0, 1.0));

            float cloudFinalAlpha = cloudDensity * clamp(CloudColor.a, 0.0, 1.0);
            finalSurface = mix(finalSurface, cloudColorBlended, cloudFinalAlpha);
        }
    }

    fragColor = vec4(finalSurface, surfaceAlpha);
}