package tizio.dev.tsp.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tizio.dev.tsp.MainClass;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SurfaceStructureProvider implements DataProvider {
    private final PackOutput packOutput;
    private final List<StructureEntry> entries = new ArrayList<>();

    public SurfaceStructureProvider(DataGenerator generator) {
        this.packOutput = generator.getPackOutput();

        addStructure("rock_1", "tsp:venus_plains", 16, 64, 4);
    }

    public SurfaceStructureProvider addStructure(String name, String biomeId, int count, int rarity, int spacing) {
        entries.add(new StructureEntry(name, biomeId, count, rarity, spacing));
        return this;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path path = packOutput.getOutputFolder();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (StructureEntry entry : entries) {

            JsonObject configuredFeature = new JsonObject();
            configuredFeature.addProperty("type", MainClass.MODID + ":rock_feature");

            JsonObject config = new JsonObject();
            config.addProperty("spacing", entry.spacing);
            configuredFeature.add("config", config);

            JsonObject placedFeature = new JsonObject();
            placedFeature.addProperty("feature", MainClass.MODID + ":" + entry.name);
            JsonArray placement = new JsonArray();

            JsonObject rarityObj = new JsonObject();
            rarityObj.addProperty("type", "minecraft:rarity_filter");
            rarityObj.addProperty("chance", entry.rarity);
            placement.add(rarityObj);

            JsonObject countObj = new JsonObject();
            countObj.addProperty("type", "minecraft:count");
            countObj.addProperty("count", entry.count);
            placement.add(countObj);

            JsonObject squareObj = new JsonObject();
            squareObj.addProperty("type", "minecraft:in_square");
            placement.add(squareObj);

            JsonObject heightmapObj = new JsonObject();
            heightmapObj.addProperty("type", "minecraft:heightmap");
            heightmapObj.addProperty("heightmap", "WORLD_SURFACE_WG");
            placement.add(heightmapObj);

            JsonObject biomePlacement = new JsonObject();
            biomePlacement.addProperty("type", "minecraft:biome");
            placement.add(biomePlacement);

            placedFeature.add("placement", placement);

            JsonObject biomeModifier = new JsonObject();
            biomeModifier.addProperty("type", "forge:add_features");
            biomeModifier.addProperty("biomes", entry.biomeId);

            JsonArray featuresArray = new JsonArray();
            featuresArray.add(MainClass.MODID + ":" + entry.name);
            biomeModifier.add("features", featuresArray);
            biomeModifier.addProperty("step", "vegetal_decoration");

            futures.add(DataProvider.saveStable(cache, configuredFeature,
                    path.resolve("data/" + MainClass.MODID + "/worldgen/configured_feature/" + entry.name + ".json")));

            futures.add(DataProvider.saveStable(cache, placedFeature,
                    path.resolve("data/" + MainClass.MODID + "/worldgen/placed_feature/" + entry.name + ".json")));

            futures.add(DataProvider.saveStable(cache, biomeModifier,
                    path.resolve("data/" + MainClass.MODID + "/forge/biome_modifier/" + entry.name + "_biome_modifier.json")));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public String getName() {
        return "Surface Structures Datagen";
    }

    private static class StructureEntry {
        final String name;
        final String biomeId;
        final int count;
        final int rarity;
        final int spacing;

        public StructureEntry(String name, String biomeId, int count, int rarity, int spacing) {
            this.name = name;
            this.biomeId = biomeId;
            this.count = count;
            this.rarity = rarity;
            this.spacing = spacing;
        }
    }
}
