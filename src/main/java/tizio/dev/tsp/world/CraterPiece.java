package tizio.dev.tsp.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import tizio.dev.tsp.registry.RegisterStructures;

public class CraterPiece extends StructurePiece {

    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int radius;
    private final int rimWidth;
    private final int maxDepth;
    private final int rimHeight;

    public CraterPiece(int centerX, int centerY, int centerZ, int radius, int rimWidth, int maxDepth, int rimHeight) {
        super(RegisterStructures.CRATER_PIECE.get(), 0, new BoundingBox(
                centerX - radius - rimWidth, centerY - maxDepth - 1, centerZ - radius - rimWidth,
                centerX + radius + rimWidth, centerY + rimHeight + 1, centerZ + radius + rimWidth));
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.rimWidth = rimWidth;
        this.maxDepth = maxDepth;
        this.rimHeight = rimHeight;
    }

    public CraterPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(RegisterStructures.CRATER_PIECE.get(), tag);
        this.centerX = tag.getInt("CenterX");
        this.centerY = tag.getInt("CenterY");
        this.centerZ = tag.getInt("CenterZ");
        this.radius = tag.getInt("Radius");
        this.rimWidth = tag.getInt("RimWidth");
        this.maxDepth = tag.getInt("MaxDepth");
        this.rimHeight = tag.getInt("RimHeight");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterY", centerY);
        tag.putInt("CenterZ", centerZ);
        tag.putInt("Radius", radius);
        tag.putInt("RimWidth", rimWidth);
        tag.putInt("MaxDepth", maxDepth);
        tag.putInt("RimHeight", rimHeight);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        int totalRadius = radius + rimWidth;
        int minX = Math.max(centerX - totalRadius, chunkBox.minX());
        int maxX = Math.min(centerX + totalRadius, chunkBox.maxX());
        int minZ = Math.max(centerZ - totalRadius, chunkBox.minZ());
        int maxZ = Math.min(centerZ + totalRadius, chunkBox.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dist = Math.sqrt((double) (x - centerX) * (x - centerX) + (double) (z - centerZ) * (z - centerZ));
                if (dist > totalRadius) continue;

                int columnSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

                if (dist <= radius) {
                    double t = dist / radius;
                    int depth = (int) Math.round(maxDepth * (1.0 - t * t));
                    for (int y = columnSurfaceY; y > columnSurfaceY - depth; y--) {
                        mpos.set(x, y, z);
                        if (chunkBox.isInside(mpos) && !level.isEmptyBlock(mpos)) {
                            level.setBlock(mpos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                } else {
                    double t = (dist - radius) / rimWidth;
                    int height = (int) Math.round(rimHeight * (1.0 - t) * (1.0 - t));
                    if (height <= 0) continue;
                    BlockState fillState = level.getBlockState(mpos.set(x, columnSurfaceY - 1, z));
                    for (int y = columnSurfaceY; y < columnSurfaceY + height; y++) {
                        mpos.set(x, y, z);
                        if (chunkBox.isInside(mpos) && level.isEmptyBlock(mpos)) {
                            level.setBlock(mpos, fillState, 3);
                        }
                    }
                }
            }
        }
    }
}
