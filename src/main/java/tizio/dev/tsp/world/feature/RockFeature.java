package tizio.dev.tsp.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.world.SpacingFeatureConfiguration;

import java.util.Optional;

public class RockFeature extends Feature<SpacingFeatureConfiguration> {

    public RockFeature(Codec<SpacingFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpacingFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int spacing = context.config().spacing;

        BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin);

        if (surfacePos.getY() <= level.getMinBuildHeight()) {
            return false;
        }

        if (spacing > 0) {
            for (BlockPos checkPos : BlockPos.betweenClosed(
                    surfacePos.offset(-spacing, 0, -spacing),
                    surfacePos.offset(spacing, 0, spacing))) {
                if (!level.getBlockState(checkPos.above()).isAir()) {
                    return false;
                }
            }
        }

        ResourceLocation structureLoc = new ResourceLocation(MainClass.MODID, "rock_1");
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        Optional<StructureTemplate> templateOpt = templateManager.get(structureLoc);

        if (templateOpt.isEmpty()) {
            return false;
        }

        StructureTemplate template = templateOpt.get();
        Rotation rotation = Rotation.getRandom(random);
        Mirror mirror = random.nextFloat() < 0.5F ? Mirror.LEFT_RIGHT : Mirror.NONE;

        BlockPos finalPlacementPos = surfacePos.below(1);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(mirror)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);

        boolean success = template.placeInWorld(level, finalPlacementPos, finalPlacementPos, settings, random, 2);

        if (success) {
            BlockPos.betweenClosedStream(
                    finalPlacementPos,
                    finalPlacementPos.offset(template.getSize(rotation))
            ).forEach(pos -> {
                BlockPos below = pos.below();
                if (level.getBlockState(below).isAir() && !level.getBlockState(pos).isAir()) {
                    level.setBlock(below, level.getBlockState(surfacePos.below(2)), 2);
                }
            });
        }

        return success;
    }
}
