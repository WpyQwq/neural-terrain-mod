package com.neuralterrain.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads a small dense MLP exported by tools/train_neural_model.py. */
public final class JsonTerrainModel implements TerrainModelBackend {
    private static final EmbeddedTerrainModel EMBEDDED_CAVE_FALLBACK = new EmbeddedTerrainModel();
    private final int inputCount;
    private final int hiddenCount;
    private final double[][] hiddenWeights;
    private final double[] hiddenBias;
    private final double[] heightWeights;
    private final double[] moistureWeights;
    private final double[] temperatureWeights;
    private final double heightBias;
    private final double moistureBias;
    private final double temperatureBias;
    private final double baseHeight;
    private final double heightAmplitude;
    private final String sourceName;
    private final int caveHiddenCount;
    private final double[][] caveHiddenWeights;
    private final double[] caveHiddenBias;
    private final double[] caveWeights;
    private final double caveBias;

    private JsonTerrainModel(JsonObject json, String sourceName) {
        if (json.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("unsupported model format");
        }
        inputCount = json.get("inputs").getAsInt();
        hiddenCount = json.get("hidden").getAsInt();
        if (inputCount != 10 || hiddenCount < 1 || hiddenCount > 256) {
            throw new IllegalArgumentException("expected 10 inputs and 1..256 hidden units");
        }
        hiddenWeights = matrix(json.getAsJsonArray("hidden_weights"), hiddenCount, inputCount);
        hiddenBias = vector(json.getAsJsonArray("hidden_bias"), hiddenCount);
        heightWeights = vector(json.getAsJsonArray("height_weights"), hiddenCount);
        moistureWeights = vector(json.getAsJsonArray("moisture_weights"), hiddenCount);
        temperatureWeights = vector(json.getAsJsonArray("temperature_weights"), hiddenCount);
        heightBias = json.has("height_bias") ? json.get("height_bias").getAsDouble() : 0.0;
        moistureBias = json.has("moisture_bias") ? json.get("moisture_bias").getAsDouble() : 0.0;
        temperatureBias = json.has("temperature_bias") ? json.get("temperature_bias").getAsDouble() : 0.0;
        baseHeight = json.has("base_height") ? json.get("base_height").getAsDouble() : 68.0;
        heightAmplitude = json.has("height_amplitude") ? json.get("height_amplitude").getAsDouble() : 46.0;
        boolean hasCaveNetwork = json.has("cave_hidden")
            && json.has("cave_hidden_weights")
            && json.has("cave_hidden_bias")
            && json.has("cave_weights");
        if (hasCaveNetwork) {
            caveHiddenCount = json.get("cave_hidden").getAsInt();
            if (caveHiddenCount < 1 || caveHiddenCount > 128) {
                throw new IllegalArgumentException("cave hidden units must be in 1..128");
            }
            caveHiddenWeights = matrix(json.getAsJsonArray("cave_hidden_weights"), caveHiddenCount, 8);
            caveHiddenBias = vector(json.getAsJsonArray("cave_hidden_bias"), caveHiddenCount);
            caveWeights = vector(json.getAsJsonArray("cave_weights"), caveHiddenCount);
            caveBias = json.has("cave_bias") ? json.get("cave_bias").getAsDouble() : 0.0;
        } else {
            caveHiddenCount = 0;
            caveHiddenWeights = null;
            caveHiddenBias = null;
            caveWeights = null;
            caveBias = 0.0;
        }
        this.sourceName = sourceName;
    }

    public static JsonTerrainModel load(Path path) throws Exception {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return new JsonTerrainModel(JsonParser.parseString(text).getAsJsonObject(), path.getFileName().toString());
    }

    @Override
    public TerrainPrediction predict(long seed, int worldX, int worldZ, int minY, int maxY) {
        double[] input = EmbeddedTerrainModel.features(seed, worldX, worldZ);
        double[] hidden = new double[hiddenCount];
        for (int h = 0; h < hiddenCount; h++) {
            double sum = hiddenBias[h];
            for (int i = 0; i < inputCount; i++) {
                sum += hiddenWeights[h][i] * input[i];
            }
            hidden[h] = Math.tanh(sum);
        }
        double height = Math.tanh(dot(hidden, heightWeights) + heightBias);
        double moisture = EmbeddedTerrainModel.sigmoid(dot(hidden, moistureWeights) + moistureBias);
        double temperature = EmbeddedTerrainModel.sigmoid(dot(hidden, temperatureWeights) + temperatureBias);
        return EmbeddedTerrainModel.toPrediction(height, moisture, temperature, minY, maxY, baseHeight, heightAmplitude);
    }

    @Override
    public float caveProbability(long seed, int worldX, int y, int worldZ) {
        if (caveHiddenCount == 0) {
            return EMBEDDED_CAVE_FALLBACK.caveProbability(seed, worldX, y, worldZ);
        }
        double[] input = EmbeddedTerrainModel.caveFeatures(seed, worldX, y, worldZ);
        double[] hidden = new double[caveHiddenCount];
        for (int h = 0; h < caveHiddenCount; h++) {
            double sum = caveHiddenBias[h];
            for (int i = 0; i < input.length; i++) {
                sum += caveHiddenWeights[h][i] * input[i];
            }
            hidden[h] = Math.tanh(sum);
        }
        return (float) EmbeddedTerrainModel.sigmoid(2.0 * dot(hidden, caveWeights) + caveBias);
    }

    @Override
    public String name() {
        return "json:" + sourceName;
    }

    private static double dot(double[] left, double[] right) {
        double sum = 0.0;
        for (int i = 0; i < left.length; i++) {
            sum += left[i] * right[i];
        }
        return sum;
    }

    private static double[] vector(JsonArray array, int expected) {
        if (array.size() != expected) {
            throw new IllegalArgumentException("vector length mismatch");
        }
        double[] result = new double[expected];
        for (int i = 0; i < expected; i++) {
            result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

    private static double[][] matrix(JsonArray array, int rows, int columns) {
        if (array.size() != rows) {
            throw new IllegalArgumentException("matrix row count mismatch");
        }
        double[][] result = new double[rows][columns];
        for (int row = 0; row < rows; row++) {
            JsonArray values = array.get(row).getAsJsonArray();
            if (values.size() != columns) {
                throw new IllegalArgumentException("matrix column count mismatch");
            }
            for (int column = 0; column < columns; column++) {
                result[row][column] = values.get(column).getAsDouble();
            }
        }
        return result;
    }
}
