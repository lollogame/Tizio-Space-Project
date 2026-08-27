#version 150

uniform float PlanetRadius;
uniform float AtmosphereRadius;
uniform float Intensity;
uniform float Exposure;
uniform float RayleighScaleHeight;
uniform float RayleighStrength;
uniform vec3 BaseColor;
uniform vec3 LightDirection;
uniform vec3 CameraLocalPos;
uniform vec3 WaveLengths;

in vec3 fragLocalPos;

out vec4 fragColor;

const int LIGHT_SAMPLES = 6;
const int VIEW_SAMPLES = 6;
const float REFERENCE_ATMOSPHERE_THICKNESS = 55.0;
const float MIN_SCALE_COMPENSATION = 0.05;
const float MAX_SCALE_COMPENSATION = 20.0;

struct Ray {
    vec3 origin;
    vec3 direction;
};

struct AtmosphereProperties {
    float planetRadius;
    float atmosphereThickness;
    vec3 waveLengths;
    float densityFalloff;
    float scatteringStrength;
    vec3 atmosphereTint;
};

vec3 getPointOnRay(Ray ray, float t) {
    return ray.origin + ray.direction * t;
}

vec2 getRaySphereIntersection(Ray ray, float radius) {
    float a = dot(ray.direction, ray.direction);
    float b = 2.0 * dot(ray.origin, ray.direction);
    float c = dot(ray.origin, ray.origin) - radius * radius;
    float discriminant = b * b - 4.0 * a * c;
    if (discriminant < 0.0) {
        return vec2(-1.0);
    }

    vec2 solutions = (vec2(-1.0, 1.0) * sqrt(discriminant) - b) / (2.0 * a);
    if (solutions.y < 0.0) {
        return vec2(-1.0);
    }

    return max(solutions, 0.0);
}

vec3 getPathInAtmosphereShell(Ray ray, float innerRadius, float outerRadius) {
    float innerHit = getRaySphereIntersection(ray, innerRadius).x;
    if (innerHit >= 0.0) {
        innerHit -= min(innerHit, 0.0) * 1000.0;
    } else {
        innerHit = outerRadius * 1000.0;
    }

    vec2 outerHits = getRaySphereIntersection(ray, outerRadius);
    if (outerHits.x < 0.0) {
        return vec3(0.0);
    }

    return ray.direction * (min(outerHits.y, innerHit) - outerHits.x) * 0.9999;
}

float getDensityAtPoint(vec3 p, float innerRadius, float outerRadius, float falloff) {
    float height = (length(p) - innerRadius) / max(outerRadius - innerRadius, 0.0001);
    return exp(-height * falloff) * max(1.0 - height, 0.0);
}

float integrateOpticalDepth(vec3 startPoint, vec3 stepVec, float innerRadius, float outerRadius, float falloff, float scaleCompensation) {
    float totalDensity = 0.0;
    float stepLength = length(stepVec) * scaleCompensation;

    for (int i = 0; i < LIGHT_SAMPLES; ++i) {
        totalDensity += getDensityAtPoint(startPoint, innerRadius, outerRadius, falloff) * stepLength;
        startPoint += stepVec;
    }

    return totalDensity;
}

