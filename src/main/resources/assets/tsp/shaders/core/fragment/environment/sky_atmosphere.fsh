#version 150

in vec3 localPos;

uniform vec4 ColorModulator;
uniform vec3 SunDir;
uniform vec3 uWavelenghts;
uniform float uCamHeight;

out vec4 fragColor;

#define PI 3.14159265359
#define INFINITY 1e9

const float PLANET_RADIUS = 6360e3;
const float ATMOSPHERE_RADIUS = 6420e3;

const float RAYLEIGH_SCALE_HEIGHT = 2994.0;
const float MIE_SCALE_HEIGHT = 1600.0;
const float MIE_G = 0.76;
const float MIE_EXTINCTION_MUL = 1.0;
const vec3 MIE_COEFFICIENT = vec3(21e-6);

const vec3 REFERENCE_WAVELENGTHS = vec3(680.0, 550.0, 440.0);
const vec3 REFERENCE_RAYLEIGH_COEFFICIENT = vec3(3.8e-6, 13.5e-6, 33.1e-6);

const uint SAMPLES = 6u;
const uint SAMPLES_LIGHT = 4u;

const float SUN_INTENSITY = 25.0;
const float EXPOSURE = 1.2;

const float HORIZON_LIFT = 0.075;
const float HORIZON_FADE_START = -1.65;
const float HORIZON_FADE_MIN = 0.0;

vec3 rayleighCoefficient() {
    return REFERENCE_RAYLEIGH_COEFFICIENT * pow(REFERENCE_WAVELENGTHS / uWavelenghts, vec3(4.0));
}

bool raySphereIntersect(const in vec3 orig, const in vec3 dir, const in float radius, out float t0, out float t1) {
    float b = dot(dir, orig);
    float c = dot(orig, orig) - (radius * radius);
    float test = b * b - c;
    if (test <= 0.0) return false;
    test = sqrt(test);
    t0 = -b - test;
    t1 = -b + test;
    if (t0 > t1) { float tmp = t0; t0 = t1; t1 = tmp; }
    return true;
}

vec3 computeIncidentLight(const in vec3 orig, const in vec3 dir, in float tmin, in float tmax, const in vec3 sunDirection, const in vec3 betaR) {
    float t0, t1;
    if (!raySphereIntersect(orig, dir, ATMOSPHERE_RADIUS, t0, t1) || t1 < 0.0) return vec3(0.0);
    if (t0 > tmin && t0 > 0.0) tmin = t0;
    if (t1 < tmax) tmax = t1;

    float segmentLength = (tmax - tmin) / float(SAMPLES);
    float tCurrent = tmin;

    vec3 sumR = vec3(0.0);
    float opticalDepthR = 0.0;
    float mu = dot(dir, sunDirection);
    float phaseR = 3.0 / (16.0 * PI) * (1.0 + mu * mu);

    vec3 sumM = vec3(0.0);
    float opticalDepthM = 0.0;
    float phaseM = 3.0 / (8.0 * PI) * ((1.0 - MIE_G * MIE_G) * (1.0 + mu * mu)) / ((2.0 + MIE_G * MIE_G) * pow(1.0 + MIE_G * MIE_G - 2.0 * MIE_G * mu, 1.5));

    for (uint i = 0u; i < SAMPLES; ++i) {

        vec3 samplePosition = orig + (tCurrent + segmentLength * 0.5) * dir;
        float height = length(samplePosition) - PLANET_RADIUS;

        float hr = exp(-height / RAYLEIGH_SCALE_HEIGHT) * segmentLength;
        opticalDepthR += hr;
        float hm = exp(-height / MIE_SCALE_HEIGHT) * segmentLength;
        opticalDepthM += hm;

        float t0Light, t1Light;
        raySphereIntersect(samplePosition, sunDirection, ATMOSPHERE_RADIUS, t0Light, t1Light);
        float segmentLengthLight = t1Light / float(SAMPLES_LIGHT);
        float tCurrentLight = 0.0;
        float opticalDepthLightR = 0.0;
        float opticalDepthLightM = 0.0;

        uint j;
        for (j = 0u; j < SAMPLES_LIGHT; ++j) {
            vec3 samplePositionLight = samplePosition + (tCurrentLight + segmentLengthLight * 0.5) * sunDirection;
            float heightLight = length(samplePositionLight) - PLANET_RADIUS;
            if (heightLight < 0.0) break;
            opticalDepthLightR += exp(-heightLight / RAYLEIGH_SCALE_HEIGHT) * segmentLengthLight;
            opticalDepthLightM += exp(-heightLight / MIE_SCALE_HEIGHT) * segmentLengthLight;
            tCurrentLight += segmentLengthLight;
        }

        if (j == SAMPLES_LIGHT) {
            vec3 tau = betaR * (opticalDepthR + opticalDepthLightR) + MIE_COEFFICIENT * MIE_EXTINCTION_MUL * (opticalDepthM + opticalDepthLightM);
            vec3 attenuation = vec3(exp(-tau.x), exp(-tau.y), exp(-tau.z));
            sumR += attenuation * hr;
            sumM += attenuation * hm;
        }
        tCurrent += segmentLength;
    }

    return (sumR * betaR * phaseR + sumM * MIE_COEFFICIENT * phaseM) * SUN_INTENSITY;
}

void main() {

    vec3 dir = normalize(localPos);
    vec3 sunDirection = normalize(SunDir);
    vec3 betaR = rayleighCoefficient();

    vec3 rayDir = dir;
    rayDir.y += HORIZON_LIFT;
    rayDir = normalize(rayDir);

    vec3 camPos = vec3(0.0, PLANET_RADIUS + 0.0, 0.0);

    float t0, t1, tMax = INFINITY;
    if (raySphereIntersect(camPos, rayDir, PLANET_RADIUS, t0, t1) && t0 > 0.0) {
        tMax = t0;
    }

    vec3 scatter = computeIncidentLight(camPos, rayDir, 0.0, tMax, sunDirection, betaR);

    vec3 color = vec3(1.0) - exp(-scatter * EXPOSURE);
    color = pow(color, vec3(1.0 / 2.2));

    float depthFade = smoothstep(HORIZON_FADE_START, HORIZON_LIFT, dir.y);
    color *= mix(HORIZON_FADE_MIN, 1.0, depthFade);

    float sunHeight = clamp(sunDirection.y, -1.0, 1.0);
    float dayFactor = smoothstep(-0.15, 0.05, sunHeight);

    fragColor = vec4(color, dayFactor) * ColorModulator;
}