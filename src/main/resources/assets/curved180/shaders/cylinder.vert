#version 330 core
// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
out vec2 uv;
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    uv = p;
    gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
}

