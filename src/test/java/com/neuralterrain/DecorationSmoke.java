package com.neuralterrain;

import com.mojang.serialization.Lifecycle;
import com.neuralterrain.world.NeuralDecoration;
import com.neuralterrain.world.NeuralTerrainRewriter;
import java.lang.reflect.Proxy;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;

/** Executes the custom decoration pass against a real ProtoChunk without a client or EULA. */
public final class DecorationSmoke {
    private static final long SEED = 123456789L;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LevelHeightAccessor height = LevelHeightAccessor.create(-64, 384);
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, height, biomeRegistry(), null);
        NeuralTerrainRewriter.rewrite(chunk, SEED);
        WorldGenLevel level = worldGenLevel(chunk, height);
        NeuralDecoration.decorate(level, chunk, SEED);

        int oreBlocks = 0;
        int vegetationBlocks = 0;
        int ruinBlocks = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y < 384; y++) {
                    var block = chunk.getBlockState(new BlockPos(x, y, z)).getBlock();
                    if (block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.COPPER_ORE
                        || block == Blocks.GOLD_ORE || block == Blocks.REDSTONE_ORE || block == Blocks.LAPIS_ORE
                        || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                        || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.DEEPSLATE_COPPER_ORE
                        || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE
                        || block == Blocks.DEEPSLATE_LAPIS_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                        oreBlocks++;
                    }
                    if (block == Blocks.OAK_LOG || block == Blocks.OAK_LEAVES || block == Blocks.GRASS
                        || block == Blocks.DANDELION || block == Blocks.POPPY) {
                        vegetationBlocks++;
                    }
                    if (block == Blocks.STONE_BRICKS || block == Blocks.MOSSY_STONE_BRICKS) {
                        ruinBlocks++;
                    }
                }
            }
        }
        if (oreBlocks == 0 || vegetationBlocks == 0) {
            throw new AssertionError("decoration pass placed no ores or vegetation: ores=" + oreBlocks + ", vegetation=" + vegetationBlocks);
        }
        System.out.println("decoration smoke ok: ores=" + oreBlocks + ", vegetation=" + vegetationBlocks + ", ruin=" + ruinBlocks);
    }

    private static WorldGenLevel worldGenLevel(ProtoChunk chunk, LevelHeightAccessor height) {
        return (WorldGenLevel) Proxy.newProxyInstance(
            WorldGenLevel.class.getClassLoader(),
            new Class<?>[] {WorldGenLevel.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("getSeed")) return SEED;
                if (name.equals("getMinBuildHeight")) return height.getMinBuildHeight();
                if (name.equals("getMaxBuildHeight")) return height.getMinBuildHeight() + height.getHeight();
                if (name.equals("getBlockState")) return chunk.getBlockState((BlockPos) args[0]);
                if (name.equals("isEmptyBlock")) return chunk.getBlockState((BlockPos) args[0]).isAir();
                if (name.equals("setBlock")) {
                    chunk.setBlockState((BlockPos) args[0], (net.minecraft.world.level.block.state.BlockState) args[1], false);
                    return true;
                }
                if (name.equals("getHeight")) {
                    return chunk.getHeight((Heightmap.Types) args[0], (int) args[1], (int) args[2]);
                }
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                if (method.getReturnType() == long.class) return 0L;
                return null;
            }
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
