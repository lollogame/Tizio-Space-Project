package tizio.dev.tsp.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.data.ExistingFileHelper;
import tizio.dev.tsp.MainClass;

import java.util.concurrent.CompletableFuture;

public class TSPBiomeTagsProvider extends BiomeTagsProvider {

    public TSPBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MainClass.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        this.tag(TSPBiomeTags.VENUS_BIOMES).add(
                myBiome("venus_plains")
        );

        this.tag(TSPBiomeTags.MOON_BIOMES).add(
                myBiome("moon_plains"),
                myBiome("moon_wasted_plains")
        );

        this.tag(TSPBiomeTags.MARS_BIOMES).add(
                myBiome("mars_plains")
        );
    }

    private ResourceKey<Biome> myBiome(String id){
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(MainClass.MODID, id));
    }
}
