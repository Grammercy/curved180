// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import dev.curved180.Curved180;
import net.irisshaders.iris.pathways.HandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=HandRenderer.class, remap=false)
public class HandRendererMixin {
    @Inject(method={"renderSolid", "renderTranslucent"}, at=@At("HEAD"), cancellable=true)
    private void curved$centerHandOnly(CallbackInfo ci) {
        if (Curved180.capturing) ci.cancel();
    }
}