vec4 calculateAtmosphereEffect(Ray viewRay, vec3 sunDirection, AtmosphereProperties props) {
    float innerRadius = props.planetRadius;
    float outerRadius = props.planetRadius + props.atmosphereThickness;
    vec3 viewPath = getPathInAtmosphereShell(viewRay, innerRadius, outerRadius);

    if (dot(viewPath, viewPath) < 0.00001) {
        return vec4(0.0);
    }

    vec3 viewStep = viewPath / float(VIEW_SAMPLES - 1);
    float scaleCompensation = clamp(
        REFERENCE_ATMOSPHERE_THICKNESS / max(props.atmosphereThickness, 0.0001),
        MIN_SCALE_COMPENSATION,
        MAX_SCALE_COMPENSATION
    );
    float viewStepLength = length(viewStep) * scaleCompensation;

    vec3 accumulatedLight = vec3(0.0);
    float viewOpticalDepth = 0.0;
    vec2 outerHits = getRaySphereIntersection(viewRay, outerRadius);
    vec3 currentPoint = getPointOnRay(viewRay, outerHits.x);
    vec3 scatterCoefficients = pow(400.0 / props.waveLengths, vec3(4.0)) * props.scatteringStrength;

    for (int i = 0; i < VIEW_SAMPLES; ++i) {
        float densityHere = getDensityAtPoint(currentPoint, innerRadius, outerRadius, props.densityFalloff) * viewStepLength;
        viewOpticalDepth += densityHere;

        Ray sunRay = Ray(currentPoint, sunDirection);
        vec2 sunRayHits = getRaySphereIntersection(sunRay, outerRadius);
        vec3 sunStep = sunDirection * (sunRayHits.y - sunRayHits.x) / float(LIGHT_SAMPLES);
        float sunOpticalDepth = integrateOpticalDepth(currentPoint, sunStep, innerRadius, outerRadius, props.densityFalloff, scaleCompensation);

        vec3 totalOpticalDepth = (sunOpticalDepth + viewOpticalDepth) * scatterCoefficients;
        vec3 transmittance = exp(-totalOpticalDepth);
        accumulatedLight += densityHere * transmittance;

        currentPoint += viewStep;
    }

    vec3 finalScatteredLight = accumulatedLight * scatterCoefficients * 1.6 * props.atmosphereTint * (Intensity * Exposure);
    vec4 finalColor = vec4(finalScatteredLight, max(finalScatteredLight.x, max(finalScatteredLight.y, finalScatteredLight.z)));

    if (finalColor.a < 0.0001) {
        return vec4(0.0);
    }

    finalColor.rgb /= finalColor.a;

    float heightFactor = 1.0 / (max(length(viewRay.origin) - innerRadius, 0.0) * 20.0 + 1.0);
    heightFactor *= heightFactor;
    float sunAngleFactor = clamp(dot(normalize(viewRay.origin), sunDirection) * 5.3, 0.0, 1.0);

    vec4 powerDay = vec4(0.7, 0.9, 0.3, 0.5);
    vec4 multiplierDay = vec4(1.2, 0.9, 2.5, 1.2);
    vec4 powerSunset = vec4(1.2, 1.4, 0.9, 1.1);
    vec4 multiplierSunset = vec4(0.8, 0.6, 1.3, 1.2);

    vec4 power = mix(powerSunset, powerDay, sunAngleFactor);
    vec4 multiplier = mix(multiplierSunset, multiplierDay, sunAngleFactor);
    finalColor = mix(finalColor, pow(finalColor, power), heightFactor) * mix(vec4(1.0), multiplier, heightFactor);

    return finalColor;
}

void main() {

    if (!gl_FrontFacing) discard;

    AtmosphereProperties atmosphereProps;
    atmosphereProps.planetRadius = PlanetRadius;
    atmosphereProps.atmosphereThickness = max(AtmosphereRadius - PlanetRadius, 0.0001);
    atmosphereProps.waveLengths = WaveLengths;
    atmosphereProps.densityFalloff = 1.0 / max(RayleighScaleHeight, 0.0001);
    atmosphereProps.scatteringStrength = RayleighStrength;
    atmosphereProps.atmosphereTint = BaseColor;

    Ray viewRay;
    viewRay.origin = CameraLocalPos;
    viewRay.direction = normalize(fragLocalPos - CameraLocalPos);

    vec3 sunDirection = normalize(LightDirection);
    vec4 color = calculateAtmosphereEffect(viewRay, sunDirection, atmosphereProps);

    if (color.a <= 0.0001) {
        discard;
    }

    fragColor = color;
}
