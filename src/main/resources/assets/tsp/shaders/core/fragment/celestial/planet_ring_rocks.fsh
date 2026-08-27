#version 150

uniform float PlanetRadius;
uniform vec3  CameraLocalPos;
uniform vec3  LightDirection;
uniform sampler2D Sampler0;
uniform float OpaquePass;

in vec3  fragLocalPos;
in vec2  fragUV;
in float fragDistAlpha;

out vec4 fragColor;

const float ShadowSoftness   = 0.015;
const float AmbientLight     = 0.25;
const float OPAQUE_THRESHOLD = 0.99;

float raySphereNearestNormalized(vec3 origin, vec3 dir, float radiusSq) {
    float b = dot(origin, dir);
    float c = dot(origin, origin) - radiusSq;
    float disc = b * b - c;
    if (disc < 0.0) return -1.0;
    float sqrtDisc = sqrt(disc);
    float t0 = -b - sqrtDisc;
    float t1 = -b + sqrtDisc;
    if (t1 < 0.0) return -1.0;
    return (t0 >= 0.0) ? t0 : t1;
}

float computePlanetShadow(vec3 p, vec3 sunDir) {
    if (PlanetRadius <= 0.0) return 1.0;

    float tCA = -dot(p, sunDir);
    if (tCA <= 0.0) return 1.0;

    vec3 closest = p + sunDir * tCA;
    float dist   = length(closest);

    float soft = max(ShadowSoftness, 0.001) * PlanetRadius;
    return smoothstep(PlanetRadius - soft, PlanetRadius + soft, dist);
}

void main() {
    if (!gl_FrontFacing) discard;

    vec2 texSize    = vec2(textureSize(Sampler0, 0));
    vec2 invTexSize = 1.0 / texSize;
    vec2 nearestUV  = (floor(fragUV * texSize) + 0.5) * invTexSize;
    vec4 texColor   = texture(Sampler0, nearestUV);

    if (texColor.a < 0.1) discard;

    float finalAlpha = texColor.a * fragDistAlpha;

    if (OpaquePass > 0.5) {
        if (finalAlpha < OPAQUE_THRESHOLD) discard;
        finalAlpha = 1.0;
    } else {
        if (finalAlpha >= OPAQUE_THRESHOLD || finalAlpha < 0.01) discard;
    }

    if (PlanetRadius > 0.0) {
        float planetRadiusSq = PlanetRadius * PlanetRadius;
        if (dot(CameraLocalPos, CameraLocalPos) < planetRadiusSq) discard;

        vec3  toFrag       = fragLocalPos - CameraLocalPos;
        float distToFragSq = dot(toFrag, toFrag);
        float distToFrag    = sqrt(distToFragSq);
        vec3  rayDir        = toFrag / distToFrag;

        float sphereHit = raySphereNearestNormalized(CameraLocalPos, rayDir, planetRadiusSq);

        if (sphereHit >= 0.0 && sphereHit < distToFrag - max(PlanetRadius * 0.0005, 0.0001)) {
            discard;
        }
    }

    vec3 flatNormal = normalize(cross(dFdx(fragLocalPos), dFdy(fragLocalPos)));
    vec3 sunDir     = normalize(LightDirection);
    float NdotL     = max(dot(flatNormal, sunDir), 0.0);

    float shadow      = computePlanetShadow(fragLocalPos, sunDir);
    float lightFactor = mix(AmbientLight, 1.0, NdotL * shadow);

    fragColor = vec4(texColor.rgb * lightFactor, finalAlpha);
}
