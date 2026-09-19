package com.neuralterrain.model;

/** Runtime contract shared by the embedded and external neural networks. */
public interface TerrainModelBackend {
    TerrainPrediction predict(long seed, int worldX, int worldZ, int minY, int maxY);

    default float caveProbability(long seed, int worldX, int y, int worldZ) {
        return 0.0f;
    }

    String name();
}
