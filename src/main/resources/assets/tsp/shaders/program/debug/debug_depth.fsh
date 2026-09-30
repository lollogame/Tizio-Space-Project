#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform float ZNear;
uniform float ZFar;

in vec2 texCoord;
out vec4 fragColor;

void main() {

    float depth = texture(DepthSampler, texCoord).r;
    float ndc = depth * 2.0 - 1.0;
    float linearDepth = (2.0 * ZNear * ZFar) / (ZFar + ZNear - ndc * (ZFar - ZNear));
    float normalizedDepth = linearDepth / ZFar;

    fragColor = vec4(vec3(normalizedDepth), 1.0);
}