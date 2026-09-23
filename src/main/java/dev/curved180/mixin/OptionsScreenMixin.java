// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.curved180.Curved180;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {
    @Inject(method = "init", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private void curved$addToggle(CallbackInfo ci, @Local GridLayout.RowHelper row) {
        row.addChild(Button.builder(curved$label(), pressed -> {
                Curved180.setEnabled(!Curved180.enabled());
                pressed.setMessage(curved$label());
            })
            .tooltip(Tooltip.create(Component.translatable("curved180.toggle.tooltip")))
            .build());
    }

    private static Component curved$label() {
        boolean enabled = Curved180.enabled();
        return Component.translatable(enabled ? "curved180.toggle.on" : "curved180.toggle.off")
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
