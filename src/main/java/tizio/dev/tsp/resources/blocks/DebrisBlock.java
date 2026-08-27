package tizio.dev.tsp.resources.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DebrisBlock extends Block {

    public static final IntegerProperty MODELSTATE = IntegerProperty.create("modeltype", 0, 1);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0, 0.05, 0, 16, 2, 16);

    public DebrisBlock(Properties properties) {
        super(Properties.copy(Blocks.GRAVEL).sound(SoundType.GRAVEL).strength(0.5F).noCollission());
    }

    public DebrisBlock() {
        super(Properties.copy(Blocks.GRAVEL).sound(SoundType.GRAVEL).strength(0.5F).noCollission());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adiacentsBlocks, Direction dir) {
        return adiacentsBlocks.getBlock() == this || super.skipRendering(state, adiacentsBlocks, dir);
    }
}
