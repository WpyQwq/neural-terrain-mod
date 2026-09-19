package com.neuralterrain.mixin;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.world.NeuralTerrainContext;
import com.neuralterrain.world.NeuralTerrainBiomes;
import com.neuralterrain.world.NeuralTerrainRewriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla noise terrain from running and supplies the neural heightfield directly. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "createBiomes", at = @At("HEAD"), cancellable = true)
    private void neuralterrain$rewriteBiomes(
        Executor executor,
        RandomState randomState,
        Blender blender,
        StructureManager structureManager,
        ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        if (!NeuralTerrainConfig.enabled() || !NeuralTerrainConfig.generateNeuralBiomes()) {
            return;
        }
        long seed = NeuralTerrainContext.seedFor((NoiseBasedChunkGenerator) (Object) this);
        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight();
        Registry<Biome> biomeRegistry = structureManager.registryAccess().registryOrThrow(Registries.BIOME);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> {
            var prediction = com.neuralterrain.model.NeuralTerrainModel.predict(seed, x, z, minY, maxY);
            return NeuralTerrainBiomes.select(biomeRegistry, prediction);
        }, null);
        cir.setReturnValue(CompletableFuture.completedFuture(chunk));
    }

    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void neuralterrain$rewriteTerrain(
        Executor executor,
        Blender blender,
        RandomState randomState,
        StructureManager structureManager,
        ChunkAccess chunk,
        CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        if (!NeuralTerrainConfig.enabled()) {
            return;
        }
        long seed = NeuralTerrainContext.seedFor((NoiseBasedChunkGenerator) (Object) this);
        cir.setReturnValue(CompletableFuture.completedFuture(NeuralTerrainRewriter.rewrite(chunk, seed)));
    }

    @Inject(
        method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void neuralterrain$skipVanillaSurface(
        WorldGenRegion region,
        StructureManager structureManager,
        RandomState randomState,
        ChunkAccess chunk,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci
    ) {
        if (NeuralTerrainConfig.enabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void neuralterrain$skipVanillaCarvers(
        WorldGenRegion region,
        long seed,
        RandomState randomState,
        BiomeManager biomeManager,
        StructureManager structureManager,
        ChunkAccess chunk,
        GenerationStep.Carving carvingStep,
        CallbackInfo ci
    ) {
        if (NeuralTerrainConfig.enabled() && NeuralTerrainConfig.disableVanillaCarvers()) {
            ci.cancel();
        }
    }
}
