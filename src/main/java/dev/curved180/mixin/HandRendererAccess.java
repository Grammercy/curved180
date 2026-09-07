// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import net.irisshaders.iris.pathways.HandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=HandRenderer.class, remap=false)
public interface HandRendererAccess {
    @Accessor("renderingSolid") boolean curved$getRenderingSolid();
    @Accessor("renderingSolid") void curved$setRenderingSolid(boolean solid);
}

