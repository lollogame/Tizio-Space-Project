package tizio.dev.tsp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.registry.RegisterItems;
import tizio.dev.tsp.resources.BlockFactory;
import tizio.dev.tsp.resources.ItemFactory;

import java.util.Map;
import java.util.function.Consumer;

public class TSPRecipeProvider extends RecipeProvider {

    public TSPRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {

        registerManualRecipes(writer);

        for (BlockFactory factory : RegisterBlocks.BUILDERS.values()) {
            if (factory.getMainBlock() == null) continue;

            ItemLike mainBlock = factory.getMainBlock().get();

            if (factory.hasStairs() && factory.getStairsBlock() != null) {
                ItemLike stairs = factory.getStairsBlock().get();
                if (factory.hasCraftingRecipes()) {
                    stairBuilder(stairs, Ingredient.of(mainBlock))
                            .unlockedBy(getHasName(mainBlock), has(mainBlock))
                            .save(writer);
                }
                if (factory.hasStonecutterRecipes()) {
                    stonecutterResultFromBase(writer, RecipeCategory.BUILDING_BLOCKS, stairs, mainBlock);
                }
            }

            if (factory.hasSlab() && factory.getSlabBlock() != null) {
                ItemLike slab = factory.getSlabBlock().get();
                if (factory.hasCraftingRecipes()) {
                    slabBuilder(RecipeCategory.BUILDING_BLOCKS, slab, Ingredient.of(mainBlock))
                            .unlockedBy(getHasName(mainBlock), has(mainBlock))
                            .save(writer);
                }
                if (factory.hasStonecutterRecipes()) {
                    stonecutterResultFromBase(writer, RecipeCategory.BUILDING_BLOCKS, slab, mainBlock, 2);
                }
            }

            for (BlockFactory.CutBlockInfo cutInfo : factory.getCutBlocks()) {
                BlockFactory targetFactory = RegisterBlocks.BUILDERS.get(cutInfo.getTargetBlockName());
                if (targetFactory != null && targetFactory.getMainBlock() != null) {
                    stonecutterResultFromBase(writer, RecipeCategory.BUILDING_BLOCKS,
                            targetFactory.getMainBlock().get(), mainBlock, cutInfo.getCount());
                }
            }

            if (factory.canSmelt() && factory.getSmeltingResultSupplier() != null) {
                ItemLike result = factory.getSmeltingResultSupplier().get();
                if (result != null && result.asItem() != Items.AIR) {
                    float xp = factory.getSmeltingXp();
                    int time = factory.getSmeltingTime();

                    SimpleCookingRecipeBuilder.smelting(Ingredient.of(mainBlock), RecipeCategory.MISC, result, xp, time)
                            .unlockedBy(getHasName(mainBlock), has(mainBlock))
                            .save(writer, new ResourceLocation(MainClass.MODID, getItemName(result) + "_from_smelting_" + getItemName(mainBlock)));

                    SimpleCookingRecipeBuilder.blasting(Ingredient.of(mainBlock), RecipeCategory.MISC, result, xp, Math.max(1, time / 2))
                            .unlockedBy(getHasName(mainBlock), has(mainBlock))
                            .save(writer, new ResourceLocation(MainClass.MODID, getItemName(result) + "_from_blasting_" + getItemName(mainBlock)));
                }
            }
        }

        for (ItemFactory factory : RegisterItems.ITEM_BUILDERS.values()) {
            if (factory.getItemRegistryObject() == null) continue;

            ItemLike item = factory.getItemRegistryObject().get();

            if (factory.getNuggetSupplier() != null) {
                ItemLike nugget = factory.getNuggetSupplier().get();

                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item)
                        .define('#', nugget)
                        .pattern("###")
                        .pattern("###")
                        .pattern("###")
                        .unlockedBy(getHasName(nugget), has(nugget))
                        .save(writer, new ResourceLocation(MainClass.MODID, getItemName(item) + "_from_nuggets"));

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 9)
                        .requires(item)
                        .unlockedBy(getHasName(item), has(item))
                        .save(writer, new ResourceLocation(MainClass.MODID, getItemName(item) + "_to_nuggets"));
            }

