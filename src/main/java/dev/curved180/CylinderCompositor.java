// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlTexture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL33C;
import static org.lwjgl.opengl.GL33C.*;

/** An OpenGL-only post-Iris compositor. All GL state touched here is restored. */
public final class CylinderCompositor implements AutoCloseable {
    private int width, height, program, vao, readFbo, drawFbo;
    private final int[] textures = new int[6];

    public void prepare(int w, int h) {
        try (State ignored = new State()) {
            if (program == 0) {
                int vs = shader(GL_VERTEX_SHADER, "/assets/curved180/shaders/cylinder.vert");
                int fs = 0;
                try {
                    fs = shader(GL_FRAGMENT_SHADER, "/assets/curved180/shaders/cylinder.frag");
                    int linked = glCreateProgram();
                    glAttachShader(linked, vs); glAttachShader(linked, fs); glLinkProgram(linked);
                    if (glGetProgrami(linked, GL_LINK_STATUS) == GL_FALSE) {
                        String log = glGetProgramInfoLog(linked); glDeleteProgram(linked);
                        throw new IllegalStateException("Cylinder program link failed: " + log);
                    }
                    program = linked;
                } finally { glDeleteShader(vs); if (fs != 0) glDeleteShader(fs); }
                vao = glGenVertexArrays(); readFbo = glGenFramebuffers(); drawFbo = glGenFramebuffers();
            }
            if (width != w || height != h) {
                glActiveTexture(GL_TEXTURE0);
                for (int i = 0; i < 6; i++) {
                    if (textures[i] != 0) glDeleteTextures(textures[i]);
                    textures[i] = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, textures[i]);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                }
                width = w; height = h;
            }
        }
    }

    public void capture(int view, RenderTarget target) {
        try (State ignored = new State()) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readFbo);
            glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            check(GL_READ_FRAMEBUFFER);
            glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, textures[view]);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        }
    }

    public void composite(RenderTarget target) {
        try (State ignored = new State()) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFbo);
            glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ((GlTexture)target.getColorTexture()).glId(), 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0); check(GL_DRAW_FRAMEBUFFER);
            glViewport(0, 0, width, height);
            glDisable(GL_DEPTH_TEST); glDisable(GL_BLEND); glDisable(GL_CULL_FACE);
            glDisable(GL_SCISSOR_TEST); glDisable(GL_STENCIL_TEST);
            glDisable(GL_FRAMEBUFFER_SRGB); glDisable(GL_RASTERIZER_DISCARD);
            glColorMask(true, true, true, true);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glUseProgram(program); glBindVertexArray(vao);
            for (int i = 0; i < 6; i++) {
                glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, textures[i]); glBindSampler(i, 0);
                glUniform1i(glGetUniformLocation(program, "view" + i), i);
            }
            ProjectionPlan plan = Curved180.plan(width, height);
            glUniform1f(glGetUniformLocation(program, "horizontalRadians"), (float)plan.horizontalRadians());
            glUniform1f(glGetUniformLocation(program, "verticalTangent"), (float)plan.verticalTangent());
            glUniform1f(glGetUniformLocation(program, "captureTangent"), (float)plan.captureTangent());
            glUniform1f(glGetUniformLocation(program, "pitchRadians"), (float)plan.pitchRadians());
            glUniform1f(glGetUniformLocation(program, "uprightWeight"), (float)plan.uprightWeight());
            glUniform1i(glGetUniformLocation(program, "singleView"), plan.multiView() ? 0 : 1);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        }
    }

    private static int shader(int kind, String path) {
        int shader = glCreateShader(kind);
        try (var in = CylinderCompositor.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Missing shader " + path);
            glShaderSource(shader, new String(in.readAllBytes(), StandardCharsets.UTF_8)); glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
                throw new IOException(glGetShaderInfoLog(shader));
            return shader;
        } catch (IOException e) { glDeleteShader(shader); throw new IllegalStateException("Cannot build cylinder shader", e); }
    }

    private static void check(int target) {
        int status = glCheckFramebufferStatus(target);
        if (status != GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Cylinder framebuffer incomplete: " + status);
    }

    @Override public void close() {
        for (int i = 0; i < 6; i++) { if (textures[i] != 0) glDeleteTextures(textures[i]); textures[i] = 0; }
        if (program != 0) glDeleteProgram(program);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (readFbo != 0) glDeleteFramebuffers(readFbo);
        if (drawFbo != 0) glDeleteFramebuffers(drawFbo);
        program = vao = readFbo = drawFbo = width = height = 0;
    }

    private static final class State implements AutoCloseable {
        private final int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        private final int program = glGetInteger(GL_CURRENT_PROGRAM), vao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        private final int active = glGetInteger(GL_ACTIVE_TEXTURE);
        private final int[] viewport = new int[4], polygon = new int[2], texture = new int[6], sampler = new int[6];
        private final int[] caps = {GL_DEPTH_TEST, GL_BLEND, GL_CULL_FACE, GL_SCISSOR_TEST, GL_STENCIL_TEST, GL_FRAMEBUFFER_SRGB, GL_RASTERIZER_DISCARD};
        private final boolean[] enabled = new boolean[caps.length];
        private final java.nio.ByteBuffer mask = org.lwjgl.BufferUtils.createByteBuffer(4);
        State() {
            glGetIntegerv(GL_VIEWPORT, viewport); glGetIntegerv(GL_POLYGON_MODE, polygon); glGetBooleanv(GL_COLOR_WRITEMASK, mask);
            for (int i = 0; i < caps.length; i++) enabled[i] = glIsEnabled(caps[i]);
            for (int i = 0; i < 6; i++) {
                glActiveTexture(GL_TEXTURE0 + i); texture[i] = glGetInteger(GL_TEXTURE_BINDING_2D); sampler[i] = glGetInteger(GL_SAMPLER_BINDING);
            }
            glActiveTexture(active);
        }
        @Override public void close() {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, read); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, draw);
            glUseProgram(program); glBindVertexArray(vao);
            glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            glPolygonMode(GL_FRONT_AND_BACK, polygon[0]);
            glColorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
            for (int i = 0; i < caps.length; i++) { if (enabled[i]) glEnable(caps[i]); else glDisable(caps[i]); }
            for (int i = 0; i < 6; i++) { glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, texture[i]); glBindSampler(i, sampler[i]); }
            glActiveTexture(active);
        }
    }
}



