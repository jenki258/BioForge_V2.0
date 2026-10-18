#version 150
uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D HistorySampler;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec4 Effects;
uniform vec4 Phantom;
uniform float PhantomStyle;
uniform float Time;
uniform float HeartRate;
in vec2 texCoord;
out vec4 fragColor;

float ellipse(vec2 p, vec2 size) { return 1.0 - smoothstep(0.85, 1.05, length(p / size)); }

vec3 phantomColor(vec3 color) {
    if (Phantom.w <= 0.0) return color;
    vec4 projected = InverseProjection * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
    vec3 ray = normalize((InverseView * vec4(projected.xyz / projected.w, 0.0)).xyz);
    vec3 normal = normalize(vec3(Phantom.x, 0.0, Phantom.z) + vec3(0.00001, 0.0, 0.0));
    float denominator = dot(ray, normal);
    if (denominator <= 0.001) return color;
    float distance = dot(Phantom.xyz, normal) / denominator;
    if (distance <= 0.0) return color;
    float depth = texture(DepthSampler, texCoord).r;
    vec4 view = InverseProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    if (depth < 0.999999 && distance > length(view.xyz / view.w) - 0.05) return color;
    vec3 right = vec3(normal.z, 0.0, -normal.x);
    vec3 local = ray * distance - Phantom.xyz;
    vec2 p = vec2(dot(local, right), local.y);
    float head = ellipse(p - vec2(0.0, 0.63), vec2(0.23, 0.3));
    float body = ellipse(p - vec2(0.0, -0.27), vec2(0.32 + max(0.0, -p.y) * 0.1, 0.83));
    float eyes = ellipse(vec2(abs(p.x) - 0.087, p.y - 0.68), vec2(0.027, 0.018));
    float mask = max(head, body);
    vec3 shade = vec3(0.004, 0.007, 0.009);
    if (PhantomStyle > 0.5 && PhantomStyle < 1.5) mask *= 0.15;
    if (PhantomStyle > 1.5) {
        shade += vec3(0.06, 0.065, 0.055) * head;
        float mouth = ellipse(p - vec2(0.0, 0.48), vec2(0.055, 0.10));
        shade *= 1.0 - mouth;
    }
    float opacity = Phantom.w * Effects.w;
    color = mix(color, shade, mask * opacity * 0.9);
    return mix(color, vec3(0.32, 0.38, 0.31), eyes * opacity * 0.7);
}

void main() {
    vec2 uv = texCoord;
    float ender = Effects.x * Effects.w;
    vec2 bend = vec2(sin(uv.y * 13.0 + Time * 1.7), cos(uv.x * 11.0 - Time * 1.3)) * ender * 0.005;
    vec2 shifted = clamp(uv + bend, vec2(0.002), vec2(0.998));
    vec3 color = texture(SceneSampler, shifted).rgb;
    if (ender > 0.001) {
        vec3 past = texture(HistorySampler, clamp(shifted + vec2(0.012, -0.003) * ender, vec2(0.002), vec2(0.998))).rgb;
        color = mix(color, past, ender * 0.26);
        color.r = mix(color.r, texture(SceneSampler, clamp(shifted + vec2(0.005 * ender, 0.0), vec2(0.002), vec2(0.998))).r, 0.65);
        color.b = mix(color.b, texture(SceneSampler, clamp(shifted - vec2(0.005 * ender, 0.0), vec2(0.002), vec2(0.998))).b, 0.65);
        color *= vec3(1.0, 1.0 - ender * 0.05, 1.0);
    }
    color = phantomColor(color);
    vec2 centered = (uv - 0.5) * 2.0;
    float radius = length(centered);
    float wither = Effects.y * Effects.w;
    if (wither > 0.001) {
        float grey = dot(color, vec3(0.299, 0.587, 0.114));
        color = mix(color, vec3(grey * 0.93, grey * 0.96, grey * 0.90), wither * 0.7);
        float angle = atan(centered.y, centered.x);
        float branches = abs(sin(angle * 19.0 + sin(radius * 24.0 + angle * 4.0) * 0.7));
        float veins = (1.0 - smoothstep(0.015, 0.09, branches)) * smoothstep(0.52, 1.15, radius);
        color *= 1.0 - wither * (veins * 0.65 + smoothstep(0.65, 1.35, radius) * 0.25);
        float ash = pow(max(0.0, sin(uv.x * 271.0 + sin(uv.y * 193.0)) * sin(uv.y * 167.0 + Time * 0.25)), 40.0);
        color += vec3(0.07) * ash * wither * smoothstep(0.4, 1.2, radius);
    }
    float hypoxia = Effects.z * Effects.w;
    if (hypoxia > 0.001) {
        float beat = pow(0.5 + 0.5 * sin(Time * HeartRate * 6.2831853), 6.0);
        float rim = smoothstep(mix(0.95, 0.38, hypoxia), 1.35 - hypoxia * 0.15, radius);
        color *= 1.0 - rim * hypoxia * (0.78 + beat * 0.13);
        color = mix(color, vec3(dot(color, vec3(0.299, 0.587, 0.114))), hypoxia * 0.18);
    }
    fragColor = vec4(color, 1.0);
}
