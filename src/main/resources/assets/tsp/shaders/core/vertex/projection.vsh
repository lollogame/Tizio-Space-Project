#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 CenterRelative;
uniform vec3 AxisX;
uniform vec3 AxisY;
uniform vec3 AxisZ;

out vec3 fragLocalPos;

void main() {
    vec3 worldOffset = Position - CenterRelative;
    fragLocalPos = vec3(dot(worldOffset, AxisX), dot(worldOffset, AxisY), dot(worldOffset, AxisZ));
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
