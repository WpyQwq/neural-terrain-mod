package com.neuralterrain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small bounded cache shared by the multiple Minecraft generation stages. */
final class TerrainPredictionCache {
    private static final int MAX_ENTRIES = 32_768;
    private final Map<Long, TerrainPrediction> entries = new LinkedHashMap<>(1024, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, TerrainPrediction> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    synchronized TerrainPrediction get(long seed, int worldX, int worldZ, int minY, int maxY) {
        return entries.get(key(seed, worldX, worldZ, minY, maxY));
    }

    synchronized void put(long seed, int worldX, int worldZ, int minY, int maxY, TerrainPrediction prediction) {
        entries.put(key(seed, worldX, worldZ, minY, maxY), prediction);
    }

    synchronized void clear() {
        entries.clear();
    }

    private static long key(long seed, int worldX, int worldZ, int minY, int maxY) {
        long value = seed;
        value ^= (long) worldX * 0x9E3779B185EBCA87L;
        value ^= (long) worldZ * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) minY * 0x165667B19E3779F9L;
        value ^= (long) maxY * 0x85EBCA77C2B2AE63L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }
}
