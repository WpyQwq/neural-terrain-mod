package com.neuralterrain.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neuralterrain.NeuralTerrainMod;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Small dependency-free JSON configuration for the generation backend. */
public final class NeuralTerrainConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "neuralterrain.json";

    private static volatile boolean enabled = true;
    private static volatile int seaLevel = 63;
    private static volatile double baseHeight = 68.0;
    private static volatile double heightAmplitude = 46.0;
    private static volatile int beachMargin = 2;
    private static volatile int bedrockLayers = 2;
    private static volatile boolean generateWater = true;
    private static volatile boolean generateNeuralBiomes = true;
    private static volatile boolean generateNeuralCaves = true;
    private static volatile boolean disableVanillaCarvers = true;
    private static volatile boolean generateNeuralDecorations = true;
    private static volatile boolean generateNeuralOres = true;
    private static volatile boolean generateNeuralVegetation = true;
    private static volatile boolean generateNeuralStructures = true;
    private static volatile double caveThreshold = 0.76;
    private static volatile String modelFile = "neuralterrain-model.json";

    private NeuralTerrainConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, GSON.toJson(defaults()), StandardCharsets.UTF_8);
                NeuralTerrainMod.LOGGER.info("Created default configuration at {}", path);
            }
            JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            enabled = bool(json, "enabled", true);
            seaLevel = integer(json, "sea_level", 63, -64, 320);
            baseHeight = number(json, "base_height", 68.0, -32.0, 320.0);
            heightAmplitude = number(json, "height_amplitude", 46.0, 1.0, 256.0);
            beachMargin = integer(json, "beach_margin", 2, 0, 32);
            bedrockLayers = integer(json, "bedrock_layers", 2, 1, 8);
            generateWater = bool(json, "generate_water", true);
            generateNeuralBiomes = bool(json, "generate_neural_biomes", true);
            generateNeuralCaves = bool(json, "generate_neural_caves", true);
            disableVanillaCarvers = bool(json, "disable_vanilla_carvers", true);
            generateNeuralDecorations = bool(json, "generate_neural_decorations", true);
            generateNeuralOres = bool(json, "generate_neural_ores", true);
            generateNeuralVegetation = bool(json, "generate_neural_vegetation", true);
            generateNeuralStructures = bool(json, "generate_neural_structures", true);
            caveThreshold = number(json, "cave_threshold", 0.76, 0.5, 0.99);
            modelFile = text(json, "model_file", "neuralterrain-model.json");
        } catch (Exception exception) {
            NeuralTerrainMod.LOGGER.error("Could not load {}, using safe defaults", path, exception);
            resetToDefaults();
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int seaLevel() {
        return seaLevel;
    }

    public static double baseHeight() {
        return baseHeight;
    }

    public static double heightAmplitude() {
        return heightAmplitude;
    }

    public static int beachMargin() {
        return beachMargin;
    }

    public static int bedrockLayers() {
        return bedrockLayers;
    }

    public static boolean generateWater() {
        return generateWater;
    }

    public static boolean generateNeuralBiomes() {
        return generateNeuralBiomes;
    }

    public static boolean generateNeuralCaves() {
        return generateNeuralCaves;
    }

    public static boolean disableVanillaCarvers() {
        return disableVanillaCarvers;
    }

    public static boolean generateNeuralDecorations() {
        return generateNeuralDecorations;
    }

    public static boolean generateNeuralOres() {
        return generateNeuralOres;
    }

    public static boolean generateNeuralVegetation() {
        return generateNeuralVegetation;
    }

    public static boolean generateNeuralStructures() {
        return generateNeuralStructures;
    }

    public static double caveThreshold() {
        return caveThreshold;
    }

    public static Path modelPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(modelFile).normalize();
    }

    public static Path samplesPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("neuralterrain-samples.jsonl").normalize();
    }

    public static Path caveSamplesPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("neuralterrain-cave-samples.jsonl").normalize();
    }

    public static String describe() {
        return "enabled=" + enabled
            + ", sea_level=" + seaLevel
            + ", base_height=" + baseHeight
            + ", height_amplitude=" + heightAmplitude
            + ", beach_margin=" + beachMargin
            + ", bedrock_layers=" + bedrockLayers
            + ", generate_water=" + generateWater
            + ", generate_neural_biomes=" + generateNeuralBiomes
            + ", generate_neural_caves=" + generateNeuralCaves
            + ", disable_vanilla_carvers=" + disableVanillaCarvers
            + ", generate_neural_decorations=" + generateNeuralDecorations
            + ", generate_neural_ores=" + generateNeuralOres
            + ", generate_neural_vegetation=" + generateNeuralVegetation
            + ", generate_neural_structures=" + generateNeuralStructures
            + ", cave_threshold=" + caveThreshold
            + ", model_file=" + modelFile;
    }

    private static JsonObject defaults() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", true);
        json.addProperty("sea_level", 63);
        json.addProperty("base_height", 68.0);
        json.addProperty("height_amplitude", 46.0);
        json.addProperty("beach_margin", 2);
        json.addProperty("bedrock_layers", 2);
        json.addProperty("generate_water", true);
        json.addProperty("generate_neural_biomes", true);
        json.addProperty("generate_neural_caves", true);
        json.addProperty("disable_vanilla_carvers", true);
        json.addProperty("generate_neural_decorations", true);
        json.addProperty("generate_neural_ores", true);
        json.addProperty("generate_neural_vegetation", true);
        json.addProperty("generate_neural_structures", true);
        json.addProperty("cave_threshold", 0.76);
        json.addProperty("model_file", "neuralterrain-model.json");
        return json;
    }

    private static void resetToDefaults() {
        enabled = true;
        seaLevel = 63;
        baseHeight = 68.0;
        heightAmplitude = 46.0;
        beachMargin = 2;
        bedrockLayers = 2;
        generateWater = true;
        generateNeuralBiomes = true;
        generateNeuralCaves = true;
        disableVanillaCarvers = true;
        generateNeuralDecorations = true;
        generateNeuralOres = true;
        generateNeuralVegetation = true;
        generateNeuralStructures = true;
        caveThreshold = 0.76;
        modelFile = "neuralterrain-model.json";
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static String text(JsonObject json, String key, String fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        String value = json.get(key).getAsString().trim();
        return value.isEmpty() || value.contains("..") || value.contains("\\") || value.contains("/") ? fallback : value;
    }

    private static int integer(JsonObject json, String key, int fallback, int min, int max) {
        int value = json.has(key) ? json.get(key).getAsInt() : fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static double number(JsonObject json, String key, double fallback, double min, double max) {
        double value = json.has(key) ? json.get(key).getAsDouble() : fallback;
        return Math.max(min, Math.min(max, value));
    }
}
