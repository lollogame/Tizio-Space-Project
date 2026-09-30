#version 150

const int MAX_SUNS = 4;

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform vec2 OutSize;

uniform mat4 SunColorMat;
uniform vec4 SunVisibility;
uniform vec4 SunScale;
uniform vec4 SunIgnoreDepth;

in vec2 texCoord;
flat in vec2 vSunScreenPos[MAX_SUNS];
flat in float vSunDepth[MAX_SUNS];
flat in float vSunOnScreen[MAX_SUNS];

out vec4 fragColor;

const vec2 OCCLUSION_OFFSETS[13] = vec2[](
    vec2( 0.0,    0.0),
    vec2( 1.0,    0.0), vec2(-1.0,    0.0), vec2( 0.0,    1.0), vec2( 0.0,   -1.0),
    vec2( 0.707,  0.707), vec2(-0.707,  0.707), vec2( 0.707, -0.707), vec2(-0.707, -0.707),
    vec2( 1.5,    0.0), vec2(-1.5,    0.0), vec2( 0.0,    1.5), vec2( 0.0,   -1.5)
);

float sdHexagon(vec2 p, float r) {
    const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
    p = abs(p);
    p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
    p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    return length(p) * sign(p.y);
}

vec3 drawHexagonGhost(vec2 coord, vec2 offset, float size, float borderSoftness) {
    vec2 p = coord - offset;

    float distG = sdHexagon(p, size);
    vec3 dists = distG + vec3(-size * 0.01, 0.0, size * 0.01);

    return smoothstep(vec3(borderSoftness), vec3(-borderSoftness), dists);
}

vec3 computeLensFlare(vec2 viewCoord, vec2 sunPos) {
    const float flareBrightness = 1.0;

    float viewDist = length(viewCoord);
    vec2 warpedCoord = viewCoord * viewDist;

    vec2 posHaloR = warpedCoord + 0.80 * sunPos;
    vec2 posHaloG = warpedCoord + 0.85 * sunPos;
    vec2 posHaloB = warpedCoord + 0.90 * sunPos;
    vec3 h2 = vec3(dot(posHaloR, posHaloR), dot(posHaloG, posHaloG), dot(posHaloB, posHaloB));
    vec3 halo = max(1.0 / (1.0 + 32.0 * h2), 0.0) * vec3(0.10, 0.08, 0.06);

    vec2 blendedCoord1 = mix(viewCoord, warpedCoord, -0.5);
    vec2 pR1 = blendedCoord1 + 0.40 * sunPos;
    vec2 pG1 = blendedCoord1 + 0.45 * sunPos;
    vec2 pB1 = blendedCoord1 + 0.50 * sunPos;
    vec3 d2_1 = vec3(dot(pR1, pR1), dot(pG1, pG1), dot(pB1, pB1));
    vec3 ring = max(vec3(0.01) - pow(d2_1, vec3(1.2)), vec3(0.0)) * vec3(6.0, 5.0, 3.0);

    vec2 blendedCoord2 = mix(viewCoord, warpedCoord, -0.4);
    vec2 pR2 = blendedCoord2 + 0.20 * sunPos;
    vec2 pG2 = blendedCoord2 + 0.40 * sunPos;
    vec2 pB2 = blendedCoord2 + 0.60 * sunPos;
    vec3 d2_2 = vec3(dot(pR2, pR2), dot(pG2, pG2), dot(pB2, pB2));
    vec3 burst = max(vec3(0.01) - pow(d2_2, vec3(2.75)), vec3(0.0)) * 2.0;

    vec3 hexGhosts = vec3(0.0);
    hexGhosts += drawHexagonGhost(viewCoord, -sunPos * 0.50, 0.18, 0.03) * 0.04;
    hexGhosts += drawHexagonGhost(viewCoord, -sunPos * 0.25, 0.10, 0.02) * 0.06;
    hexGhosts += drawHexagonGhost(viewCoord,  sunPos * 0.15, 0.06, 0.015) * 0.08;
    hexGhosts += drawHexagonGhost(viewCoord,  sunPos * 0.35, 0.14, 0.03) * 0.03;
    hexGhosts += drawHexagonGhost(viewCoord,  sunPos * 0.70, 0.22, 0.04) * 0.02;

    vec3 accumulatedColor = max(vec3(0.0), (halo + ring + burst) * 1.3) + hexGhosts;

    return accumulatedColor * flareBrightness;
}

float computeAreaOcclusion(vec2 centerUV, float targetDepth, vec2 invAspect, float scale) {
    float sampleRadius = 0.015 * clamp(scale, 0.5, 2.0);
    float targetDepthThreshold = targetDepth - 0.005;

    float visibleSamples = 0.0;

    for (int i = 0; i < 13; i++) {
        vec2 offsetUV = (OCCLUSION_OFFSETS[i] * sampleRadius) * invAspect;
        vec2 samplePos = clamp(centerUV + offsetUV, vec2(0.001), vec2(0.999));
        float sceneDepth = texture(DepthSampler, samplePos).r;

        if (sceneDepth >= targetDepthThreshold) {
            visibleSamples += 1.0;
        }
    }

    return visibleSamples * (1.0 / 13.0);
}

vec3 accumulateSun(int index, vec3 color, float visibility, float scale, float ignoreDepth, vec2 aspectRatio, vec2 invAspect) {
    if (visibility <= 0.001 || vSunOnScreen[index] <= 0.5) {
        return vec3(0.0);
    }

    float occlusion = ignoreDepth > 0.5 ? 1.0 : computeAreaOcclusion(vSunScreenPos[index], vSunDepth[index], invAspect, scale);
    float effectiveVisibility = visibility * occlusion;

    if (effectiveVisibility <= 0.001) {
        return vec3(0.0);
    }

    float safeScale = max(scale, 0.4);
    vec2 viewCoord = ((texCoord - 0.5) * aspectRatio) / safeScale;
    vec2 sunPos = ((vSunScreenPos[index] - 0.5) * aspectRatio) / safeScale;

    return color * computeLensFlare(viewCoord, sunPos) * effectiveVisibility;
}

void main() {
    vec4 baseScene = texture(DiffuseSampler, texCoord);

    if (dot(SunVisibility, vec4(1.0)) <= 0.001) {
        fragColor = baseScene;
        return;
    }

    vec2 aspectRatio = vec2(OutSize.x / OutSize.y, 1.0);
    vec2 invAspect = vec2(OutSize.y / OutSize.x, 1.0);

    vec3 totalFlare = vec3(0.0);
    for (int i = 0; i < MAX_SUNS; i++) {
        totalFlare += accumulateSun(i, SunColorMat[i].rgb, SunVisibility[i], SunScale[i], SunIgnoreDepth[i], aspectRatio, invAspect);
    }

    fragColor = vec4(baseScene.rgb + totalFlare, 1.0);
}