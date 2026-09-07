// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180.mixin;

import dev.curved180.Curved180;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=PipelineManager.class, remap=false)
public class PipelineManagerMixin {
    // Extra views are not dimension changes. Resetting shared time here desynchronizes
    // clouds, animated noise and temporal jitter partway through a displayed frame.
    @WrapOperation(method="preparePipeline", at=@At(value="INVOKE", target="Lnet/irisshaders/iris/uniforms/SystemTimeUniforms$FrameCounter;reset()V"))
    private void curved$keepFrame(SystemTimeUniforms.FrameCounter counter, Operation<Void> original) {
        if (!Curved180.capturing || Curved180.view == 1) original.call(counter);
    }
    @WrapOperation(method="preparePipeline", at=@At(value="INVOKE", target="Lnet/irisshaders/iris/uniforms/SystemTimeUniforms$Timer;reset()V"))
    private void curved$keepTime(SystemTimeUniforms.Timer timer, Operation<Void> original) {
        if (!Curved180.capturing || Curved180.view == 1) original.call(timer);
    }
    // Change cache keys only. The factory still receives the REAL dimension, so
    // dimension-specific shader programs and material maps remain correct.
    @ModifyArg(method="preparePipeline", at=@At(value="INVOKE", target="Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"), index=0)
    private Object curved$contains(Object key) { return curved$key(key); }
    @ModifyArg(method="preparePipeline", at=@At(value="INVOKE", target="Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), index=0)
    private Object curved$get(Object key) { return curved$key(key); }
    @ModifyArg(method="preparePipeline", at=@At(value="INVOKE", target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), index=0)
    private Object curved$put(Object key) { return curved$key(key); }
    private static Object curved$key(Object key) {
        if (!Curved180.capturing || Curved180.view == 1) return key;
        NamespacedId dimension = (NamespacedId)key;
        return new NamespacedId("curved180", dimension.getNamespace() + "/" + dimension.getName() + "/view" + Curved180.view);
    }
}

