#version 150

uniform float Time;
uniform float EffectRadius;
uniform float DiskRotationSpeed;
uniform float Intensity;
uniform vec3 CameraLocalPos;
uniform vec3 BaseColor;

uniform sampler2D Sampler0;

in vec3 fragLocalPos;

out vec4 fragColor;

#define DISK_LAYERS 5
#define MARCH_GROUPS 10
#define MARCH_SUBSTEPS 6

vec4 raymarchDisk(vec3 ray, vec3 zeroPos) {
    vec3 position = zeroPos;
    float lengthPos = length(position.xz);
    float rayY = max(abs(ray.y), 0.0001);
    float dist = min(1.0, lengthPos * (1.0 / EffectRadius) * 0.5) * EffectRadius * 0.4 * (1.0 / float(DISK_LAYERS)) / rayY;

    position += dist * float(DISK_LAYERS) * ray * 0.5;

    vec2 deltaPos;
    deltaPos.x = -zeroPos.z * 0.01 + zeroPos.x;
    deltaPos.y = zeroPos.x * 0.01 + zeroPos.z;
    deltaPos = normalize(deltaPos - zeroPos.xz);

    float parallel = dot(ray.xz, deltaPos);
    parallel /= sqrt(max(lengthPos, 0.0001));
    parallel *= 0.5;
    float redShift = parallel + 0.3;
    redShift *= redShift;
    redShift = clamp(redShift, 0.0, 1.0);

    vec3 hotColor = mix(BaseColor * 1.35, vec3(1.0), 0.18);
    vec3 darkColor = BaseColor * 0.095;
    float disMix = clamp((lengthPos - EffectRadius * 2.0) * (1.0 / EffectRadius) * 0.24, 0.0, 1.0);
    vec3 insideCol = mix(hotColor, darkColor, disMix);
    vec3 shiftLow = BaseColor * vec3(0.45, 0.28, 0.18) + vec3(0.04);
    vec3 shiftHigh = mix(BaseColor * vec3(1.6, 1.8, 2.2), vec3(1.0, 1.1, 1.3), 0.22);
    insideCol *= mix(shiftLow, shiftHigh, redShift);
    insideCol *= 1.0 * Intensity;

    redShift += 0.12;
    redShift *= redShift;

    vec4 accumulated = vec4(0.0);

    for (int i = 0; i < DISK_LAYERS; i++) {
        position -= dist * ray;

        float fi = float(i);
        float intensity = clamp(1.0 - abs((fi - 0.8) * (1.0 / float(DISK_LAYERS)) * 2.0), 0.0, 1.0);
        float layerLength = length(position.xz);
        float distMult = 1.15;
        distMult *= clamp((layerLength - EffectRadius * 0.75) * (1.0 / EffectRadius) * 1.5, 0.0, 1.0);
        distMult *= clamp((EffectRadius * 10.0 - layerLength) * (1.0 / EffectRadius) * 0.20, 0.0, 1.0);
        distMult *= distMult;

        float u = layerLength + Time * EffectRadius * 0.21 + intensity * EffectRadius * 0.6;

        vec2 xy;
        float rot = mod(Time * DiskRotationSpeed, 8192.0);
        xy.x = -position.z * sin(rot) + position.x * cos(rot);
        xy.y = position.x * sin(rot) + position.z * cos(rot);

        float x = abs(xy.x / max(abs(xy.y), 0.0001));
        float angle = 0.02 * atan(x);

        const float noiseFrequency = 70.0;
        vec2 noiseUV = vec2(angle, u * (2.0 / EffectRadius) * 0.035);

        float n1 = textureLod(Sampler0, noiseUV * noiseFrequency * 0.05, 0.0).r;
        float n2 = textureLod(Sampler0, noiseUV * noiseFrequency * 0.10, 0.0).r;
        float noise = n1 * 0.66 + 0.33 * n2;
        // ----------------------------------------------

        //TODO: Ridurre il raggio dell'anello bianco nel bucoh

        float extraWidth = noise * (1.0 - clamp(fi * (1.0 / float(DISK_LAYERS)) * 2.0 - 1.0, 0.0, 1.0));
        float alpha = clamp(noise * (intensity + extraWidth) * ((1.0 / EffectRadius) * 10.0 + 0.01) * dist * distMult, 0.0, 1.0);
        vec3 col = 2.0 * mix(vec3(0.3, 0.2, 0.15) * insideCol, insideCol, min(1.0, intensity * 2.0));

        accumulated = clamp(vec4(col * alpha + accumulated.rgb * (1.0 - alpha), accumulated.a * (1.0 - alpha) + alpha), vec4(0.0), vec4(0.95));

        float normalizedLength = max(layerLength * (1.0 / EffectRadius), 0.001);
        accumulated.rgb += redShift * (intensity + 0.5) * (1.0 / float(DISK_LAYERS)) * 100.0 * distMult / (normalizedLength * normalizedLength);
    }

    accumulated.rgb = clamp(accumulated.rgb - 0.005, 0.0, 1.0);
    return accumulated;
}

