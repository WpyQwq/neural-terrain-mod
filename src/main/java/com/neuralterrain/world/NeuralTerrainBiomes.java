package com.neuralterrain.world;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.model.TerrainPrediction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/** Maps the neural climate head to a vanilla biome so later generation stages stay coherent. */
public final class NeuralTerrainBiomes {
    private NeuralTerrainBiomes() {
    }

    public static Holder<Biome> select(Registry<Biome> registry, TerrainPrediction prediction) {
        if (prediction.surfaceY() < NeuralTerrainConfig.seaLevel()) {
            return holder(registry, Biomes.OCEAN);
        }
        float moisture = prediction.moisture();
        float temperature = prediction.temperature();
        if (temperature < 0.24f) {
            return holder(registry, moisture > 0.55f ? Biomes.SNOWY_TAIGA : Biomes.SNOWY_PLAINS);
        }
        if (temperature > 0.78f && moisture < 0.32f) {
            return holder(registry, Biomes.DESERT);
        }
        if (temperature > 0.68f && moisture < 0.48f) {
            return holder(registry, Biomes.SAVANNA);
        }
        if (temperature > 0.72f && moisture > 0.68f) {
            return holder(registry, Biomes.JUNGLE);
        }
        if (moisture > 0.78f) {
            return holder(registry, Biomes.SWAMP);
        }
        if (moisture > 0.56f) {
            return holder(registry, temperature < 0.45f ? Biomes.TAIGA : Biomes.FOREST);
        }
        return holder(registry, Biomes.PLAINS);
    }

    private static Holder<Biome> holder(Registry<Biome> registry, net.minecraft.resources.ResourceKey<Biome> key) {
        return registry.getHolderOrThrow(key);
    }
}
