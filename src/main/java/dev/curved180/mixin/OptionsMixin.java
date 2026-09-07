// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Options.class)
public class OptionsMixin {
    @Shadow @Final @Mutable private OptionInstance<Integer> fov;

    // Replace only the FOV option, before Options loads its saved value.
    @Redirect(method="<init>", at=@At(value="FIELD", target="Lnet/minecraft/client/Options;fov:Lnet/minecraft/client/OptionInstance;", opcode=Opcodes.PUTFIELD))
    private void curved$extendSlider(Options options, OptionInstance<Integer> original) {
        fov = new OptionInstance<>("options.fov",
            OptionInstance.cachedConstantTooltip(Component.translatable("curved180.fov.tooltip")),
            (caption, value) -> Component.translatable("curved180.fov.label", value),
            new OptionInstance.IntRange(30, 360), original.codec(), 70, OptionInstance.NO_ACTION);
    }
}

