package tizio.dev.tsp.registry;


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
import tizio.dev.tsp.resources.blocks.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RegisterBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MainClass.MODID);

    // Mappa dove vengono salvate le configurazioni di generazione dei blocchi
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

    //-------------------------------------------------------------------------------------------------
    // REGISTRAZIONI CON NUOVA FLUENT API

    public static final RegistryObject<Block> MARS_SAND = registerBlock("mars_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MARS_STONE = registerBlock("mars_stone", StoneBlock::new).makeStairs().makeSlab().build();
    public static final RegistryObject<Block> MARS_GRAVEL = registerBlock("mars_gravel", GravelBlock::new).randomRotation().build();

    public static final RegistryObject<Block> MOON_SAND = registerBlock("moon_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MOON_DARK_SAND = registerBlock("moon_dark_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> MOON_STONE = registerBlock("moon_stone", StoneBlock::new).makeStairs().makeSlab().build();

    public static final RegistryObject<Block> VENUS_SAND = registerBlock("venus_sand", SandBlock::new).randomRotation().build();
    public static final RegistryObject<Block> VENUS_STONE = registerBlock("venus_stone", StoneBlock::new).makeStairs().makeSlab().build();
    public static final RegistryObject<Block> VENUS_GRAVEL = registerBlock("venus_gravel", GravelBlock::new).randomRotation().build();
    public static final RegistryObject<Block> DEBRIS = registerBlock("debris", DebrisBlock::new).randomRotation().customItem("debris").customModel("debris_1").customModel("debris_2").build();

    //-------------------------------------------------------------------------------------------------

    public static final RegistryObject<Block> MARS_IRON_ORE = RegisterOres.registerOre(
            OreProperties.builder("mars_iron_ore", TSPBlockTags.MARS_STONE_ORE_REPLACEABLE, TSPBiomeTags.MARS_BIOMES)
                    .strength(3f, 3f)
                    .drops(() -> Items.RAW_IRON, 1, 1)
                    .xp(0, 2)
                    .vein(9, 8)
                    .height(-55, 33)
                    .build()
    );

    //-------------------------------------------------------------------------------------------------

    public static class RegisterOres {
        public static final Map<OreProperties, RegistryObject<Block>> ORES = new LinkedHashMap<>();

        public static RegistryObject<Block> registerOre(OreProperties properties) {
            RegistryObject<Block> block = BLOCKS.register(properties.name(), () -> new OreBlock(properties));
            itemRegistryObject(properties.name(), block);
            ORES.put(properties, block);
            return block;
        }
    }
}
