package com.neuralterrain.mixin;

import com.neuralterrain.world.NeuralTerrainContext;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the world seed once the server chunk source has been created. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void neuralterrain$captureWorldSeed(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        NeuralTerrainContext.register(level.getChunkSource().getGenerator(), level.getSeed());
    }
}
