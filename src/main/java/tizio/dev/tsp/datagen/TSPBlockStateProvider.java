package tizio.dev.tsp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import tizio.dev.tsp.MainClass;

import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.resources.BlockFactory;
import tizio.dev.tsp.resources.OreProperties;

import java.util.ArrayList;
import java.util.List;

public class TSPBlockStateProvider extends BlockStateProvider {

    public TSPBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MainClass.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        java.util.Set<String> oreNames = new java.util.HashSet<>();
        for (OreProperties oreProps : RegisterBlocks.RegisterOres.ORES.keySet()) {
            oreNames.add(oreProps.name());
        }

        for (BlockFactory builder : RegisterBlocks.BUILDERS.values()) {

            String name = builder.getName();
            if (oreNames.contains(name)) continue;

            Block mainBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, name));

            if (mainBlock == null) continue;

            VariantBlockStateBuilder variantBuilder = getVariantBuilder(mainBlock);
            List<ConfiguredModel> modelsList = new ArrayList<>();
            List<ModelFile> baseModels = new ArrayList<>();

            if (builder.getCustomModels().isEmpty()) {

                baseModels.add(cubeAll(mainBlock));
            } else {
                for (String customModelName : builder.getCustomModels()) {
                    baseModels.add(models().getExistingFile(modLoc("custom/" + customModelName)));
                }
            }

            for (ModelFile model : baseModels) {
                if (builder.isRandomRotation()) {
                    modelsList.add(new ConfiguredModel(model, 0, 0, false));
                    modelsList.add(new ConfiguredModel(model, 0, 90, false));
                    modelsList.add(new ConfiguredModel(model, 0, 180, false));
                    modelsList.add(new ConfiguredModel(model, 0, 270, false));
                } else {
                    modelsList.add(new ConfiguredModel(model));
                }
            }

            variantBuilder.partialState().setModels(modelsList.toArray(new ConfiguredModel[0]));

            if (builder.getCustomItemModel() != null) {
            } else if (!builder.getCustomModels().isEmpty()) {
                ModelFile firstCustomModel = models().getExistingFile(modLoc("custom/" + builder.getCustomModels().get(0)));
                simpleBlockItem(mainBlock, firstCustomModel);
            } else {
                simpleBlockItem(mainBlock, cubeAll(mainBlock));
            }

            if (builder.hasSlab()) {
                Block slabBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, name + "_slab"));
                if (slabBlock instanceof SlabBlock slab) {
                    ResourceLocation texture = blockTexture(mainBlock);
                    slabBlock(slab, texture, texture);
                    simpleBlockItem(slab, models().slab(name + "_slab", texture, texture, texture));
                }
            }

            if (builder.hasStairs()) {
                Block stairsBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, name + "_stairs"));
                if (stairsBlock instanceof StairBlock stairs) {
                    ResourceLocation texture = blockTexture(mainBlock);
                    stairsBlock(stairs, texture);
                    simpleBlockItem(stairs, models().stairs(name + "_stairs", texture, texture, texture));
                }
            }
        }

        for (OreProperties oreProps : RegisterBlocks.RegisterOres.ORES.keySet()) {
            Block oreBlock = RegisterBlocks.RegisterOres.ORES.get(oreProps).get();
            String oreName = oreProps.name();

            ModelFile oreModel = models().cubeAll(oreName, modLoc("block/ores/" + oreName));
            simpleBlock(oreBlock, oreModel);
            simpleBlockItem(oreBlock, oreModel);
        }
    }
}
