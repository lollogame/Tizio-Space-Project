#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ProjMat;
uniform vec3 CenterRelative;
uniform vec3 AxisX;
uniform vec3 AxisY;
uniform vec3 AxisZ;

uniform float RingInnerRadius;
uniform float RingOuterRadius;
uniform float RockMinSize;
uniform float RockMaxSize;
uniform float RockHeight;
uniform float OrbitSpeed;
uniform float Time;
uniform float RingSeed;

out vec2 fragUV;
out vec3 fragLocalPos;
out float fragDistAlpha;

const float TAU = 6.28318530718;
const int VERTS_PER_CUBE = 24;
const float SQRT_3 = 1.7320508;

const float MAX_DIST_RATIO   = 0.25; //0.35
const float FADE_START_RATIO = 0.85; //0.85

float hash1(float n, float salt) {
    return fract(sin(n * 12.9898 + salt * 78.233) * 43758.5453123);
}

mat3 rotationAxis(vec3 axis, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    float t = 1.0 - c;
    vec3 a = axis;
    return mat3(
    t*a.x*a.x + c,       t*a.x*a.y + s*a.z,  t*a.x*a.z - s*a.y,
    t*a.x*a.y - s*a.z,   t*a.y*a.y + c,       t*a.y*a.z + s*a.x,
    t*a.x*a.z + s*a.y,   t*a.y*a.z - s*a.x,   t*a.z*a.z + c
    );
}

void main() {
    int cubeIndex = gl_VertexID / VERTS_PER_CUBE;

    float fi = float(cubeIndex) + RingSeed * 1000.0;

    float ringWidth  = max(RingOuterRadius - RingInnerRadius, 0.1);
    float numTracks  = floor(clamp(ringWidth * 0.5, 8.0, 64.0));
    float invNumTracks = 1.0 / numTracks;

    float idxOverTracks = float(cubeIndex) * invNumTracks;
    float slotIdx    = floor(idxOverTracks);
    float trackIdx   = float(cubeIndex) - numTracks * slotIdx;
    float trackWidth = ringWidth * invNumTracks;

    float rJitter    = (hash1(fi, 1.0) - 0.5) * 0.6 * trackWidth;
    float baseRadius = RingInnerRadius + (trackIdx + 0.5) * trackWidth + rJitter;

    float hashAngle   = hash1(trackIdx, 2.0) * TAU;
    float angleJitter = (hash1(fi, 3.0) - 0.5) * 0.3;

    float slotOffset  = fract(slotIdx * 1.61803398875) * TAU;
    float baseAngle   = mod(hashAngle + slotOffset + angleJitter, TAU);
    float keplerSpeed = OrbitSpeed * sqrt(RingInnerRadius / max(baseRadius, 0.1));

    float deltaAngle  = mod(Time * keplerSpeed, TAU);
    float angle       = mod(baseAngle + deltaAngle, TAU);

    float radialPhase  = sin(angle * 2.0 + hash1(fi, 11.0) * TAU) * 0.5 + 0.5;
    float smoothRadius = mix(baseRadius * 0.98, baseRadius * 1.02, radialPhase);

    float height = (hash1(fi, 4.0) - 0.5) * 2.0 * RockHeight;
    float size   = mix(RockMinSize, RockMaxSize, hash1(fi, 5.0));

    vec3 ringLocalPos = vec3(cos(angle) * smoothRadius, height, sin(angle) * smoothRadius);
    vec3 worldCenterOffset = ringLocalPos.x * AxisX + ringLocalPos.y * AxisY + ringLocalPos.z * AxisZ;
    vec3 rockCenterCam = CenterRelative + worldCenterOffset;

    float centerDistSq = dot(rockCenterCam, rockCenterCam);
    float maxDist      = max(RingOuterRadius * MAX_DIST_RATIO, 20.0);

    if (centerDistSq > maxDist * maxDist) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }

    float boundingRadius = size * SQRT_3;
    vec4 centerClip      = ProjMat * vec4(rockCenterCam, 1.0);


    float marginX = ProjMat[0][0] * boundingRadius;
    float marginY = ProjMat[1][1] * boundingRadius;
    float marginZ = boundingRadius;


    if (centerClip.x < -centerClip.w - marginX ||
    centerClip.x >  centerClip.w + marginX ||
    centerClip.y < -centerClip.w - marginY ||
    centerClip.y >  centerClip.w + marginY ||
    centerClip.z < -centerClip.w - marginZ ||
    centerClip.z >  centerClip.w + marginZ) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }


    float centerDist = sqrt(centerDistSq);
    float fadeStart  = maxDist * FADE_START_RATIO;
    fragDistAlpha    = 1.0 - smoothstep(fadeStart, maxDist, centerDist);


    vec3 spinAxis = normalize(vec3(
    hash1(fi, 6.0) * 2.0 - 1.0,
    hash1(fi, 7.0) * 2.0 - 1.0,
    hash1(fi, 8.0) * 2.0 - 1.0
    ));
    float spinSpeed   = 0.02 + hash1(fi, 9.0) * 0.52;
    float spinAngle   = mod(hash1(fi, 10.0) * TAU + mod(Time * spinSpeed, TAU), TAU);
    mat3 selfRotation = rotationAxis(spinAxis, spinAngle);

    vec3 cubeCorner   = selfRotation * (Position * size);
    vec3 localPos     = ringLocalPos + cubeCorner;

    vec3 worldCornerOffset = cubeCorner.x * AxisX + cubeCorner.y * AxisY + cubeCorner.z * AxisZ;
    vec3 finalPos          = rockCenterCam + worldCornerOffset;

    fragUV       = UV0;
    fragLocalPos = localPos;

    gl_Position = ProjMat * vec4(finalPos, 1.0);
}