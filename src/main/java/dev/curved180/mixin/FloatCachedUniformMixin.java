// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import dev.curved180.Curved180;
import net.irisshaders.iris.uniforms.custom.cached.FloatCachedUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=FloatCachedUniform.class, remap=false)
public abstract class FloatCachedUniformMixin {
    @Shadow private float cached;

    @Inject(method="doUpdate", at=@At("RETURN"))
    private void curved$shareEnvironment(CallbackInfoReturnable<Boolean> ci) {
        if (Curved180.capturing && Curved180.shareEnvironment) {
            String name = ((FloatCachedUniform)(Object)this).getName();
            cached = Curved180.sharedUniforms.resolve(name, cached, Curved180.view);
        }
    }
}

