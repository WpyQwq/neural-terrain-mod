package com.neuralterrain.tools;

import com.google.gson.JsonObject;
import com.neuralterrain.config.NeuralTerrainConfig;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Exports interior air labels from an existing world for training the cave head. */
public final class CaveSampleExporter {
    private CaveSampleExporter() {
    }

    public static ExportResult export(ServerLevel level, BlockPos center, int radius, int stride, int minY, int maxY) throws Exception {
        Path path = NeuralTerrainConfig.caveSamplesPath();
        Files.createDirectories(path.getParent());
        long seed = level.getSeed();
        int worldMinY = Math.max(level.getMinBuildHeight() + NeuralTerrainConfig.bedrockLayers() + 5, minY);
        int worldMaxY = Math.min(level.getMaxBuildHeight() - 1, maxY);
        int samples = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x += stride) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += stride) {
                    int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    int top = Math.min(worldMaxY, surface - 6);
                    for (int y = worldMinY; y <= top; y += stride) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            continue;
                        }
                        int solidNeighbors = 0;
                        for (BlockPos neighbor : new BlockPos[] {pos.north(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below()}) {
                            if (!level.getBlockState(neighbor).isAir()) {
                                solidNeighbors++;
                            }
                        }
                        if (level.getBlockState(pos).isAir() || solidNeighbors >= 4) {
                            JsonObject row = new JsonObject();
                            row.addProperty("seed", seed);
                            row.addProperty("x", x);
                            row.addProperty("y", y);
                            row.addProperty("z", z);
                            row.addProperty("cave", level.getBlockState(pos).isAir() ? 1.0 : 0.0);
                            writer.write(row.toString());
                            writer.newLine();
                            samples++;
                        }
                    }
                }
            }
        }
        return new ExportResult(path, samples);
    }

    public record ExportResult(Path path, int samples) {
    }
}
