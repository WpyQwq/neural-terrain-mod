package com.neuralterrain.model;

import com.neuralterrain.NeuralTerrainMod;
import java.nio.file.Files;
import java.nio.file.Path;

/** Selects the external JSON model when available and otherwise uses the embedded MLP. */
public final class NeuralTerrainModel {
    private static volatile TerrainModelBackend active = new EmbeddedTerrainModel();
    private static final TerrainPredictionCache CACHE = new TerrainPredictionCache();

    private NeuralTerrainModel() {
    }

    public static void load(Path modelPath) {
        CACHE.clear();
        if (modelPath != null && Files.isRegularFile(modelPath)) {
            try {
                active = JsonTerrainModel.load(modelPath);
                NeuralTerrainMod.LOGGER.info("Loaded terrain model backend {}", active.name());
                return;
            } catch (Exception exception) {
                NeuralTerrainMod.LOGGER.error("Could not load terrain model {}, using embedded fallback", modelPath, exception);
            }
        }
        active = new EmbeddedTerrainModel();
        NeuralTerrainMod.LOGGER.info("Using terrain model backend {}", active.name());
    }

    public static TerrainPrediction predict(long seed, int worldX, int worldZ, int minY, int maxY) {
        TerrainPrediction cached = CACHE.get(seed, worldX, worldZ, minY, maxY);
        if (cached != null) {
            return cached;
        }
        TerrainPrediction prediction = active.predict(seed, worldX, worldZ, minY, maxY);
        CACHE.put(seed, worldX, worldZ, minY, maxY, prediction);
        return prediction;
    }

    public static float caveProbability(long seed, int worldX, int y, int worldZ) {
        return active.caveProbability(seed, worldX, y, worldZ);
    }

    public static String backendName() {
        return active.name();
    }
}
