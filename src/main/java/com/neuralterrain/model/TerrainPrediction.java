package com.neuralterrain.model;

/** Output of the terrain network for one world position. */
public record TerrainPrediction(int surfaceY, float moisture, float temperature) {
}
