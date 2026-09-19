package com.neuralterrain.model;

import com.neuralterrain.config.NeuralTerrainConfig;

/** Built-in fallback network used when no external weights are available. */
public final class EmbeddedTerrainModel implements TerrainModelBackend {
    private static final int INPUTS = 10;
    private static final int HIDDEN = 12;
    private static final int CAVE_INPUTS = 8;
    private static final int CAVE_HIDDEN = 10;

    private static final float[][] HIDDEN_WEIGHTS = {
        { 0.82f, -0.11f, 0.34f, 0.18f, 0.42f, -0.21f, 0.08f, 0.14f, -0.05f, 0.12f },
        {-0.31f,  0.74f, 0.17f, 0.29f, -0.23f, 0.18f, 0.11f, -0.08f, 0.13f, 0.04f },
        { 0.21f,  0.33f, 0.69f, -0.12f, 0.31f, 0.26f, -0.14f, 0.07f, 0.18f, -0.09f },
        {-0.55f,  0.18f, 0.08f, 0.81f, 0.22f, -0.32f, 0.09f, 0.16f, -0.12f, 0.05f },
        { 0.15f, -0.46f, 0.38f, 0.21f, 0.72f, 0.12f, 0.03f, -0.11f, 0.09f, 0.16f },
        { 0.42f,  0.17f, -0.28f, 0.34f, 0.11f, 0.65f, -0.07f, 0.05f, 0.15f, -0.18f },
        {-0.19f,  0.52f, 0.14f, -0.37f, 0.26f, 0.23f, 0.69f, 0.08f, -0.06f, 0.11f },
        { 0.27f, -0.13f, 0.44f, 0.31f, -0.16f, 0.18f, 0.09f, 0.73f, 0.12f, 0.07f },
        { 0.11f,  0.24f, -0.17f, 0.46f, 0.33f, -0.09f, 0.16f, 0.05f, 0.77f, -0.03f },
        {-0.08f,  0.35f, 0.29f, -0.15f, 0.18f, 0.21f, -0.11f, 0.14f, 0.06f, 0.82f },
        { 0.36f,  0.09f, 0.22f, 0.15f, 0.41f, 0.07f, 0.18f, -0.04f, 0.17f, -0.12f },
        {-0.24f,  0.28f, 0.31f, 0.19f, -0.34f, 0.11f, 0.24f, 0.13f, -0.08f, 0.29f }
    };

    private static final float[] HIDDEN_BIAS = {
        0.08f, -0.04f, 0.11f, -0.07f, 0.03f, 0.06f,
        -0.02f, 0.05f, -0.06f, 0.04f, 0.02f, -0.03f
    };

    private static final float[] HEIGHT_WEIGHTS = {
        0.42f, -0.18f, 0.31f, 0.27f, 0.22f, 0.38f,
        0.19f, -0.12f, 0.25f, 0.17f, 0.33f, -0.21f
    };

    private static final float[] MOISTURE_WEIGHTS = {
        -0.12f, 0.28f, 0.17f, -0.25f, 0.09f, 0.21f,
        0.18f, 0.31f, -0.14f, 0.23f, -0.08f, 0.16f
    };

    private static final float[] TEMPERATURE_WEIGHTS = {
        0.21f, 0.07f, -0.19f, 0.22f, 0.14f, -0.11f,
        0.26f, -0.04f, 0.18f, 0.13f, 0.09f, 0.24f
    };

    private static final float[][] CAVE_HIDDEN_WEIGHTS = {
        { 0.72f, -0.18f, 0.31f, 0.44f, 0.16f, -0.62f, 0.27f, -0.11f },
        {-0.41f,  0.67f, 0.28f, -0.22f, 0.54f,  0.18f, -0.33f, 0.21f },
        { 0.36f,  0.22f, -0.73f, 0.19f, 0.12f, 0.51f, 0.42f, -0.28f },
        { 0.18f, -0.51f, 0.47f, 0.63f, -0.37f, 0.26f, 0.14f, 0.45f },
        {-0.64f,  0.29f, 0.15f, -0.39f, 0.71f, -0.14f, 0.38f, 0.17f },
        { 0.23f,  0.48f, 0.52f, -0.18f, -0.55f, 0.33f, -0.21f, 0.69f },
        { 0.55f, -0.37f, 0.08f, 0.24f, 0.49f, 0.58f, -0.44f, -0.16f },
        {-0.27f,  0.16f, 0.61f, 0.35f, 0.28f, -0.43f, 0.73f, 0.12f },
        { 0.43f,  0.34f, -0.26f, 0.57f, -0.18f, 0.41f, 0.19f, 0.62f },
        {-0.12f,  0.59f, 0.39f, -0.47f, 0.33f, 0.22f, 0.52f, -0.36f }
    };

