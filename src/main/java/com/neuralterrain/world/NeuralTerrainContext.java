package com.neuralterrain.world;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Associates a live server world's seed with its generator instance. */
public final class NeuralTerrainContext {
    private static final Map<ChunkGenerator, Long> SEEDS = Collections.synchronizedMap(new WeakHashMap<>());

    private NeuralTerrainContext() {
    }

    public static void register(ChunkGenerator generator, long seed) {
        SEEDS.put(generator, seed);
    }

    public static long seedFor(ChunkGenerator generator) {
        return SEEDS.getOrDefault(generator, 0L);
    }
}
