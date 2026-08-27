package tizio.dev.tsp.resources;

import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public record OreProperties(
        String name,
        TagKey<Block> replaces,
        TagKey<Biome> biomes,
        MapColor mapColor,
        float hardness,
        float resistance,
        boolean requiresCorrectTool,
        UniformInt xpRange,
        Supplier<Item> dropItem,
        int minDrop,
        int maxDrop,
        int veinSize,
        int veinsPerChunk,
        int minY,
        int maxY,
        GenerationStep.Decoration decorationStep,
        OreToolTier toolTier
) {

    public static Builder builder(String name, TagKey<Block> replaces, TagKey<Biome> biomes) {
        return new Builder(name, replaces, biomes);
    }

    public static class Builder {
        private final String name;
        private final TagKey<Block> replaces;
        private final TagKey<Biome> biomes;
        private MapColor mapColor = MapColor.STONE;
        private float hardness = 3f;
        private float resistance = 3f;
        private boolean requiresCorrectTool = true;
        private int minXp = 0;
        private int maxXp = 0;
        private Supplier<Item> dropItem = null;
        private int minDrop = 1;
        private int maxDrop = 1;
        private int veinSize = 8;
        private int veinsPerChunk = 6;
        private int minY = 0;
        private int maxY = 64;
        private GenerationStep.Decoration decorationStep = GenerationStep.Decoration.UNDERGROUND_ORES;
        private OreToolTier toolTier = OreToolTier.STONE;

        private Builder(String name, TagKey<Block> replaces, TagKey<Biome> biomes) {
            this.name = name;
            this.replaces = replaces;
            this.biomes = biomes;
        }

        public Builder mapColor(MapColor mapColor) {
            this.mapColor = mapColor;
            return this;
        }

        public Builder strength(float hardness, float resistance) {
            this.hardness = hardness;
            this.resistance = resistance;
            return this;
        }

        public Builder requiresCorrectTool(boolean value) {
            this.requiresCorrectTool = value;
            return this;
        }

        public Builder xp(int min, int max) {
            this.minXp = min;
            this.maxXp = max;
            return this;
        }

        public Builder drops(Supplier<Item> item, int min, int max) {
            this.dropItem = item;
            this.minDrop = min;
            this.maxDrop = max;
            return this;
        }

        public Builder vein(int size, int perChunk) {
            this.veinSize = size;
            this.veinsPerChunk = perChunk;
            return this;
        }

        public Builder height(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
            return this;
        }

        public Builder decorationStep(GenerationStep.Decoration step) {
            this.decorationStep = step;
            return this;
        }

        public Builder toolTier(OreToolTier tier) {
            this.toolTier = tier;
            return this;
        }

        public OreProperties build() {
            return new OreProperties(name, replaces, biomes, mapColor, hardness, resistance, requiresCorrectTool,
                    UniformInt.of(minXp, maxXp), dropItem, minDrop, maxDrop, veinSize, veinsPerChunk, minY, maxY,
                    decorationStep, toolTier);
        }
    }
}



