package tizio.dev.tsp.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.resources.ArmorProperties;
import tizio.dev.tsp.resources.ArmorTier;
import tizio.dev.tsp.resources.ItemFactory;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RegisterItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MainClass.MODID);

    public static final Map<String, ItemFactory> ITEM_BUILDERS = new LinkedHashMap<>();
    public static final Map<String, ArmorProperties> ARMOR_REGISTRY = new LinkedHashMap<>();
    public static final Map<String, Map<ArmorItem.Type, RegistryObject<Item>>> ARMOR_PIECES = new HashMap<>();

    public static ItemFactory registerItem(String name) {
        return new ItemFactory(name);
    }

    public static ItemFactory registerItem(String name, Supplier<Item> itemSupplier) {
        return new ItemFactory(name, itemSupplier);
    }

    public static final RegistryObject<Item> LEAD_NUGGET = registerItem("lead_nugget").build();
    public static final RegistryObject<Item> LEAD_INGOT = registerItem("lead_ingot").makeNuggets(() -> LEAD_NUGGET.get()).makeBlock(() -> RegisterBlocks.LEAD_BLOCK.get()).build();
    public static final RegistryObject<Item> RAW_LEAD = registerItem("raw_lead").canSmelt(() -> LEAD_INGOT.get()).makeBlock(() -> RegisterBlocks.RAW_LEAD_BLOCK.get()).build();

    public static final RegistryObject<Item> ALLUMINIUM_NUGGET = registerItem("alluminium_nugget").build();
    public static final RegistryObject<Item> ALLUMINIUM_INGOT = registerItem("alluminium_ingot").makeNuggets(() -> ALLUMINIUM_NUGGET.get()).makeBlock(() -> RegisterBlocks.ALLUMINIUM_BLOCK.get()).build();
    public static final RegistryObject<Item> RAW_ALLUMINIUM = registerItem("raw_alluminium").canSmelt(() -> ALLUMINIUM_INGOT.get()).makeBlock(() -> RegisterBlocks.RAW_ALLUMINIUM_BLOCK.get()).build();

    public static final RegistryObject<Item> TITANIUM_NUGGET = registerItem("titanium_nugget").build();
    public static final RegistryObject<Item> TITANIUM_INGOT = registerItem("titanium_ingot").makeNuggets(() -> TITANIUM_NUGGET.get()).makeBlock(() -> RegisterBlocks.TITANIUM_BLOCK.get()).build();
    public static final RegistryObject<Item> RAW_TITANIUM = registerItem("raw_titanium").canSmelt(() -> TITANIUM_INGOT.get()).makeBlock(() -> RegisterBlocks.RAW_TITANIUM_BLOCK.get()).build();

    public static final RegistryObject<Item> CIRCUIT_BOARD = registerItem("circuit_board").build();

    public static ArmorProperties registerArmor(ArmorProperties.Builder builder) {
        ArmorProperties properties = builder.build();
        ARMOR_REGISTRY.put(properties.getId(), properties);

        Map<ArmorItem.Type, RegistryObject<Item>> pieces = new EnumMap<>(ArmorItem.Type.class);

        for (ArmorItem.Type pieceType : ArmorItem.Type.values()) {
            String registryName = properties.getId() + "_" + pieceType.getName();
            pieces.put(pieceType, ITEMS.register(registryName, () -> new CustomArmorItem(pieceType, properties, new Item.Properties())));
        }

        ARMOR_PIECES.put(properties.getId(), pieces);
        return properties;
    }

    public static final ArmorProperties SPACE_SUIT_TIER_1_ORANGE = registerArmor(ArmorProperties.create("Space Suit - Orange", "space_suit_1_orange", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_RED =    registerArmor(ArmorProperties.create("Space Suit - Red", "space_suit_1_red", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_YELLOW = registerArmor(ArmorProperties.create("Space Suit - Yellow", "space_suit_1_yellow", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_GREEN =  registerArmor(ArmorProperties.create("Space Suit - Green", "space_suit_1_green", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_BLUE =   registerArmor(ArmorProperties.create("Space Suit - Blue", "space_suit_1_blue", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_CYAN =   registerArmor(ArmorProperties.create("Space Suit - Cyan", "space_suit_1_cyan", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_PINK =   registerArmor(ArmorProperties.create("Space Suit - Pink", "space_suit_1_pink", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_PURPLE = registerArmor(ArmorProperties.create("Space Suit - Purple", "space_suit_1_purple", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_WHITE =  registerArmor(ArmorProperties.create("Space Suit - White", "space_suit_1_white", ArmorTier.TIER_1, ArmorMaterials.DIAMOND).with3DModel().withCustomHelmet("helmet"));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
