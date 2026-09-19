package com.neuralterrain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.neuralterrain.model.JsonTerrainModel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Verifies the optional external cave-head JSON schema consumed by the runtime. */
public final class JsonModelSmoke {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempFile("neuralterrain-model-smoke", ".json");
        try {
            JsonObject json = new JsonObject();
            json.addProperty("format", 1);
            json.addProperty("inputs", 10);
            json.addProperty("hidden", 1);
            json.add("hidden_weights", matrix(1, 10));
            json.add("hidden_bias", vector(1));
            json.add("height_weights", vector(1));
            json.add("moisture_weights", vector(1));
            json.add("temperature_weights", vector(1));
            json.addProperty("base_height", 68.0);
            json.addProperty("height_amplitude", 46.0);
            json.addProperty("cave_hidden", 1);
            json.add("cave_hidden_weights", matrix(1, 8));
            json.add("cave_hidden_bias", vector(1));
            json.add("cave_weights", vector(1));
            json.addProperty("cave_bias", 0.0);
            Files.writeString(path, json.toString(), StandardCharsets.UTF_8);

            JsonTerrainModel model = JsonTerrainModel.load(path);
            float cave = model.caveProbability(123L, 4, 20, 8);
            if (cave < 0.0f || cave > 1.0f) {
                throw new AssertionError("external cave probability was out of range: " + cave);
            }
            System.out.println("json model smoke ok: cave=" + cave);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static JsonArray vector(int size) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < size; i++) array.add(0.0);
        return array;
    }

    private static JsonArray matrix(int rows, int columns) {
        JsonArray array = new JsonArray();
        for (int row = 0; row < rows; row++) array.add(vector(columns));
        return array;
    }
}
