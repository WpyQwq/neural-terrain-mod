package com.neuralterrain.mixin;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.world.NeuralDecoration;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the model-driven decoration pass after vanilla biome features have run. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void neuralterrain$decorate(
        WorldGenLevel level,
        ChunkAccess chunk,
        StructureManager structureManager,
        CallbackInfo ci
    ) {
        if (NeuralTerrainConfig.enabled() && NeuralTerrainConfig.generateNeuralDecorations()) {
            NeuralDecoration.decorate(level, chunk, level.getSeed());
        }
    }
}
