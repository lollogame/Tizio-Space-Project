package tizio.dev.tsp.registry;

import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.datagen.TSPBiomeTags;
import tizio.dev.tsp.datagen.TSPBlockTags;
import tizio.dev.tsp.resources.BlockFactory;
import tizio.dev.tsp.resources.OreProperties;
import tizio.dev.tsp.resources.OreToolTier;
import tizio.dev.tsp.resources.blocks.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RegisterBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MainClass.MODID);
    public static final Map<String, BlockFactory> BUILDERS = new LinkedHashMap<>();

    public static BlockFactory registerBlock(String name, Supplier<Block> blockSupplier) {
        return new BlockFactory(name, blockSupplier);
    }

    public static <T extends Block> RegistryObject<Item> itemRegistryObject(String name, RegistryObject<T> block) {
        return RegisterItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    public static final RegistryObject<Block> MARS_SAND = registerBlock("mars_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MARS_STONE = registerBlock("mars_stone", StoneBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();
    public static final RegistryObject<Block> MARS_GRAVEL = registerBlock("mars_gravel", GravelBlock::new).randomRotation().build();

    public static final RegistryObject<Block> MOON_SAND = registerBlock("moon_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MOON_DARK_SAND = registerBlock("moon_dark_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MOON_STONE = registerBlock("moon_stone", StoneBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();

    public static final RegistryObject<Block> VENUS_SAND = registerBlock("venus_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> VENUS_STONE = registerBlock("venus_stone", StoneBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();
    public static final RegistryObject<Block> VENUS_GRAVEL = registerBlock("venus_gravel", GravelBlock::new).randomRotation().build();
    public static final RegistryObject<Block> DEBRIS = registerBlock("debris", DebrisBlock::new).randomRotation().customItem("debris").customModel("debris_1").customModel("debris_2").build();

    public static final RegistryObject<Block> DEEPSLATE_TITANIUM_ORE = RegisterOres.registerOre(OreProperties.builder("deepslate_titanium_ore", BlockTags.DEEPSLATE_ORE_REPLACEABLES, BiomeTags.IS_OVERWORLD).toolTier(OreToolTier.IRON).strength(4.5f, 3.0f).drops(() -> RegisterItems.RAW_TITANIUM.get(), 1, 1).smelt(() -> RegisterItems.TITANIUM_INGOT.get(), 1.0f, 200).xp(3, 7).vein(8, 7).height(-64, 16).build());

    public static final RegistryObject<Block> MARS_IRON_ORE = RegisterOres.registerOre(OreProperties.builder("mars_iron_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> Items.RAW_IRON, 1, 1).xp(0, 2).vein(9, 8).height(-55, 33).build());
    public static final RegistryObject<Block> MOON_IRON_ORE = RegisterOres.registerOre(OreProperties.builder("moon_iron_ore", TSPBlockTags.MOON_STONE_ORE_REPLACEABLE, TSPBiomeTags.MOON_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> Items.RAW_IRON, 1, 1).xp(0, 2).vein(9, 8).height(-55, 33).build());
    public static final RegistryObject<Block> VENUS_IRON_ORE = RegisterOres.registerOre(OreProperties.builder("venus_iron_ore", TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE, TSPBiomeTags.VENUS_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> Items.RAW_IRON, 1, 1).xp(0, 2).vein(9, 8).height(-55, 33).build());

    public static final RegistryObject<Block> MARS_LEAD_ORE = RegisterOres.registerOre(OreProperties.builder("mars_lead_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> RegisterItems.RAW_LEAD.get(), 1, 1).smelt(() -> RegisterItems.LEAD_INGOT.get(), 1.0f, 100).xp(0, 2).vein(9, 8).height(-55, 33).build());
    public static final RegistryObject<Block> MOON_LEAD_ORE = RegisterOres.registerOre(OreProperties.builder("moon_lead_ore", TSPBlockTags.MOON_STONE_ORE_REPLACEABLE, TSPBiomeTags.MOON_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> RegisterItems.RAW_LEAD.get(), 1, 1).smelt(() -> RegisterItems.LEAD_INGOT.get(), 1.0f, 100).xp(0, 2).vein(9, 8).height(-55, 33).build());
    public static final RegistryObject<Block> VENUS_LEAD_ORE = RegisterOres.registerOre(OreProperties.builder("venus_lead_ore", TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE, TSPBiomeTags.VENUS_BIOMES).toolTier(OreToolTier.STONE).strength(3f, 3f).drops(() -> RegisterItems.RAW_LEAD.get(), 1, 1).smelt(() -> RegisterItems.LEAD_INGOT.get(), 1.0f, 100).xp(0, 2).vein(9, 8).height(-55, 33).build());

    public static final RegistryObject<Block> MARS_GOLD_ORE = RegisterOres.registerOre(OreProperties.builder("mars_gold_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> Items.RAW_GOLD, 1, 1).xp(0, 1).vein(9, 8).height(-55, -16).build());
    public static final RegistryObject<Block> MOON_GOLD_ORE = RegisterOres.registerOre(OreProperties.builder("moon_gold_ore", TSPBlockTags.MOON_STONE_ORE_REPLACEABLE, TSPBiomeTags.MOON_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> Items.RAW_GOLD, 1, 1).xp(0, 1).vein(9, 8).height(-55, -16).build());
    public static final RegistryObject<Block> VENUS_GOLD_ORE = RegisterOres.registerOre(OreProperties.builder("venus_gold_ore", TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE, TSPBiomeTags.VENUS_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> Items.RAW_GOLD, 1, 1).xp(0, 1).vein(9, 8).height(-55, -16).build());

    public static final RegistryObject<Block> MARS_ALLUMINIUM_ORE = RegisterOres.registerOre(OreProperties.builder("mars_alluminium_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> RegisterItems.RAW_ALLUMINIUM.get(), 1, 1).smelt(() -> RegisterItems.ALLUMINIUM_INGOT.get(), 1.0f, 120).xp(0, 1).vein(8, 8).height(-55, -16).build());
    public static final RegistryObject<Block> MOON_ALLUMINIUM_ORE = RegisterOres.registerOre(OreProperties.builder("moon_alluminium_ore", TSPBlockTags.MOON_STONE_ORE_REPLACEABLE, TSPBiomeTags.MOON_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> RegisterItems.RAW_ALLUMINIUM.get(), 1, 1).smelt(() -> RegisterItems.ALLUMINIUM_INGOT.get(), 1.0f, 120).xp(0, 1).vein(8, 8).height(-55, -16).build());
    public static final RegistryObject<Block> VENUS_ALLUMINIUM_ORE = RegisterOres.registerOre(OreProperties.builder("venus_alluminium_ore", TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE, TSPBiomeTags.VENUS_BIOMES).toolTier(OreToolTier.IRON).strength(3f, 3f).drops(() -> RegisterItems.RAW_ALLUMINIUM.get(), 1, 1).smelt(() -> RegisterItems.ALLUMINIUM_INGOT.get(), 1.0f, 120).xp(0, 1).vein(8, 8).height(-55, -16).build());

    public static final RegistryObject<Block> MARS_TITANIUM_ORE = RegisterOres.registerOre(OreProperties.builder("mars_titanium_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES).toolTier(OreToolTier.DIAMOND).strength(3f, 3f).drops(() -> RegisterItems.RAW_TITANIUM.get(), 1, 1).smelt(() -> RegisterItems.TITANIUM_INGOT.get(), 1.0f, 200).xp(3, 7).vein(4, 4).height(-55, -36).build());
    public static final RegistryObject<Block> MOON_TITANIUM_ORE = RegisterOres.registerOre(OreProperties.builder("moon_titanium_ore", TSPBlockTags.MOON_STONE_ORE_REPLACEABLE, TSPBiomeTags.MOON_BIOMES).toolTier(OreToolTier.DIAMOND).strength(3f, 3f).drops(() -> RegisterItems.RAW_TITANIUM.get(), 1, 1).smelt(() -> RegisterItems.TITANIUM_INGOT.get(), 1.0f, 200).xp(3, 7).vein(4, 4).height(-55, -36).build());
    public static final RegistryObject<Block> VENUS_TITANIUM_ORE = RegisterOres.registerOre(OreProperties.builder("venus_titanium_ore", TSPBlockTags.VENUS_STONE_ORE_REPLACEABLE, TSPBiomeTags.VENUS_BIOMES).toolTier(OreToolTier.DIAMOND).strength(3f, 3f).drops(() -> RegisterItems.RAW_TITANIUM.get(), 1, 1).smelt(() -> RegisterItems.TITANIUM_INGOT.get(), 1.0f, 200).xp(3, 7).vein(4, 4).height(-55, -36).build());

    public static final RegistryObject<Block> RAW_TITANIUM_BLOCK = registerBlock("raw_titanium_block", MetalBlock::new).canSmelt(() -> RegisterItems.TITANIUM_INGOT.get()).build();
    public static final RegistryObject<Block> RAW_ALLUMINIUM_BLOCK = registerBlock("raw_alluminium_block", MetalBlock::new).canSmelt(() -> RegisterItems.ALLUMINIUM_INGOT.get()).build();
    public static final RegistryObject<Block> RAW_LEAD_BLOCK = registerBlock("raw_lead_block", MetalBlock::new).canSmelt(() -> RegisterItems.LEAD_INGOT.get()).build();

    public static final RegistryObject<Block> TITANIUM_BLOCK = registerBlock("titanium_block", MetalBlock::new).craftingRecipes().stonecutter().cutBlock("titanium_block_cut").makeStairs().makeSlab().build();
    public static final RegistryObject<Block> TITANIUM_BLOCK_CUT = registerBlock("titanium_block_cut", MetalBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();

    public static final RegistryObject<Block> ALLUMINIUM_BLOCK = registerBlock("alluminium_block", MetalBlock::new).craftingRecipes().stonecutter().cutBlock("alluminium_block_cut").makeStairs().makeSlab().build();
    public static final RegistryObject<Block> ALLUMINIUM_BLOCK_CUT = registerBlock("alluminium_block_cut", MetalBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();

    public static final RegistryObject<Block> LEAD_BLOCK = registerBlock("lead_block", MetalBlock::new).craftingRecipes().stonecutter().makeStairs().makeSlab().build();

    public static class RegisterOres {

        public static final Map<OreProperties, RegistryObject<Block>> ORES = new LinkedHashMap<>();

        public static RegistryObject<Block> registerOre(OreProperties properties) {

            BlockFactory factory = registerBlock(properties.name(), () -> new OreBlock(properties))
                    .textureFolder("block/ores");

            if (properties.smeltResult() != null) {
                factory.canSmelt(properties.smeltResult(), properties.smeltXp(), properties.smeltTime());
            }

            RegistryObject<Block> block = factory.build();
            ORES.put(properties, block);
            return block;
        }
    }

}
