package com.neuralterrain;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

/** Prints the mapped 1.20.1 generator hooks available to the integration layer. */
public final class WorldgenIntrospection {
    private WorldgenIntrospection() {
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Class<?> generator = Class.forName("net.minecraft.world.level.chunk.ChunkGenerator");
        Arrays.stream(generator.getDeclaredMethods())
            .filter(method -> method.getName().contains("Biome")
                || method.getName().contains("Structure")
                || method.getName().contains("Carver")
                || method.getName().contains("Noise")
                || method.getName().contains("Decoration")
                || method.getName().contains("Feature"))
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .forEach(WorldgenIntrospection::print);
        System.out.println("--- NoiseBasedChunkGenerator ---");
        Class<?> noise = Class.forName("net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator");
        System.out.println("--- NoiseBasedChunkGenerator constructors ---");
        Arrays.stream(noise.getDeclaredConstructors())
            .forEach(System.out::println);
        System.out.println("--- registry access factories ---");
        Class<?> access = Class.forName("net.minecraft.core.RegistryAccess");
        Arrays.stream(access.getDeclaredMethods())
            .filter(method -> method.getName().toLowerCase().contains("builtin")
                || method.getName().toLowerCase().contains("registry"))
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .forEach(WorldgenIntrospection::print);
        System.out.println("--- NoiseGeneratorSettings ---");
        Class<?> settings = Class.forName("net.minecraft.world.level.levelgen.NoiseGeneratorSettings");
        Arrays.stream(settings.getDeclaredConstructors()).forEach(System.out::println);
        Arrays.stream(settings.getDeclaredMethods())
            .filter(method -> Modifier.isStatic(method.getModifiers()) || method.getName().toLowerCase().contains("overworld"))
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .forEach(WorldgenIntrospection::print);
        System.out.println("--- BiomeSource implementations ---");
        Class<?> fixedBiome = Class.forName("net.minecraft.world.level.biome.FixedBiomeSource");
        Arrays.stream(fixedBiome.getDeclaredConstructors()).forEach(System.out::println);
        System.out.println("--- StructureManager constructors ---");
        Class<?> structureManager = Class.forName("net.minecraft.world.level.StructureManager");
        Arrays.stream(structureManager.getDeclaredConstructors()).forEach(System.out::println);
        Class<?> worldGenLevel = Class.forName("net.minecraft.world.level.WorldGenLevel");
        System.out.println("WorldGenLevel interface=" + worldGenLevel.isInterface());
        System.out.println("--- WorldOptions constructors ---");
        Class<?> worldOptions = Class.forName("net.minecraft.world.level.levelgen.WorldOptions");
        Arrays.stream(worldOptions.getDeclaredConstructors()).forEach(System.out::println);
        System.out.println("--- StructureCheck constructors ---");
        Class<?> structureCheck = Class.forName("net.minecraft.world.level.chunk.StructureCheck");
        Arrays.stream(structureCheck.getDeclaredConstructors()).forEach(System.out::println);
        Arrays.stream(noise.getDeclaredMethods())
            .filter(method -> method.getName().contains("Surface")
                || method.getName().contains("Carver")
                || method.getName().contains("Noise")
                || method.getName().contains("Biome"))
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .forEach(WorldgenIntrospection::print);
        System.out.println("--- ChunkAccess biome/block hooks ---");
        Class<?> chunk = Class.forName("net.minecraft.world.level.chunk.ChunkAccess");
        Arrays.stream(chunk.getDeclaredMethods())
            .filter(method -> method.getName().contains("Biome")
                || method.getName().contains("Block")
                || method.getName().contains("Height"))
            .sorted((left, right) -> left.toString().compareTo(right.toString()))
            .forEach(WorldgenIntrospection::print);
        System.out.println("--- BuiltInRegistries fields ---");
        Class<?> builtIns = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Arrays.stream(builtIns.getDeclaredFields())
            .filter(field -> field.getType().getName().contains("Registry"))
            .sorted((left, right) -> left.getName().compareTo(right.getName()))
            .forEach(WorldgenIntrospection::printField);
    }

    private static void print(Method method) {
        System.out.println(Modifier.toString(method.getModifiers()) + " " + method);
    }

    private static void printField(Field field) {
        System.out.println(Modifier.toString(field.getModifiers()) + " " + field.getType().getName() + " " + field.getName());
    }
}
