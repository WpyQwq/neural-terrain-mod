package com.neuralterrain.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neuralterrain.config.NeuralTerrainConfig;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Exports real generated columns as JSONL training samples. */
public final class TerrainSampleExporter {
    private static final Gson GSON = new GsonBuilder().create();

    private TerrainSampleExporter() {
    }

    public static ExportResult export(ServerLevel level, BlockPos center, int radius, int stride) throws IOException {
        Path output = NeuralTerrainConfig.samplesPath();
        Files.createDirectories(output.getParent());
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int count = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
            output,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )) {
            for (int x = minX; x <= maxX; x += stride) {
                for (int z = minZ; z <= maxZ; z += stride) {
                    int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    int surfaceY = Math.max(level.getMinBuildHeight() + 1, height - 1);
                    BlockPos position = new BlockPos(x, surfaceY, z);
                    Biome biome = level.getBiome(position).value();
                    JsonObject sample = new JsonObject();
                    sample.addProperty("seed", level.getSeed());
                    sample.addProperty("x", x);
                    sample.addProperty("z", z);
                    sample.addProperty("surface_y", surfaceY);
                    sample.addProperty("moisture", biome.hasPrecipitation() ? 0.7 : 0.1);
                    sample.addProperty("temperature", normalizeTemperature(biome.getBaseTemperature()));
                    writer.write(GSON.toJson(sample));
                    writer.newLine();
                    count++;
                }
            }
        }
        return new ExportResult(output, count);
    }

    private static double normalizeTemperature(float temperature) {
        return Math.max(0.0, Math.min(1.0, (temperature + 1.0) / 3.0));
    }

    public record ExportResult(Path path, int samples) {
    }
}
