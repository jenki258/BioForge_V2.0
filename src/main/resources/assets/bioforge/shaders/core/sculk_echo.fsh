#version 150
uniform sampler2D DepthSampler;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec4 Echo0;
uniform vec4 Echo1;
uniform vec4 Echo2;
uniform vec4 Echo3;
uniform vec4 Echo4;
uniform vec4 Echo5;
uniform vec4 Echo6;
uniform vec4 Echo7;
uniform vec4 Echo8;
uniform vec4 Echo9;
uniform vec4 Echo10;
uniform vec4 Echo11;
uniform vec4 Echo12;
uniform vec4 Echo13;
uniform vec4 Echo14;
uniform vec4 Echo15;
uniform vec4 Echo16;
uniform vec4 Echo17;
uniform vec4 Echo18;
uniform vec4 Echo19;
uniform vec4 Echo20;
uniform vec4 Echo21;
uniform vec4 Echo22;
uniform vec4 Echo23;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.999999) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    vec4 view = InverseProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    view /= view.w;
    vec3 position = (InverseView * vec4(view.xyz, 1.0)).xyz;
    vec4 echoes[24] = vec4[24](Echo0, Echo1, Echo2, Echo3, Echo4, Echo5, Echo6, Echo7, Echo8, Echo9, Echo10, Echo11, Echo12, Echo13, Echo14, Echo15, Echo16, Echo17, Echo18, Echo19, Echo20, Echo21, Echo22, Echo23);
    float energy = 0.0;
    for (int i = 0; i < 24; i++) {
        float radius = echoes[i].w;
        if (radius < 0.0 || radius > 40.0) continue;
        float distance = length(position - echoes[i].xyz);
        float passed = radius - distance;
        float wave = exp(-abs(passed) * 0.6) * smoothstep(-1.2, 0.1, passed);
        float fade = 1.0 - smoothstep(10.0, 32.0, radius);
        energy = max(energy, wave * fade);
    }
    vec3 normal = normalize(cross(dFdx(position), dFdy(position)));
    float silhouette = pow(1.0 - abs(dot(normal, normalize(-position))), 2.0);
    float edge = clamp(length(fwidth(normal)) * 2.0, 0.0, 1.0);
    float detail = 0.24 + 0.52 * silhouette + 0.65 * edge;
    vec3 color = vec3(0.06, 0.72, 0.78) * energy * detail;
    fragColor = vec4(color, 1.0);
}
