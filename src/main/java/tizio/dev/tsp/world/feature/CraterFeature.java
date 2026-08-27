package tizio.dev.tsp.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.world.CraterConfiguration;

public class CraterFeature extends Feature<CraterConfiguration> {

    public CraterFeature(Codec<CraterConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        CraterConfiguration config = context.config();

        int span = Math.max(1, config.maxRadius() - config.minRadius());
        int radius = config.minRadius() + random.nextInt(span + 1);
        int rimWidth = config.rimWidth();
        int maxDepth = config.maxDepth();
        int rimHeight = config.rimHeight();
        int totalRadius = radius + rimWidth;

        int maxSafeRadius = 15;
        if (totalRadius > maxSafeRadius) {
            double scale = (double) maxSafeRadius / totalRadius;
            radius = Math.max(1, (int) Math.floor(radius * scale));
            rimWidth = Math.max(1, maxSafeRadius - radius);
            totalRadius = radius + rimWidth;
        }

        if (origin.getY() > config.maxSurfaceY()) {
            return false;
        }

        StructureManager structureManager = level.getLevel().structureManager();
        Structure giantCrater = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(new ResourceLocation(MainClass.MODID, "giant_crater"));
        if (giantCrater != null) {
            StructureStart start = structureManager.getStructureWithPieceAt(origin, giantCrater);
            if (start != null && start.isValid()) {
                return false;
            }
        }

        int centerX = origin.getX();
        int centerZ = origin.getZ();

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                if (dist > totalRadius) continue;

                int x = centerX + dx;
                int z = centerZ + dz;
                int columnSurfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

                if (dist <= radius) {
                    double t = dist / radius;
                    int depth = (int) Math.round(maxDepth * (1.0 - t * t));
                    for (int y = columnSurfaceY; y > columnSurfaceY - depth; y--) {
                        pos.set(x, y, z);
                        if (!level.isEmptyBlock(pos)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            placed = true;
                        }
                    }
                } else {
                    double t = (dist - radius) / rimWidth;
                    int height = (int) Math.round(rimHeight * (1.0 - t) * (1.0 - t));
                    if (height <= 0) continue;
                    BlockState fillState = level.getBlockState(pos.set(x, columnSurfaceY - 1, z));
                    for (int y = columnSurfaceY; y < columnSurfaceY + height; y++) {
                        pos.set(x, y, z);
                        if (level.isEmptyBlock(pos)) {
                            level.setBlock(pos, fillState, 3);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }
}
