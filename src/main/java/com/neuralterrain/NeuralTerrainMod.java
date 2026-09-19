package com.neuralterrain;

import com.neuralterrain.config.NeuralTerrainConfig;
import com.neuralterrain.model.NeuralTerrainModel;
import com.neuralterrain.tools.TerrainSampleExporter;
import com.neuralterrain.tools.CaveSampleExporter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the Neural Terrain mod. */
public final class NeuralTerrainMod implements ModInitializer {
    public static final String MOD_ID = "neuralterrain";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NeuralTerrainConfig.load();
        NeuralTerrainModel.load(NeuralTerrainConfig.modelPath());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        LOGGER.info("Neural Terrain loaded: deterministic neural heightfield generation is enabled.");
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neuralterrain")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(context -> {
                    context.getSource().sendSuccess(
                        () -> Component.literal("Neural Terrain: " + NeuralTerrainConfig.describe() + ", backend=" + NeuralTerrainModel.backendName()),
                        false
                    );
                    return 1;
                }))
            .then(Commands.literal("reload")
                .executes(context -> {
                    NeuralTerrainConfig.load();
                    NeuralTerrainModel.load(NeuralTerrainConfig.modelPath());
                    context.getSource().sendSuccess(
                        () -> Component.literal("Neural Terrain configuration reloaded."),
                        true
                    );
                    return 1;
                }))
            .then(Commands.literal("export_samples")
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, 512))
                    .then(Commands.argument("stride", IntegerArgumentType.integer(1, 32))
                        .executes(context -> {
                            BlockPos center = BlockPos.containing(context.getSource().getPosition());
                            int radius = IntegerArgumentType.getInteger(context, "radius");
                            int stride = IntegerArgumentType.getInteger(context, "stride");
                            try {
                                TerrainSampleExporter.ExportResult result = TerrainSampleExporter.export(
                                    context.getSource().getLevel(), center, radius, stride
                                );
                                context.getSource().sendSuccess(
                                    () -> Component.literal("Exported " + result.samples() + " terrain samples to " + result.path()),
                                    true
                                );
                                return result.samples();
                            } catch (Exception exception) {
                                LOGGER.error("Terrain sample export failed", exception);
                                context.getSource().sendFailure(Component.literal("Terrain sample export failed: " + exception.getMessage()));
                                return 0;
                            }
                        }))))
            .then(Commands.literal("export_cave_samples")
                .then(Commands.argument("radius", IntegerArgumentType.integer(16, 256))
                    .then(Commands.argument("stride", IntegerArgumentType.integer(1, 8))
                        .then(Commands.argument("min_y", IntegerArgumentType.integer(-64, 320))
                            .then(Commands.argument("max_y", IntegerArgumentType.integer(-64, 320))
                                .executes(context -> {
                                    BlockPos center = BlockPos.containing(context.getSource().getPosition());
                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                    int stride = IntegerArgumentType.getInteger(context, "stride");
                                    int minY = IntegerArgumentType.getInteger(context, "min_y");
                                    int maxY = IntegerArgumentType.getInteger(context, "max_y");
                                    if (minY >= maxY) {
                                        context.getSource().sendFailure(Component.literal("min_y must be below max_y"));
                                        return 0;
                                    }
                                    try {
                                        CaveSampleExporter.ExportResult result = CaveSampleExporter.export(
                                            context.getSource().getLevel(), center, radius, stride, minY, maxY
                                        );
                                        context.getSource().sendSuccess(
                                            () -> Component.literal("Exported " + result.samples() + " cave samples to " + result.path()),
                                            true
                                        );
                                        return result.samples();
                                    } catch (Exception exception) {
                                        LOGGER.error("Cave sample export failed", exception);
                                        context.getSource().sendFailure(Component.literal("Cave sample export failed: " + exception.getMessage()));
                                        return 0;
                                    }
                                })))))));
    }
}
