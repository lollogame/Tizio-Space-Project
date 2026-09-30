#version 150

const int MAX_SUNS = 4;

in vec4 Position;

uniform mat4 ProjMat;
uniform mat4 SunProjMat;
uniform mat4 SunViewMat;
uniform vec2 OutSize;
uniform mat4 SunRelPosMat;

out vec2 texCoord;
out vec2 oneTexel;

flat out vec2 vSunScreenPos[MAX_SUNS];
flat out float vSunDepth[MAX_SUNS];
flat out float vSunOnScreen[MAX_SUNS];

void projectSun(vec4 relPos, int index) {
    vec4 clip = SunProjMat * SunViewMat * relPos;

    if (clip.w > 0.0) {
        vec3 ndc = clip.xyz / clip.w;
        vec2 screenPos = ndc.xy * 0.5 + 0.5;

        vSunScreenPos[index] = screenPos;
        vSunDepth[index] = clamp(ndc.z * 0.5 + 0.5, 0.0, 0.999);
        vSunOnScreen[index] = (screenPos.x > -0.2 && screenPos.x < 1.2 &&
        screenPos.y > -0.2 && screenPos.y < 1.2) ? 1.0 : 0.0;
    } else {
        vSunScreenPos[index] = vec2(0.5);
        vSunDepth[index] = 0.999;
        vSunOnScreen[index] = 0.0;
    }
}

void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    texCoord = Position.xy / OutSize;
    oneTexel = 1.0 / OutSize;

    projectSun(SunRelPosMat[0], 0);
    projectSun(SunRelPosMat[1], 1);
    projectSun(SunRelPosMat[2], 2);
    projectSun(SunRelPosMat[3], 3);
}