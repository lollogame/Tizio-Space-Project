package tizio.dev.tsp.world.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import tizio.dev.tsp.registry.RegisterStructures;
import tizio.dev.tsp.world.CraterPiece;

import java.util.Optional;

public class CraterStructure extends Structure {

    public static final Codec<CraterStructure> CODEC = simpleCodec(CraterStructure::new);

    public CraterStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX() + 8;
        int z = chunkPos.getMinBlockZ() + 8;
        int y = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());

        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> {
            RandomSource random = context.random();
            int radius = 55 + random.nextInt(11);
            int rimWidth = 4 + random.nextInt(5);
            int maxDepth = 40 + random.nextInt(10);
            int rimHeight = 1 + random.nextInt(2);
            builder.addPiece(new CraterPiece(x, y, z, radius, rimWidth, maxDepth, rimHeight));
        }));
    }

    @Override
    public StructureType<?> type() {
        return RegisterStructures.CRATER_STRUCTURE_TYPE.get();
    }
}

