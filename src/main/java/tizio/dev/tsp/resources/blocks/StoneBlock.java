package tizio.dev.tsp.resources.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class StoneBlock extends Block {

    public StoneBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adiacentsBlocks, Direction dir) {
        return adiacentsBlocks.getBlock() == this || super.skipRendering(state, adiacentsBlocks, dir);
    }

}
