package com.neuralterrain.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Development-only workaround for a native glfwWindowShouldClose crash. */
@Mixin(Window.class)
public abstract class WindowDevMixin {
    @Inject(method = "shouldClose", at = @At("HEAD"), cancellable = true)
    private void neuralterrain$avoidNativeShouldClose(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.getBoolean("neuralterrain.dev_no_glfw_wait")) {
            cir.setReturnValue(false);
        }
    }
}
