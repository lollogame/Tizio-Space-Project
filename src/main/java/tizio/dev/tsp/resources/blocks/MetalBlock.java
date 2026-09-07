package tizio.dev.tsp.resources.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class MetalBlock extends Block {

    public MetalBlock() {
        super(Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adiacentsBlocks, Direction dir) {
        return adiacentsBlocks.getBlock() == this || super.skipRendering(state, adiacentsBlocks, dir);
    }

}