            if (factory.getBlockSupplier() != null) {
                ItemLike block = factory.getBlockSupplier().get();

                ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                        .define('#', item)
                        .pattern("###")
                        .pattern("###")
                        .pattern("###")
                        .unlockedBy(getHasName(item), has(item))
                        .save(writer, new ResourceLocation(MainClass.MODID, getItemName(block) + "_from_ingots"));

                ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item, 9)
                        .requires(block)
                        .unlockedBy(getHasName(block), has(block))
                        .save(writer, new ResourceLocation(MainClass.MODID, getItemName(item) + "_from_" + getItemName(block)));
            }

            if (factory.canSmelt() && factory.getSmeltingResultSupplier() != null) {
                ItemLike result = factory.getSmeltingResultSupplier().get();
                if (result != null && result.asItem() != Items.AIR) {
                    float xp = factory.getSmeltingXp();
                    int time = factory.getSmeltingTime();

                    SimpleCookingRecipeBuilder.smelting(Ingredient.of(item), RecipeCategory.MISC, result, xp, time)
                            .unlockedBy(getHasName(item), has(item))
                            .save(writer, new ResourceLocation(MainClass.MODID, getItemName(result) + "_from_smelting_" + getItemName(item)));

                    SimpleCookingRecipeBuilder.blasting(Ingredient.of(item), RecipeCategory.MISC, result, xp, Math.max(1, time / 2))
                            .unlockedBy(getHasName(item), has(item))
                            .save(writer, new ResourceLocation(MainClass.MODID, getItemName(result) + "_from_blasting_" + getItemName(item)));
                }
            }
        }
    }

    private void registerManualRecipes(Consumer<FinishedRecipe> writer) {

        shapedRecipe(writer, RecipeCategory.REDSTONE, RegisterItems.ITEM_BUILDERS.get("circuit_board").getItemRegistryObject().get(), 1, "circuit_board",
                new String[]{
                        "   ",
                        "IRI",
                        "###"
                },
                'I', Items.IRON_INGOT,
                '#', Items.GOLD_NUGGET,
                'R', Items.REDSTONE
        );

        Item titanium = RegisterItems.TITANIUM_INGOT.get();
        Item circuit = RegisterItems.CIRCUIT_BOARD.get();
        Item temperedGlass = Blocks.TINTED_GLASS.asItem();

        shapedRecipe(writer, RecipeCategory.COMBAT, RegisterItems.ARMOR_PIECES.get("space_suit_1_white").get(ArmorItem.Type.HELMET).get(), 1, "space_suit_1_white_helmet",
                new String[]{
                        "TTT",
                        "TGT",
                        " C "
                },
                'T', titanium,
                'G', temperedGlass,
                'C', circuit
        );

        shapedRecipe(writer, RecipeCategory.COMBAT, RegisterItems.ARMOR_PIECES.get("space_suit_1_white").get(ArmorItem.Type.CHESTPLATE).get(), 1, "space_suit_1_white_chestplate",
                new String[]{
                        "T T",
                        "TCT",
                        "TTT"
                },
                'T', titanium,
                'C', circuit
        );

        shapedRecipe(writer, RecipeCategory.COMBAT, RegisterItems.ARMOR_PIECES.get("space_suit_1_white").get(ArmorItem.Type.LEGGINGS).get(), 1, "space_suit_1_white_leggings",
                new String[]{
                        "TTT",
                        "TCT",
                        "T T"
                },
                'T', titanium,
                'C', circuit
        );

        shapedRecipe(writer, RecipeCategory.COMBAT, RegisterItems.ARMOR_PIECES.get("space_suit_1_white").get(ArmorItem.Type.BOOTS).get(), 1, "space_suit_1_white_boots",
                new String[]{
                        "T T",
                        "TCT",
                        "   "
                },
                'T', titanium,
                'C', circuit
        );

        Map<String, Item> dyes = Map.of(
                "orange", Items.ORANGE_DYE,
                "red", Items.RED_DYE,
                "yellow", Items.YELLOW_DYE,
                "green", Items.GREEN_DYE,
                "blue", Items.BLUE_DYE,
                "cyan", Items.CYAN_DYE,
                "pink", Items.PINK_DYE,
                "purple", Items.PURPLE_DYE
        );

        for (Map.Entry<String, Item> entry : dyes.entrySet()) {
            String color = entry.getKey();
            Item dye = entry.getValue();
            String armorId = "space_suit_1_" + color;

            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                Item whitePiece = RegisterItems.ARMOR_PIECES.get("space_suit_1_white").get(type).get();
                Item coloredPiece = RegisterItems.ARMOR_PIECES.get(armorId).get(type).get();

                shapelessRecipe(writer, RecipeCategory.COMBAT, coloredPiece, 1, armorId + "_" + type.getName() + "_from_tint",
                        whitePiece, dye
                );
            }
        }
    }

    private void shapedRecipe(Consumer<FinishedRecipe> writer, RecipeCategory category, ItemLike result, int count, String name, String[] pattern, Object... keys) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(category, result, count);
        for (String row : pattern) {
            builder.pattern(row);
        }

        ItemLike unlockItem = null;
        for (int i = 0; i < keys.length; i += 2) {
            char symbol = (Character) keys[i];
            ItemLike ingredient = (ItemLike) keys[i + 1];
            builder.define(symbol, ingredient);
            if (unlockItem == null) unlockItem = ingredient;
        }

        if (unlockItem != null) {
            builder.unlockedBy(getHasName(unlockItem), has(unlockItem));
        }

        builder.save(writer, new ResourceLocation(MainClass.MODID, name));
    }

    private void shapelessRecipe(Consumer<FinishedRecipe> writer, RecipeCategory category, ItemLike result, int count, String name, ItemLike... ingredients) {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(category, result, count);
        for (ItemLike ingredient : ingredients) {
            builder.requires(ingredient);
        }

        if (ingredients.length > 0) {
            builder.unlockedBy(getHasName(ingredients[0]), has(ingredients[0]));
        }

        builder.save(writer, new ResourceLocation(MainClass.MODID, name));
    }
}