package tizio.dev.tsp.resources.blocks;

import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import tizio.dev.tsp.resources.OreProperties;

public class OreBlock extends DropExperienceBlock {

    public OreBlock(OreProperties properties) {
        super(buildProperties(properties), properties.xpRange());
    }

    private static BlockBehaviour.Properties buildProperties(OreProperties properties) {
        BlockBehaviour.Properties blockProperties = BlockBehaviour.Properties.of()
                .mapColor(properties.mapColor())
                .strength(properties.hardness(), properties.resistance());

        if (properties.requiresCorrectTool()) {
            blockProperties.requiresCorrectToolForDrops();
        }

        return blockProperties;
    }

}

