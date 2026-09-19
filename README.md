# Neural Terrain

Fabric 1.20.1 mod that replaces the vanilla noise heightfield with a deterministic feed-forward neural terrain model.

## Current implementation

- Fabric Loader and Fabric API for Minecraft 1.20.1.
- Mixin into `NoiseBasedChunkGenerator.fillFromNoise` and short-circuit the vanilla noise terrain stage.
- Captures each server world's seed and feeds it into the model.
- External JSON MLP weights with an embedded thread-safe fallback; both produce surface height, moisture, and temperature.
- Converts predictions into bedrock, stone, dirt, grass/sand, water, and air columns.
- Maps the network's temperature/moisture head to vanilla ocean, desert, savanna, snowy, taiga, forest, jungle, swamp, and plains biomes.
- Runs a separate deterministic 3D neural field for cave probability using `(seed, x, y, z)` features.
- Can disable vanilla noise carvers so neural caves are not mixed with a second cave generator.
- Adds a deterministic model-driven decoration pass for extra ores, vegetation, and small stone ruins after vanilla biome decoration.
- Keeps the world seed deterministic and only changes newly generated chunks.

The model runtime is deliberately dependency-light: it uses JSON weights instead of native ONNX libraries, while preserving a stable `TerrainPrediction` contract.

## Build

Use Java 21 to run Gradle/Loom. The produced bytecode targets Java 17, matching Minecraft 1.20.1.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat build
```

The remapped jar is written to `build/libs/`.

Run the headless generation smoke test without accepting the server EULA:

```powershell
.\gradlew.bat chunkSmoke
.\gradlew.bat biomeSmoke decorationSmoke jsonModelSmoke
```

On first launch the mod creates `config/neuralterrain.json`. Set `enabled` to `false` to restore vanilla noise generation without removing the mod. The other fields control sea level, model height scale, beach width, bedrock thickness, water generation, neural biome selection, neural caves, carver disabling, neural decorations, extra ores/vegetation/structures, cave threshold, and the external model filename. Restart the game after changing the file.

Operators with permission level 2 can use `/neuralterrain status` and `/neuralterrain reload` in-game. Reload affects newly generated chunks; already generated chunks are not rewritten.

Use `/neuralterrain export_samples 256 8` near a generated area to export real surface heights and biome climate features to `config/neuralterrain-samples.jsonl`. That file can be passed directly to the trainer.

Use `/neuralterrain export_cave_samples 128 4 -64 128` to export interior air/solid labels to `config/neuralterrain-cave-samples.jsonl`. The command samples only below the predicted surface and is intended for collecting cave training data from reference worlds.

## Test in the development client

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat runClient
```

Create a new world after launching the client. Existing chunks are not rewritten.
If the development client crashes in native GLFW before the menu appears, use the headless generation smoke tasks below; this is an environment-specific renderer issue and does not affect the server-side generation code.

For this specific development environment, `.\gradlew.bat runClient -PdevNoGlfwWait=true` enables a disabled-by-default client workaround that gets past the first GLFW wait calls long enough to verify world creation. It is not enabled in normal gameplay or in the packaged configuration.

## Replacing the model

If `config/neuralterrain-model.json` exists, it is loaded automatically. The terrain-height JSON schema is documented in `tools/train_neural_model.py`; if it is missing or invalid, the mod uses the embedded MLP fallback. Cave weights are optional: models without the optional `cave_*` arrays use the embedded 3D cave network, while models that include them can predict caves externally too.

The standard-library trainer accepts JSONL samples and writes a compatible model:

```powershell
python tools/train_neural_model.py `
  --input tools/sample_training_samples.jsonl `
  --cave-input tools/sample_cave_samples.jsonl `
  --output run/config/neuralterrain-model.json `
  --epochs 200 `
  --cave-epochs 200
```

The sample files are only pipeline smoke tests. For useful terrain, replace them with measurements extracted from reference worlds or a curated terrain dataset. Omitting `--cave-input` keeps the embedded cave network as the fallback.

## Important limitation

This implementation covers neural terrain height, climate-driven surface blocks, model-selected vanilla biomes, water, neural caves, model-driven extra ores/vegetation, and small deterministic ruins. The standard vanilla structure/ore/vegetation stages remain enabled after biome selection. Actual in-game visual tuning still depends on training against a representative reference-world dataset.
