package tizio.dev.tsp.resources;

import net.minecraft.world.level.ItemLike;
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

    private boolean createCraftingRecipes = false;
    private boolean createStonecutterRecipes = false;

    private Supplier<? extends ItemLike> smeltingResultSupplier = null;
    private float smeltingXp = 0.7f;
    private int smeltingTime = 200;
    private boolean canSmelt = false;

    private final List<CutBlockInfo> cutBlocks = new ArrayList<>();

    private RegistryObject<Block> mainBlock;
    private RegistryObject<Block> slabBlock;
    private RegistryObject<Block> stairsBlock;

    private String textureFolder = "block";

    public BlockFactory(String name, Supplier<Block> baseBlock) {
        this.name = name;
        this.blockSupplier = baseBlock;
    }

    public String getTextureFolder() {
        return textureFolder;
    }

    public String getName() { return name; }
    public List<String> getCustomModels() { return customModels; }
    public String getCustomItemModel() { return customItemModel; }
    public boolean isRandomRotation() { return isRandomRotation; }
    public boolean hasSlab() { return createSlab; }
    public boolean hasStairs() { return createStairs; }

    public boolean hasCraftingRecipes() { return createCraftingRecipes; }
    public boolean hasStonecutterRecipes() { return createStonecutterRecipes; }

    public Supplier<? extends ItemLike> getSmeltingResultSupplier() { return smeltingResultSupplier; }
    public float getSmeltingXp() { return smeltingXp; }
    public int getSmeltingTime() { return smeltingTime; }
    public boolean canSmelt() { return canSmelt; }

    public RegistryObject<Block> getMainBlock() { return mainBlock; }
    public RegistryObject<Block> getSlabBlock() { return slabBlock; }
    public RegistryObject<Block> getStairsBlock() { return stairsBlock; }
    public List<CutBlockInfo> getCutBlocks() { return cutBlocks; }

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

    public BlockFactory stonecutter() {
        this.createStonecutterRecipes = true;
        return this;
    }

    public BlockFactory craftingRecipes() {
        this.createCraftingRecipes = true;
        return this;
    }

    public BlockFactory textureFolder(String folder) {
        this.textureFolder = folder;
        return this;
    }

    public BlockFactory canSmelt(Supplier<? extends ItemLike> resultSupplier, float xp, int cookingTime) {
        this.canSmelt = true;
        this.smeltingResultSupplier = resultSupplier;
        this.smeltingXp = xp;
        this.smeltingTime = cookingTime;
        return this;
    }

    public BlockFactory canSmelt(Supplier<? extends ItemLike> resultSupplier) {
        return canSmelt(resultSupplier, 0.7f, 200);
    }

    public BlockFactory cutBlock(String targetBlockName) {
        return cutBlock(targetBlockName, 1);
    }

    public BlockFactory cutBlock(String targetBlockName, int count) {
        this.cutBlocks.add(new CutBlockInfo(targetBlockName, count));
        return this;
    }

    public RegistryObject<Block> build() {
        mainBlock = RegisterBlocks.BLOCKS.register(name, blockSupplier);
        RegisterBlocks.itemRegistryObject(name, mainBlock);
        RegisterBlocks.BUILDERS.put(name, this);

        if (createSlab) {
            String slabName = name + "_slab";
            slabBlock = RegisterBlocks.BLOCKS.register(slabName,
                    () -> new SlabBlock(BlockBehaviour.Properties.copy(mainBlock.get())));
            RegisterBlocks.itemRegistryObject(slabName, slabBlock);
        }

        if (createStairs) {
            String stairsName = name + "_stairs";
            stairsBlock = RegisterBlocks.BLOCKS.register(stairsName,
                    () -> new StairBlock(() -> mainBlock.get().defaultBlockState(), BlockBehaviour.Properties.copy(mainBlock.get())));
            RegisterBlocks.itemRegistryObject(stairsName, stairsBlock);
        }

        return mainBlock;
    }

    public static class CutBlockInfo {
        private final String targetBlockName;
        private final int count;

        public CutBlockInfo(String targetBlockName, int count) {
            this.targetBlockName = targetBlockName;
            this.count = count;
        }

        public String getTargetBlockName() { return targetBlockName; }
        public int getCount() { return count; }
    }
}