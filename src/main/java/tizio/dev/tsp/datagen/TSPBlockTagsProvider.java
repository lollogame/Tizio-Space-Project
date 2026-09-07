package tizio.dev.tsp.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.resources.OreProperties;
import tizio.dev.tsp.resources.OreToolTier;
import tizio.dev.tsp.resources.blocks.StoneBlock;

import java.util.concurrent.CompletableFuture;

public class TSPBlockTagsProvider extends net.minecraftforge.common.data.BlockTagsProvider {

    public TSPBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MainClass.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        this.tag(TSPBlockTags.MARS_STONE_ORE_REPLACEABLE).add(RegisterBlocks.MARS_STONE.get());
        this.tag(TSPBlockTags.MARS_STONE_ORE_REPLACEABLE).add(RegisterBlocks.MARS_SAND.get());

        this.tag(TSPBlockTags.MOON_STONE_ORE_REPLACEABLE).add(RegisterBlocks.MOON_STONE.get());

        this.tag(TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE).add(RegisterBlocks.VENUS_STONE.get());

        for (Block block : RegisterBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).toList()) {
            if (block instanceof StoneBlock) {
                this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            }
        }

        for (OreProperties props : RegisterBlocks.RegisterOres.ORES.keySet()) {
            Block block = RegisterBlocks.RegisterOres.ORES.get(props).get();

            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);

            OreToolTier toolTier = props.toolTier();
            if (toolTier.tag() != null) {
                this.tag(toolTier.tag()).add(block);
            }
        }
    }

}

