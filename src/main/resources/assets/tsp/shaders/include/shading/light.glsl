#version 150

vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));

    float len1 = length(lightDir1);
    if (len1 > 0.90) {
        float lightAccum = min(1.0, (light0 + light1) * 0.8 + 0.2);
        return vec4(color.rgb * lightAccum, color.a);
    } else {
        float lightAccum = min(1.0, light0 * 0.90 + light1 * 0.90 + 0.08);
        return vec4(color.rgb * lightAccum, color.a);
    }
}