    private static final float[] CAVE_HIDDEN_BIAS = {
        0.04f, -0.08f, 0.03f, 0.06f, -0.02f,
        0.05f, -0.04f, 0.02f, 0.07f, -0.06f
    };

    private static final float[] CAVE_OUTPUT_WEIGHTS = {
        0.83f, 0.66f, 0.72f, 0.58f, 0.69f,
        0.61f, 0.77f, 0.64f, 0.55f, 0.74f
    };

    @Override
    public TerrainPrediction predict(long seed, int worldX, int worldZ, int minY, int maxY) {
        double[] input = features(seed, worldX, worldZ);
        double[] hidden = new double[HIDDEN];
        for (int h = 0; h < HIDDEN; h++) {
            double sum = HIDDEN_BIAS[h];
            for (int i = 0; i < INPUTS; i++) {
                sum += HIDDEN_WEIGHTS[h][i] * input[i];
            }
            hidden[h] = Math.tanh(sum);
        }

        double height = output(hidden, HEIGHT_WEIGHTS, 0.0);
        double moisture = sigmoid(output(hidden, MOISTURE_WEIGHTS, 0.0));
        double temperature = sigmoid(output(hidden, TEMPERATURE_WEIGHTS, 0.0));
        return toPrediction(height, moisture, temperature, minY, maxY, NeuralTerrainConfig.baseHeight(), NeuralTerrainConfig.heightAmplitude());
    }

    @Override
    public float caveProbability(long seed, int worldX, int y, int worldZ) {
        double[] input = caveFeatures(seed, worldX, y, worldZ);
        double[] hidden = new double[CAVE_HIDDEN];
        for (int h = 0; h < CAVE_HIDDEN; h++) {
            double sum = CAVE_HIDDEN_BIAS[h];
            for (int i = 0; i < CAVE_INPUTS; i++) {
                sum += CAVE_HIDDEN_WEIGHTS[h][i] * input[i];
            }
            hidden[h] = Math.tanh(sum);
        }
        return (float) sigmoid(2.0 * output(hidden, CAVE_OUTPUT_WEIGHTS, -0.08) - 1.35);
    }

    @Override
    public String name() {
        return "embedded-mlp";
    }

    static double[] features(long seed, int worldX, int worldZ) {
        double x = worldX;
        double z = worldZ;
        long foldedSeed = seed ^ (seed >>> 33) ^ (seed << 11);
        double seedOffset = foldedSeed * 0.00000017;
        return new double[] {
            Math.sin(x * 0.0041),
            Math.cos(x * 0.0041),
            Math.sin(z * 0.0037),
            Math.cos(z * 0.0037),
            Math.sin((x + z) * 0.0023),
            Math.cos((x - z) * 0.0027),
            Math.sin(x * 0.013 + z * 0.009),
            Math.cos(x * 0.011 - z * 0.015),
            Math.sin((x + seedOffset) * 0.00091),
            Math.cos((z - seedOffset) * 0.00107)
        };
    }

    static double[] caveFeatures(long seed, int worldX, int y, int worldZ) {
        long foldedSeed = seed ^ (seed >>> 29) ^ (seed << 17);
        double seedOffset = foldedSeed * 0.00000011;
        double x = worldX + seedOffset;
        double z = worldZ - seedOffset;
        return new double[] {
            Math.sin(x * 0.035),
            Math.cos(x * 0.035),
            Math.sin(z * 0.037),
            Math.cos(z * 0.037),
            Math.sin(y * 0.12),
            Math.cos(y * 0.12),
            Math.sin((x + z + y) * 0.018),
            Math.cos((x - z + y) * 0.021)
        };
    }

    static TerrainPrediction toPrediction(double height, double moisture, double temperature, int minY, int maxY, double baseHeight, double amplitude) {
        int usableHeight = Math.max(32, maxY - minY - 8);
        int surface = (int) Math.round(baseHeight + height * amplitude);
        surface = Math.max(minY + 6, Math.min(maxY - 4, Math.min(minY + usableHeight, surface)));
        return new TerrainPrediction(surface, (float) moisture, (float) temperature);
    }

    static double output(double[] hidden, float[] weights, double bias) {
        double sum = bias;
        for (int i = 0; i < hidden.length; i++) {
            sum += hidden[i] * weights[i];
        }
        return Math.tanh(sum);
    }

    static double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value * 2.0));
    }
}
