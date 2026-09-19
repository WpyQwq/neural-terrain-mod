package com.neuralterrain.world;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.model.NeuralTerrainModel;
import com.neuralterrain.model.TerrainPrediction;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Model-driven post-decoration that complements the normal biome feature pipeline. */
public final class NeuralDecoration {
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState GRASS_BLOCK = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState OAK_LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS.defaultBlockState();
    private static final BlockState DANDELION = Blocks.DANDELION.defaultBlockState();
    private static final BlockState POPPY = Blocks.POPPY.defaultBlockState();

    private NeuralDecoration() {
    }

    public static void decorate(WorldGenLevel level, ChunkAccess chunk, long seed) {
        if (!NeuralTerrainConfig.enabled() || !NeuralTerrainConfig.generateNeuralDecorations()) {
            return;
        }
        long decorationSeed = mix(seed, chunk.getPos().x, chunk.getPos().z);
        Random random = new Random(decorationSeed);
        if (NeuralTerrainConfig.generateNeuralOres()) {
            decorateOres(level, chunk, seed, random);
        }
        if (NeuralTerrainConfig.generateNeuralVegetation()) {
            decorateVegetation(level, chunk, seed, random);
        }
        if (NeuralTerrainConfig.generateNeuralStructures()) {
            decorateRuin(level, chunk, seed, random);
        }
    }

