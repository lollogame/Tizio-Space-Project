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
import tizio.dev.tsp.resources.armor.CustomArmorItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MainClass.MODID);

    public static final Map<String, ArmorProperties> ARMOR_REGISTRY = new LinkedHashMap<>();
    public static final Map<String, Map<ArmorItem.Type, RegistryObject<Item>>> ARMOR_PIECES = new HashMap<>();

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

    //ARMORS REGISTRY
    public static final ArmorProperties SPACE_SUIT_TIER_1_ORANGE = registerArmor(ArmorProperties.create("Space Suit - Orange", "space_suit_1_orange", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_RED =    registerArmor(ArmorProperties.create("Space Suit - Red", "space_suit_1_red", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_YELLOW = registerArmor(ArmorProperties.create("Space Suit - Yellow", "space_suit_1_yellow", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_GREEN =  registerArmor(ArmorProperties.create("Space Suit - Green", "space_suit_1_green", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_BLUE =   registerArmor(ArmorProperties.create("Space Suit - Blue", "space_suit_1_blue", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_ACQUA =  registerArmor(ArmorProperties.create("Space Suit - Acqua", "space_suit_1_acqua", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_PINK =   registerArmor(ArmorProperties.create("Space Suit - Pink", "space_suit_1_pink", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_PURPLE = registerArmor(ArmorProperties.create("Space Suit - Purple", "space_suit_1_purple", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));
    public static final ArmorProperties SPACE_SUIT_TIER_1_WHITE =  registerArmor(ArmorProperties.create("Space Suit - White", "space_suit_1_white", ArmorTier.TIER_1, ArmorMaterials.IRON).with3DModel().withCustomHelmet("helmet"));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}