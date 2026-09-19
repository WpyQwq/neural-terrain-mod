package com.neuralterrain;

import com.neuralterrain.model.NeuralTerrainModel;
import com.neuralterrain.model.TerrainPrediction;
import com.neuralterrain.world.NeuralTerrainRewriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** Headless generation test that does not require accepting the Minecraft EULA. */
public final class ChunkGenerationSmoke {
    private static final long SEED = 123456789L;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        ProtoChunk first = newChunk(height);
        ProtoChunk second = newChunk(height);
        NeuralTerrainRewriter.rewrite(first, SEED);
        NeuralTerrainRewriter.rewrite(second, SEED);

        TerrainPrediction prediction = NeuralTerrainModel.predict(SEED, 0, 0, -64, 320);
        BlockPos base = new BlockPos(0, -64, 0);
        BlockPos surface = new BlockPos(0, prediction.surfaceY(), 0);
        if (!first.getBlockState(base).is(Blocks.BEDROCK)) {
            throw new AssertionError("bottom layer is not bedrock");
        }
        if (!first.getBlockState(surface).is(Blocks.GRASS_BLOCK)
            && !first.getBlockState(surface).is(Blocks.SAND)) {
            throw new AssertionError("predicted surface is not a terrain block: " + first.getBlockState(surface));
        }
        if (first.getBlockState(surface).getBlock() != second.getBlockState(surface).getBlock()) {
            throw new AssertionError("same seed and coordinates produced different terrain");
        }
        int heightmap = first.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 0, 0);
        if (heightmap <= -64) {
            throw new AssertionError("heightmap was not updated");
        }
        float caveA = NeuralTerrainModel.caveProbability(SEED, 3, 20, 7);
        float caveB = NeuralTerrainModel.caveProbability(SEED, 3, 20, 7);
        if (caveA != caveB || caveA < 0.0f || caveA > 1.0f) {
            throw new AssertionError("cave network is not deterministic or bounded");
        }
        int caveAir = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                TerrainPrediction column = NeuralTerrainModel.predict(SEED, x, z, -64, 320);
                for (int y = -64 + 7; y < column.surfaceY() - 6; y++) {
                    if (first.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        caveAir++;
                    }
                }
            }
        }
        if (caveAir == 0) {
            throw new AssertionError("cave network did not carve any deep air blocks");
        }
        System.out.println("chunk smoke ok: surface=" + prediction.surfaceY() + ", heightmap=" + heightmap + ", cave_air=" + caveAir);
    }

    private static ProtoChunk newChunk(LevelHeightAccessor height) {
        return new ProtoChunk(
            new ChunkPos(0, 0),
            UpgradeData.EMPTY,
            height,
            biomeRegistry(),
            null
        );
    }

    private static Registry<Biome> biomeRegistry() {
        MappedRegistry<Biome> registry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        Biome biome = new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(7907327)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build();
        Registry.register(registry, Biomes.PLAINS, biome);
        return registry;
    }
}