    private static void decorateOres(WorldGenLevel level, ChunkAccess chunk, long seed, Random random) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        TerrainPrediction center = NeuralTerrainModel.predict(seed, startX + 8, startZ + 8, minY, maxY);
        int extraTries = 2 + Math.round(center.moisture() * 3.0f);
        for (int i = 0; i < extraTries; i++) {
            placeVein(level, random, startX, startZ, minY, maxY, Blocks.COAL_ORE.defaultBlockState(), -16, 96, 8);
            placeVein(level, random, startX, startZ, minY, maxY, Blocks.IRON_ORE.defaultBlockState(), -32, 64, 6);
        }
        for (int i = 0; i < 1 + Math.round(center.temperature()); i++) {
            placeVein(level, random, startX, startZ, minY, maxY, Blocks.COPPER_ORE.defaultBlockState(), -16, 56, 7);
            placeVein(level, random, startX, startZ, minY, maxY, Blocks.GOLD_ORE.defaultBlockState(), -32, 32, 5);
        }
        placeVein(level, random, startX, startZ, minY, maxY, Blocks.REDSTONE_ORE.defaultBlockState(), -64, 16, 5);
        placeVein(level, random, startX, startZ, minY, maxY, Blocks.LAPIS_ORE.defaultBlockState(), -32, 32, 4);
        if (center.moisture() > 0.38f) {
            placeVein(level, random, startX, startZ, minY, maxY, Blocks.DIAMOND_ORE.defaultBlockState(), -64, 12, 2);
        }
    }

    private static void placeVein(
        WorldGenLevel level,
        Random random,
        int startX,
        int startZ,
        int minY,
        int maxY,
        BlockState ore,
        int minOreY,
        int maxOreY,
        int size
    ) {
        int x = startX + random.nextInt(16);
        int z = startZ + random.nextInt(16);
        int y = minOreY + random.nextInt(Math.max(1, maxOreY - minOreY + 1));
        if (y < minY || y >= maxY) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < size; i++) {
            pos.set(
                Math.max(startX, Math.min(startX + 15, x + random.nextInt(5) - 2)),
                Math.max(minY, Math.min(maxY - 1, y + random.nextInt(5) - 2)),
                Math.max(startZ, Math.min(startZ + 15, z + random.nextInt(5) - 2))
            );
            BlockState current = level.getBlockState(pos);
            if (current.is(STONE.getBlock()) || current.is(DEEPSLATE.getBlock())) {
                level.setBlock(pos, current.is(DEEPSLATE.getBlock()) ? deepslateVariant(ore) : ore, 2);
            }
        }
    }

    private static BlockState deepslateVariant(BlockState ore) {
        if (ore.is(Blocks.COAL_ORE)) return Blocks.DEEPSLATE_COAL_ORE.defaultBlockState();
        if (ore.is(Blocks.IRON_ORE)) return Blocks.DEEPSLATE_IRON_ORE.defaultBlockState();
        if (ore.is(Blocks.COPPER_ORE)) return Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState();
        if (ore.is(Blocks.GOLD_ORE)) return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
        if (ore.is(Blocks.REDSTONE_ORE)) return Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState();
        if (ore.is(Blocks.LAPIS_ORE)) return Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState();
        if (ore.is(Blocks.DIAMOND_ORE)) return Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
        return ore;
    }

    private static void decorateVegetation(WorldGenLevel level, ChunkAccess chunk, long seed, Random random) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = startX + 2 + random.nextInt(12);
            int z = startZ + 2 + random.nextInt(12);
            TerrainPrediction prediction = NeuralTerrainModel.predict(seed, x, z, minY, maxY);
            int y = surfaceY(level, x, z);
            if (y <= minY || y + 6 >= maxY) {
                continue;
            }
            BlockPos ground = new BlockPos(x, y, z);
            BlockState groundState = level.getBlockState(ground);
            if (!groundState.is(GRASS_BLOCK.getBlock()) && !groundState.is(DIRT.getBlock())) {
                continue;
            }
            if (prediction.temperature() > 0.78f && prediction.moisture() < 0.32f) {
                continue;
            }
            if (prediction.moisture() > 0.48f && random.nextFloat() < 0.25f) {
                placeTree(level, x, y + 1, z, maxY);
            } else {
                BlockPos plant = ground.above();
                if (level.isEmptyBlock(plant)) {
                    level.setBlock(plant, random.nextBoolean() ? GRASS : (prediction.temperature() > 0.55f ? DANDELION : POPPY), 2);
                }
            }
        }
    }

    private static void placeTree(WorldGenLevel level, int x, int y, int z, int maxY) {
        for (int dy = 0; dy < 4; dy++) {
            BlockPos trunk = new BlockPos(x, y + dy, z);
            if (!level.isEmptyBlock(trunk)) {
                return;
            }
        }
        for (int dy = 0; dy < 4; dy++) {
            level.setBlock(new BlockPos(x, y + dy, z), OAK_LOG, 2);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 2; dy <= 4; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) <= (dy == 4 ? 1 : 2)) {
                        BlockPos leaves = new BlockPos(x + dx, y + dy, z + dz);
                        if (leaves.getY() < maxY && level.isEmptyBlock(leaves)) {
                            level.setBlock(leaves, OAK_LEAVES, 2);
                        }
                    }
                }
            }
        }
    }

    private static void decorateRuin(WorldGenLevel level, ChunkAccess chunk, long seed, Random random) {
        int centerX = chunk.getPos().getMinBlockX() + 8;
        int centerZ = chunk.getPos().getMinBlockZ() + 8;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        TerrainPrediction prediction = NeuralTerrainModel.predict(seed, centerX, centerZ, minY, maxY);
        if (prediction.surfaceY() < NeuralTerrainConfig.seaLevel()
            || prediction.moisture() > 0.78f
            || random.nextFloat() > 0.07f) {
            return;
        }
        int groundY = surfaceY(level, centerX, centerZ);
        if (groundY <= minY + 1 || groundY + 5 >= maxY) {
            return;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = new BlockPos(centerX + dx, groundY, centerZ + dz);
                level.setBlock(floor, (Math.abs(dx) + Math.abs(dz)) % 2 == 0 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    level.setBlock(floor.above(), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                    if ((dx + dz) % 2 == 0) {
                        level.setBlock(floor.above(2), Blocks.COBBLESTONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static int surfaceY(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        while (y > level.getMinBuildHeight() && level.isEmptyBlock(new BlockPos(x, y, z))) {
            y--;
        }
        return y;
    }

    private static long mix(long seed, int chunkX, int chunkZ) {
        long value = seed + chunkX * 341873128712L + chunkZ * 132897987541L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }
}
