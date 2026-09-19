package com.neuralterrain.world;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.model.NeuralTerrainModel;
import com.neuralterrain.model.TerrainPrediction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Converts the network's continuous prediction into Minecraft block columns. */
public final class NeuralTerrainRewriter {
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();

    private NeuralTerrainRewriter() {
    }

    public static ChunkAccess rewrite(ChunkAccess chunk, long seed) {
        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight();
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                TerrainPrediction prediction = NeuralTerrainModel.predict(seed, worldX, worldZ, minY, maxY);
                int surfaceY = prediction.surfaceY();
                int seaLevel = NeuralTerrainConfig.seaLevel();
                boolean beach = surfaceY <= seaLevel + NeuralTerrainConfig.beachMargin();
                boolean dry = prediction.temperature() > 0.72f && prediction.moisture() < 0.42f;
                BlockState surfaceState = beach || dry ? SAND : GRASS;
                int columnTop = NeuralTerrainConfig.generateWater() && surfaceY < seaLevel ? seaLevel : surfaceY;
                columnTop = Math.min(maxY - 1, columnTop);

                for (int y = minY; y <= columnTop; y++) {
                    BlockState state;
                    if (y < minY + NeuralTerrainConfig.bedrockLayers()) {
                        state = BEDROCK;
                    } else if (y < surfaceY - 3) {
                        boolean cave = NeuralTerrainConfig.generateNeuralCaves()
                            && y > minY + NeuralTerrainConfig.bedrockLayers() + 4
                            && y < surfaceY - 6
                            && NeuralTerrainModel.caveProbability(seed, worldX, y, worldZ)
                                >= NeuralTerrainConfig.caveThreshold();
                        state = cave ? AIR : (y < 0 ? DEEPSLATE : STONE);
                    } else if (y < surfaceY) {
                        state = beach || dry ? SAND : DIRT;
                    } else if (y == surfaceY) {
                        state = surfaceState;
                    } else if (NeuralTerrainConfig.generateWater() && y <= seaLevel && surfaceY < seaLevel) {
                        state = WATER;
                    } else {
                        state = AIR;
                    }
                    mutable.set(worldX, y, worldZ);
                    chunk.setBlockState(mutable, state, false);
                }

                // Keep heightmaps coherent for later surface/decoration stages.
                int heightmapY = columnTop;
                BlockState heightmapState = surfaceY < seaLevel && NeuralTerrainConfig.generateWater() ? WATER : surfaceState;
                chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
                    .update(localX, heightmapY, localZ, heightmapState);
            }
        }
        return chunk;
    }
}
