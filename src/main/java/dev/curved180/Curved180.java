// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Curved180 implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Curved180");
    public static boolean capturing;
    public static boolean shareEnvironment;
    public static final SharedFrameUniforms sharedUniforms = new SharedFrameUniforms();
    public static int view = 1;
    public static double effectiveFov = 70;
    public static double pitchDegrees;
    public static final SharedShadowRaster sharedShadows = new SharedShadowRaster();
    private static boolean enabled = true;

    @Override public void onInitializeClient() {
        try {
            Path config = configPath();
            if (Files.exists(config)) {
                Properties properties = new Properties();
                try (var input = Files.newInputStream(config)) { properties.load(input); }
                enabled = !"false".equalsIgnoreCase(properties.getProperty("enabled"));
            }
        } catch (IOException e) {
            LOG.warn("Could not read Curved180 settings", e);
        }
        LOG.info("Curved180: horizontal FOV 30–360, with camera FOV effects and zoom support");
    }

    public static boolean enabled() { return enabled; }

    public static void setEnabled(boolean value) {
        enabled = value;
        try {
            Path config = configPath();
            Files.createDirectories(config.getParent());
            Files.writeString(config, "enabled=" + value + System.lineSeparator());
        } catch (IOException e) {
            LOG.warn("Could not save Curved180 settings", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("curved180.properties");
    }

    public static ProjectionPlan plan(int width, int height) {
        return ProjectionPlan.create(effectiveFov, Math.max(1, width), Math.max(1, height), pitchDegrees,
            Minecraft.getInstance().options.fov().get() == 360);
    }

    public static ProjectionPlan plan() {
        var window = Minecraft.getInstance().getWindow();
        return plan(window.getWidth(), window.getHeight());
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return enabled && mc.level != null && mc.player != null && !mc.player.isScoping()
            && mc.gameRenderer.mainRenderTarget().getColorTexture() instanceof com.mojang.blaze3d.opengl.GlTexture;
    }
}



