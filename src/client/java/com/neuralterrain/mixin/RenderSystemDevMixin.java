package com.neuralterrain.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Development-only workaround for environments whose GLFW frame wait crashes. */
@Mixin(RenderSystem.class)
public abstract class RenderSystemDevMixin {
    @Inject(method = "limitDisplayFPS", at = @At("HEAD"), cancellable = true)
    private static void neuralterrain$disableFrameWait(int limit, CallbackInfo ci) {
        if (Boolean.getBoolean("neuralterrain.dev_no_glfw_wait")) {
            ci.cancel();
        }
    }
}
