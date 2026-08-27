package tizio.dev.tsp.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import tizio.dev.tsp.MainClass;

public class TSPBlockTags {

    public static final TagKey<Block> MARS_STONE_ORE_REPLACEABLE = tag("mars_stone_ore_replaceable");
    public static final TagKey<Block> MOON_STONE_ORE_REPLACEABLE = tag("moon_stone_ore_replaceable");

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(MainClass.MODID, name));
    }

}

