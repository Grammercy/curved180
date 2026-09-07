// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Curved180 implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Curved180");
    public static boolean capturing;
    public static boolean shareEnvironment;
    public static final SharedFrameUniforms sharedUniforms = new SharedFrameUniforms();
    public static int view = 1;
    public static double effectiveFov = 70;
    public static double pitchDegrees;

    @Override public void onInitializeClient() {
        LOG.info("Curved180: horizontal FOV 30–360, with camera FOV effects and zoom support");
    }

    public static ProjectionPlan plan(int width, int height) {
        return ProjectionPlan.create(effectiveFov, Math.max(1, width), Math.max(1, height), pitchDegrees);
    }

    public static ProjectionPlan plan() {
        var window = Minecraft.getInstance().getWindow();
        return plan(window.getWidth(), window.getHeight());
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && !mc.player.isScoping()
            && mc.gameRenderer.mainRenderTarget().getColorTexture() instanceof com.mojang.blaze3d.opengl.GlTexture;
    }
}



