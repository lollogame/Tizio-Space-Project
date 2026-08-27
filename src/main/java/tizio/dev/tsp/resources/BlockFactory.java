package tizio.dev.tsp.resources;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.registry.RegisterBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BlockFactory {

    private final String name;
    private Supplier<Block> blockSupplier;
    private final List<String> customModels = new ArrayList<>();
    private String customItemModel = null;
    private boolean isRandomRotation = false;
    private boolean createSlab = false;
    private boolean createStairs = false;

    public BlockFactory(String name, Supplier<Block> baseBlock) {
        this.name = name;
        this.blockSupplier = baseBlock;
    }

    public String getName() { return name; }
    public List<String> getCustomModels() { return customModels; }
    public String getCustomItemModel() { return customItemModel; }
    public boolean isRandomRotation() { return isRandomRotation; }
    public boolean hasSlab() { return createSlab; }
    public boolean hasStairs() { return createStairs; }

    public BlockFactory customModel(String modelPath) {
        this.customModels.add(modelPath);
        return this;
    }

    public BlockFactory customItem(String itemModelName) {
        this.customItemModel = itemModelName;
        return this;
    }

    public BlockFactory randomRotation() {
        this.isRandomRotation = true;
        return this;
    }

    public BlockFactory makeSlab() {
        this.createSlab = true;
        return this;
    }

    public BlockFactory makeStairs() {
        this.createStairs = true;
        return this;
    }

    public RegistryObject<Block> build() {
        RegistryObject<Block> mainBlock = RegisterBlocks.BLOCKS.register(name, blockSupplier);
        RegisterBlocks.itemRegistryObject(name, mainBlock);
        RegisterBlocks.BUILDERS.put(name, this);

        if (createSlab) {
            String slabName = name + "_slab";
            RegistryObject<Block> slabBlock = RegisterBlocks.BLOCKS.register(slabName,
                    () -> new SlabBlock(BlockBehaviour.Properties.copy(mainBlock.get())));
            RegisterBlocks.itemRegistryObject(slabName, slabBlock);
        }

        if (createStairs) {
            String stairsName = name + "_stairs";
            RegistryObject<Block> stairsBlock = RegisterBlocks.BLOCKS.register(stairsName,
                    () -> new StairBlock(() -> mainBlock.get().defaultBlockState(), BlockBehaviour.Properties.copy(mainBlock.get())));
            RegisterBlocks.itemRegistryObject(stairsName, stairsBlock);
        }

        return mainBlock;
    }
}
