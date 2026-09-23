// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

import com.mojang.blaze3d.opengl.GlTexture;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.RenderTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;

import java.util.ArrayList;
import java.util.List;

/** Copies the view-independent light-space raster before each view's shadow composites run. */
public final class SharedShadowRaster {
    private final List<Copy> copies = new ArrayList<>();
    private boolean ready;
    private int resolution;

    public void beginFrame() { ready = false; }
    public boolean ready() { return ready; }

    public void capture(ShadowRenderTargets targets) {
        int size = targets.getResolution();
        List<Source> sources = sources(targets);
        if (size != resolution || copies.size() != sources.size() || !matches(sources)) {
            close();
            resolution = size;
            for (Source source : sources) {
                int texture = GL45C.glCreateTextures(GL11C.GL_TEXTURE_2D);
                GL45C.glTextureStorage2D(texture, 1, source.format, size, size);
                copies.add(new Copy(texture, source.format, source.key));
            }
        }
        for (int i = 0; i < sources.size(); i++) copy(sources.get(i).texture, copies.get(i).texture, size);
        ready = true;
    }

    public boolean restore(ShadowRenderTargets targets) {
        if (!ready || targets.getResolution() != resolution) return false;
        List<Source> destinations = sources(targets);
        if (destinations.size() != copies.size() || !matches(destinations)) return false;
        for (int i = 0; i < destinations.size(); i++) copy(copies.get(i).texture, destinations.get(i).texture, resolution);
        return true;
    }

    private boolean matches(List<Source> sources) {
        for (int i = 0; i < sources.size(); i++)
            if (sources.get(i).format != copies.get(i).format || sources.get(i).key != copies.get(i).key) return false;
        return true;
    }

    private static List<Source> sources(ShadowRenderTargets targets) {
        List<Source> result = new ArrayList<>();
        result.add(new Source(((GlTexture) targets.getDepthTexture()).glId(), GL30C.GL_DEPTH_COMPONENT32F, -2));
        result.add(new Source(((GlTexture) targets.getDepthTextureNoTranslucents()).glId(), GL30C.GL_DEPTH_COMPONENT32F, -1));
        for (int i = 0; i < targets.getNumColorTextures(); i++) {
            RenderTarget target = targets.get(i);
            if (target == null) continue;
            int format = target.getInternalFormat().getGlFormat();
            result.add(new Source(target.getMainTexture(), format, 2 * i));
            result.add(new Source(target.getAltTexture(), format, 2 * i + 1));
        }
        return result;
    }

    private static void copy(int source, int destination, int size) {
        GL43C.glCopyImageSubData(source, GL11C.GL_TEXTURE_2D, 0, 0, 0, 0,
            destination, GL11C.GL_TEXTURE_2D, 0, 0, 0, 0, size, size, 1);
    }

    public void close() {
        for (Copy copy : copies) GL11C.glDeleteTextures(copy.texture);
        copies.clear();
        ready = false;
        resolution = 0;
    }

    private record Source(int texture, int format, int key) {}
    private record Copy(int texture, int format, int key) {}
}
