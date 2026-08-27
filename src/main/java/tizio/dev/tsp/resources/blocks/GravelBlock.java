package tizio.dev.tsp.resources.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class GravelBlock extends FallingBlock {

    public GravelBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.SNARE).strength(0.6F).sound(SoundType.GRAVEL));
    }


    @Override
    public boolean skipRendering(BlockState state, BlockState adiacentsBlocks, Direction dir) {
        return adiacentsBlocks.getBlock() == this || super.skipRendering(state, adiacentsBlocks, dir);
    }


}
