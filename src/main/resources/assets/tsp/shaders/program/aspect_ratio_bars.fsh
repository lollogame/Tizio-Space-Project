#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float TargetAspect;

in vec2 texCoord;
out vec4 fragColor;

void main() {

    vec4 baseScene = texture(DiffuseSampler, texCoord);
    float currentAspect = OutSize.x / OutSize.y;

    if (currentAspect < TargetAspect) {
        float barHeight = (1.0 - (currentAspect / TargetAspect)) * 0.5;
        if (texCoord.y < barHeight || texCoord.y > (1.0 - barHeight)) {
            fragColor = vec4(0.0, 0.0, 0.0, baseScene.a);
            return;
        }
    }

    fragColor = baseScene;
}