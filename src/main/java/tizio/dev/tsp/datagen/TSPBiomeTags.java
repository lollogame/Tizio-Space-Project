package tizio.dev.tsp.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import tizio.dev.tsp.MainClass;

public class TSPBiomeTags {


    public static final TagKey<Biome> MOON_BIOMES = tag("moon_biomes");
    public static final TagKey<Biome> MARS_BIOMES = tag("mars_biomes");
    public static final TagKey<Biome> VENUS_BIOMES = tag("venus_biomes");


    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(MainClass.MODID, name));
    }

}
