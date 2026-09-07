// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.curved180.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.DebugCrosshairRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.GameType;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.vertices.ImmediateState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private void extractCamera(DeltaTracker delta, float worldPartial, float cameraPartial) {}
    @Shadow @Final private Projection hudProjection;
    @Shadow @Final private ProjectionMatrixBuffer hud3dProjectionMatrixBuffer;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private DebugCrosshairRenderer debugCrosshairRenderer;
    @Shadow private void bobHurt(CameraRenderState camera, PoseStack poses) {}
    @Shadow private void bobView(CameraRenderState camera, PoseStack poses) {}
    @Unique private final CylinderCompositor curved$compositor = new CylinderCompositor();
    @Unique private final SubmitNodeStorage curved$handNodes = new SubmitNodeStorage();

    @WrapOperation(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
    private void curved$render(GameRenderer renderer, DeltaTracker delta, Operation<Void> original) {
        if (!Curved180.active()) { original.call(renderer, delta); return; }
        var state = renderer.gameRenderState();
        boolean bob = state.optionsRenderState.bobView;
        float effects = state.optionsRenderState.screenEffectScale;
        float hurt = state.levelRenderState.cameraRenderState.entityRenderState.hurtTime;
        Curved180.sharedUniforms.beginFrame();
        Curved180.shareEnvironment = Iris.getCurrentPackName().toLowerCase(java.util.Locale.ROOT).contains("complementary");
        Curved180.capturing = true;
        try {
            curved$compositor.prepare(renderer.mainRenderTarget().width, renderer.mainRenderTarget().height);
            state.optionsRenderState.bobView = false;
            state.optionsRenderState.screenEffectScale = 0;
            for (int v : Curved180.plan().views()) {
                Curved180.view = v;
                if (v != 1) {
                    renderer.update(delta);
                    boolean prior = ImmediateState.isRenderingLevel;
                    ImmediateState.isRenderingLevel = true;
                    try {
                        extractCamera(delta, delta.getGameTimeDeltaPartialTick(false), renderer.mainCamera().getCameraEntityPartialTicks(delta));
                        minecraft.levelExtractor.extract(delta, renderer.mainCamera(), delta.getGameTimeDeltaPartialTick(false));
                    } finally { ImmediateState.isRenderingLevel = prior; }
                }
                state.levelRenderState.cameraRenderState.entityRenderState.hurtTime = 0;
                // Each view must receive a freshly cleared target, including depth.
                var target = renderer.mainRenderTarget();
                com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
                    .clearColorAndDepthTextures(target.getColorTexture(), new org.joml.Vector4f(0, 0, 0, 1), target.getDepthTexture(), 0.0);
                original.call(renderer, delta);
                curved$compositor.capture(v, target);
            }
            curved$compositor.composite(renderer.mainRenderTarget());
        } finally {
            Curved180.view = 1;
            Curved180.capturing = false;
            state.optionsRenderState.bobView = bob;
            state.optionsRenderState.screenEffectScale = effects;
            renderer.update(delta);
            boolean prior = ImmediateState.isRenderingLevel;
            ImmediateState.isRenderingLevel = true;
            try { extractCamera(delta, delta.getGameTimeDeltaPartialTick(false), renderer.mainCamera().getCameraEntityPartialTicks(delta)); }
            finally { ImmediateState.isRenderingLevel = prior; }
            state.levelRenderState.cameraRenderState.entityRenderState.hurtTime = hurt;
            Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
        }
        curved$renderHand(renderer, delta);
        if (state.levelRenderState.render3dCrosshair && state.optionsRenderState.cameraType.isFirstPerson()
                && !state.guiRenderState.isHudHidden) {
            var target = renderer.mainRenderTarget();
            RenderSystem.backupProjectionMatrix();
            try {
                hudProjection.setupPerspective(0.05f, 100f, state.levelRenderState.cameraRenderState.hudFov, target.width, target.height);
                RenderSystem.setProjectionMatrix(hud3dProjectionMatrixBuffer.getBuffer(hudProjection), ProjectionType.PERSPECTIVE);
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthTexture(), 0.0);
                debugCrosshairRenderer.render(state.levelRenderState.cameraRenderState, state.windowRenderState.guiScale);
            } finally { RenderSystem.restoreProjectionMatrix(); }
        }
    }

    @WrapOperation(method="renderLevel", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/DebugCrosshairRenderer;render(Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V"))
    private void curved$deferCrosshair(DebugCrosshairRenderer renderer, CameraRenderState camera, int scale, Operation<Void> original) {
        if (!Curved180.capturing) original.call(renderer, camera, scale);
    }

    /** Render the first-person layer once with normal perspective after all world postprocessing. */
    @Unique private void curved$renderHand(GameRenderer renderer, DeltaTracker delta) {
        var state = renderer.gameRenderState();
        var camera = state.levelRenderState.cameraRenderState;
        if (!state.optionsRenderState.cameraType.isFirstPerson() || camera.entityRenderState.isSleeping
                || state.guiRenderState.isHudHidden || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        float partial = renderer.mainCamera().getCameraEntityPartialTicks(delta);
        var target = renderer.mainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthTexture(), 0.0);
        hudProjection.setupPerspective(0.05f, 100f, camera.hudFov, target.width, target.height);
        RenderSystem.backupProjectionMatrix();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            RenderSystem.setProjectionMatrix(hud3dProjectionMatrixBuffer.getBuffer(hudProjection), ProjectionType.PERSPECTIVE);
            modelView.mul(camera.viewRotationMatrix);
            PoseStack poses = new PoseStack();
            poses.mulPose(camera.viewRotationMatrix.invert(new Matrix4f()));
            bobHurt(camera, poses);
            if (state.optionsRenderState.bobView) bobView(camera, poses);
            // Call directly: Iris suppresses the vanilla call site inside renderItemInHand.
            // The world pipeline has finished; this layer intentionally uses vanilla item lighting.
            int light = minecraft.getEntityRenderDispatcher().getPackedLightCoords(minecraft.player, partial);
            if (Iris.isPackInUseQuick()) {
                HandRendererAccess hand = (HandRendererAccess) net.irisshaders.iris.pathways.HandRenderer.INSTANCE;
                boolean wasSolid = hand.curved$getRenderingSolid();
                try {
                    // Iris filters opaque and translucent hands separately even outside its render pass.
                    hand.curved$setRenderingSolid(true);
                    renderer.itemInHandRenderer.submitHandsWithItems(partial, poses, curved$handNodes, minecraft.player, light);
                    hand.curved$setRenderingSolid(false);
                    renderer.itemInHandRenderer.submitHandsWithItems(partial, poses, curved$handNodes, minecraft.player, light);
                } finally { hand.curved$setRenderingSolid(wasSolid); }
            } else {
                renderer.itemInHandRenderer.submitHandsWithItems(partial, poses, curved$handNodes, minecraft.player, light);
            }
            featureRenderDispatcher.renderAllFeatures(curved$handNodes);
        } finally {
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    @Inject(method="close", at=@At("HEAD"))
    private void curved$close(CallbackInfo ci) { curved$compositor.close(); }

    @Inject(method="renderItemInHand", at=@At("HEAD"), cancellable=true)
    private void curved$sideHand(CallbackInfo ci) {
        if (Curved180.capturing) ci.cancel();
    }
}


