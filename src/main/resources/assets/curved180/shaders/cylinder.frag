#version 330 core
// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
in vec2 uv;
out vec4 fragColor;
uniform sampler2D view0;
uniform sampler2D view1;
uniform sampler2D view2;
uniform sampler2D view3;
uniform sampler2D view4;
uniform sampler2D view5;
uniform float horizontalRadians;
uniform float verticalTangent;
uniform float captureTangent;
uniform float pitchRadians;
uniform float uprightWeight;
uniform bool singleView;

vec2 st(vec2 xy, float forward) { return xy / (forward * captureTangent) * 0.5 + 0.5; }
void main() {
    float theta = (uv.x - 0.5) * horizontalRadians;
    float y = (uv.y * 2.0 - 1.0) * verticalTangent;
    vec3 ray = normalize(vec3(sin(theta), y, -cos(theta)));
    if (uprightWeight > 0.0) {
        float p = clamp(pitchRadians, -1.483529864, 1.483529864);
        float elevation = atan(y - tan(p));
        vec3 upright = vec3(sin(theta) * cos(elevation), sin(elevation), -cos(theta) * cos(elevation));
        upright.yz = mat2(cos(p), sin(p), -sin(p), cos(p)) * upright.yz;
        ray = normalize(mix(ray, upright, uprightWeight));
    }
    if (singleView) { fragColor = texture(view1, st(ray.xy, -ray.z)); return; }
    // Overlapping cube faces: blend rays, not unrelated positions in neighbouring images.
    vec3 a = abs(ray);
    float largest = max(a.x, max(a.y, a.z));
    vec3 weights = smoothstep(vec3(largest * 0.9), vec3(largest), a);
    vec4 color = vec4(0.0);
    if (weights.x > 0.0) {
        vec4 c;
        if (ray.x > 0.0) c = texture(view2, st(vec2(ray.z, ray.y), ray.x));
        else c = texture(view0, st(vec2(-ray.z, ray.y), -ray.x));
        color += weights.x * c;
    }
    if (weights.y > 0.0) {
        vec4 c;
        if (ray.y > 0.0) c = texture(view4, st(vec2(ray.x, ray.z), ray.y));
        else c = texture(view5, st(vec2(ray.x, -ray.z), -ray.y));
        color += weights.y * c;
    }
    if (weights.z > 0.0) {
        vec4 c;
        if (ray.z < 0.0) c = texture(view1, st(ray.xy, -ray.z));
        else c = texture(view3, st(vec2(-ray.x, ray.y), ray.z));
        color += weights.z * c;
    }
    fragColor = color / (weights.x + weights.y + weights.z);
}