void main() {

    if (!gl_FrontFacing) discard;

    vec3 pos = CameraLocalPos;
    vec3 ray = normalize(fragLocalPos - CameraLocalPos);

    vec4 diskAccum = vec4(0.0);
    vec4 glowAccum = vec4(0.0);
    vec4 finalColor = vec4(100.0);

    for (int disk = 0; disk < MARCH_GROUPS; disk++) {
        for (int h = 0; h < MARCH_SUBSTEPS; h++) {
            float dotPos = dot(pos, pos);
            float invDist = inversesqrt(max(dotPos, 0.0001));
            float centerDist = dotPos * invDist;
            float rayY = max(abs(ray.y), 0.0001);
            float stepDist = 0.92 * abs(pos.y / rayY);
            float farLimit = centerDist * 0.5;
            float closeLimit = centerDist * 0.1 + 0.05 * centerDist * centerDist * (1.0 / EffectRadius);
            stepDist = min(stepDist, min(farLimit, closeLimit));

            float invDistSqr = invDist * invDist;
            float bendForce = stepDist * invDistSqr * EffectRadius * 0.9;
            ray = normalize(ray - (bendForce * invDist) * pos);
            pos += stepDist * ray;

            vec3 glowColor = mix(BaseColor * 1.2, vec3(1.0), 0.20);
            glowAccum += vec4(glowColor, 1.0) * (0.01 * stepDist * invDistSqr * invDistSqr * clamp(centerDist * 2.0 - 1.2, 0.0, 1.0)) * Intensity;
        }

        float distToCenter = length(pos);
        if (distToCenter < EffectRadius * 0.1) {
            finalColor = vec4(diskAccum.rgb * diskAccum.a + glowAccum.rgb * (1.0 - diskAccum.a), 1.0);
            break;
        } else if (abs(pos.y) <= EffectRadius * 0.002) {
            vec4 diskCol = raymarchDisk(ray, pos);
            pos.y = 0.0;
            pos += abs(EffectRadius * 0.001 / max(abs(ray.y), 0.0001)) * ray;
            diskAccum = vec4(diskCol.rgb * (1.0 - diskAccum.a) + diskAccum.rgb, diskAccum.a + diskCol.a * (1.0 - diskAccum.a));
        }
    }

    if (finalColor.r == 100.0) {
        vec3 rgb = diskAccum.rgb + glowAccum.rgb * (diskAccum.a + glowAccum.a) * 0.35;
        float alpha = clamp(max(diskAccum.a, max(glowAccum.r, max(glowAccum.g, glowAccum.b)) * 0.12), 0.0, 1.0);
        finalColor = vec4(rgb, alpha);
    }

    finalColor.rgb = clamp(finalColor.rgb, 0.0, 1.0);
    if (finalColor.a <= 0.001) {
        discard;
    }

    fragColor = finalColor;
}
