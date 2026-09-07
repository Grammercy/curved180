// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import dev.curved180.Curved180;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;
    @Shadow private float xRot;
    @Shadow private float yRot;

    @ModifyExpressionValue(method="update", at=@At(value="INVOKE", target="Lnet/minecraft/client/Camera;calculateFov(F)F"))
    private float curved$fov(float adjustedFov) {
        if (!Curved180.active()) return adjustedFov;
        // Observe the completed vanilla + mod calculation, including Zoomify's return modifier.
        // Freeze it while rendering secondary cameras so animation advances only once per frame.
        if (!Curved180.capturing) Curved180.effectiveFov = adjustedFov;
        return Curved180.plan().captureDegrees();
    }

    @ModifyArgs(method="update", at=@At(value="INVOKE", target="Lnet/minecraft/client/Camera;setupPerspective(FFFFF)V"))
    private void curved$square(Args args) {
        if (Curved180.active()) { args.set(2, Curved180.plan().captureDegrees()); args.set(3, 1f); args.set(4, 1f); }
    }

    @Inject(method="createProjectionMatrixForCulling", at=@At("RETURN"), cancellable=true)
    private void curved$culling(CallbackInfoReturnable<Matrix4f> ci) {
        if (Curved180.active()) {
            // Preserve the engine's depth convention; only replace X/Y focal lengths.
            float focal = (float)(1.0 / Curved180.plan().captureTangent());
            ci.getReturnValue().m00(focal).m11(focal);
        }
    }

    @Inject(method="update", at=@At(value="INVOKE", target="Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift=At.Shift.AFTER))
    private void curved$rotate(DeltaTracker delta, CallbackInfo ci) {
        if (!Curved180.active()) return;
        if (!Curved180.capturing) Curved180.pitchDegrees = xRot;
        if (!Curved180.capturing || Curved180.view == 1) return;
        float angle = switch (Curved180.view) { case 0 -> -(float)Math.PI / 2; case 2 -> (float)Math.PI / 2; case 3 -> (float)Math.PI; default -> 0f; };
        // Rotate in camera-local space, preserving pitch and roll continuity across seams.
        rotation.rotateY(-angle);
        if (Curved180.view == 4) rotation.rotateX((float)Math.PI / 2);
        if (Curved180.view == 5) rotation.rotateX(-(float)Math.PI / 2);
        forwards.set(0, 0, -1).rotate(rotation);
        up.set(0, 1, 0).rotate(rotation);
        left.set(-1, 0, 0).rotate(rotation);
        xRot = (float)Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, forwards.y))));
        yRot = (float)Math.toDegrees(Math.atan2(-forwards.x, forwards.z));
        matrixPropertiesDirty |= 3;
    }
}


