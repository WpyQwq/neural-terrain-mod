package com.neuralterrain;

import com.mojang.serialization.Lifecycle;
import com.neuralterrain.model.TerrainPrediction;
import com.neuralterrain.world.NeuralTerrainBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import java.util.List;
import net.minecraft.resources.ResourceKey;

/** Verifies that model climate signals map to the intended vanilla biome keys. */
public final class BiomeSelectionSmoke {
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MappedRegistry<Biome> registry = new MappedRegistry<>(Registries.BIOME, Lifecycle.stable());
        for (ResourceKey<Biome> key : List.of(
            Biomes.OCEAN, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS, Biomes.DESERT,
            Biomes.SAVANNA, Biomes.JUNGLE, Biomes.SWAMP, Biomes.TAIGA,
            Biomes.FOREST, Biomes.PLAINS
        )) {
            Registry.register(registry, key, biome());
        }
        assertKey(registry, new TerrainPrediction(40, 0.5f, 0.5f), Biomes.OCEAN);
        assertKey(registry, new TerrainPrediction(80, 0.7f, 0.1f), Biomes.SNOWY_TAIGA);
        assertKey(registry, new TerrainPrediction(80, 0.1f, 0.9f), Biomes.DESERT);
        assertKey(registry, new TerrainPrediction(80, 0.8f, 0.75f), Biomes.JUNGLE);
        assertKey(registry, new TerrainPrediction(80, 0.4f, 0.5f), Biomes.PLAINS);
        System.out.println("biome selection smoke ok");
    }

    private static void assertKey(Registry<Biome> registry, TerrainPrediction prediction, net.minecraft.resources.ResourceKey<Biome> expected) {
        Holder<Biome> actual = NeuralTerrainBiomes.select(registry, prediction);
        if (actual.unwrapKey().isEmpty() || !actual.unwrapKey().get().equals(expected)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static Biome biome() {
        return new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .fogColor(12638463)
                .waterColor(4159204)
                .waterFogColor(329011)
                .skyColor(7907327)
                .build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .generationSettings(BiomeGenerationSettings.EMPTY)
            .build();
    }
}
