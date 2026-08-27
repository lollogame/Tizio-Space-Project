package tizio.dev.tsp.resources.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class SandBlock extends FallingBlock {

    public SandBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adiacentsBlocks, Direction dir) {
        return adiacentsBlocks.getBlock() == this || super.skipRendering(state, adiacentsBlocks, dir);
    }


}
