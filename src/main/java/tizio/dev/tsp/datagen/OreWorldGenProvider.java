package tizio.dev.tsp.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.resources.OreProperties;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class OreWorldGenProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, OreWorldGenProvider::configuredFeatures)
            .add(Registries.PLACED_FEATURE, OreWorldGenProvider::placedFeatures)
            .add(ForgeRegistries.Keys.BIOME_MODIFIERS, OreWorldGenProvider::biomeModifiers);

    public OreWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(MainClass.MODID));
    }

    private static void configuredFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        for (OreProperties props : RegisterBlocks.RegisterOres.ORES.keySet()) {
            context.register(configuredKey(props), new ConfiguredFeature<>(Feature.ORE,
                    new OreConfiguration(List.of(OreConfiguration.target(
                            new TagMatchTest(props.replaces()),
                            RegisterBlocks.RegisterOres.ORES.get(props).get().defaultBlockState()
                    )), props.veinSize())));
        }
    }

    private static void placedFeatures(BootstapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        for (OreProperties props : RegisterBlocks.RegisterOres.ORES.keySet()) {
            context.register(placedKey(props), new PlacedFeature(
                    configuredFeatures.getOrThrow(configuredKey(props)),
                    List.of(
                            CountPlacement.of(props.veinsPerChunk()),
                            InSquarePlacement.spread(),
                            HeightRangePlacement.uniform(VerticalAnchor.absolute(props.minY()), VerticalAnchor.absolute(props.maxY())),
                            BiomeFilter.biome()
                    )
            ));
        }
    }

    private static void biomeModifiers(BootstapContext<BiomeModifier> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        for (OreProperties props : RegisterBlocks.RegisterOres.ORES.keySet()) {
            context.register(biomeModifierKey(props), new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                    biomes.getOrThrow(props.biomes()),
                    HolderSet.direct(placedFeatures.getOrThrow(placedKey(props))),
                    props.decorationStep()
            ));
        }
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(OreProperties props) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(MainClass.MODID, props.name() + "_ore"));
    }

    private static ResourceKey<PlacedFeature> placedKey(OreProperties props) {
        return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MainClass.MODID, props.name() + "_ore_placed"));
    }

    private static ResourceKey<BiomeModifier> biomeModifierKey(OreProperties props) {
        return ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MainClass.MODID, props.name() + "_ore_add"));
    }
}

