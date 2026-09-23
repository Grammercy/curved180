// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import dev.curved180.Curved180;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL;

@Mixin(value = ShadowRenderer.class, remap = false)
public class ShadowRendererMixin {
    @Shadow @Final private ShadowRenderTargets targets;
    @Shadow @Final private ShadowCompositeRenderer compositeRenderer;
    @Shadow private boolean packHasVoxelization;
    @Shadow private void generateMipmaps() {}

    @Inject(method = "renderShadows", at = @At("HEAD"), cancellable = true)
    private void curved$reuseRaster(CallbackInfo ci) {
        // Voxel packs can write to images and buffers beyond the shadow targets.
        if (!Curved180.capturing || Curved180.view == 1 || packHasVoxelization) return;
        if (!Curved180.sharedShadows.restore(targets)) return;
        generateMipmaps();
        ShadowRenderer.ACTIVE = true;
        try {
            compositeRenderer.renderAll();
        } finally {
            ShadowRenderer.ACTIVE = false;
            var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            GlStateManager._viewport(0, 0, target.width, target.height);
        }
        Profiler.get().popPush("updatechunks");
        ci.cancel();
    }

    @Inject(method = "renderShadows", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/shadows/ShadowCompositeRenderer;renderAll()V"))
    private void curved$capture(CallbackInfo ci) {
        if (Curved180.capturing && Curved180.view == 1 && !packHasVoxelization && GL.getCapabilities().OpenGL45)
            Curved180.sharedShadows.capture(targets);
    }
}
