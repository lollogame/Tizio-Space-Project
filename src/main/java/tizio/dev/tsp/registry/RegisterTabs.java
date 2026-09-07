package tizio.dev.tsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.resources.ArmorProperties;

import java.util.*;

public class RegisterTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MainClass.MODID);

    public static final RegistryObject<CreativeModeTab> BLOCKS_TAB = TABS.register("blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tsp_tabs.blocks"))
                    .icon(() -> new ItemStack(RegisterBlocks.MARS_STONE.get()))
                    .displayItems((parameters, output) -> {
                        RegisterBlocks.BLOCKS.getEntries().stream()
                                .map(RegistryObject::get)
                                .sorted(Comparator
                                        .<Block, Integer>comparing(RegisterTabs::getBlockCategoryPriority)
                                        .thenComparing(block -> Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getPath())
                                )
                                .forEach(block -> output.accept(new ItemStack(block)));
                    })
                    .build()
    );

    public static final RegistryObject<CreativeModeTab> MATERIALS_TAB = TABS.register("materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tsp_tabs.materials"))
                    .icon(() -> new ItemStack(RegisterItems.TITANIUM_INGOT.get()))
                    .displayItems((parameters, output) -> {
                        Set<Item> armorItems = new HashSet<>();
                        RegisterItems.ARMOR_PIECES.values().forEach(pieces ->
                                pieces.values().forEach(piece -> armorItems.add(piece.get())));

                        RegisterItems.ITEMS.getEntries().stream()
                                .map(RegistryObject::get)
                                .filter(item -> !(item instanceof BlockItem))
                                .filter(item -> !armorItems.contains(item))
                                .sorted(Comparator
                                        .<Item, Integer>comparing(RegisterTabs::getItemCategoryPriority)
                                        .thenComparing(item -> Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).getPath())
                                )
                                .forEach(item -> output.accept(new ItemStack(item)));
                    })
                    .build()
    );

    public static final RegistryObject<CreativeModeTab> ARMORS_TAB = TABS.register("armors",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tsp_tabs.armors"))
                    .icon(() -> {
                        var pieces = RegisterItems.ARMOR_PIECES.get("space_suit_1_orange");
                        if (pieces != null && pieces.containsKey(ArmorItem.Type.HELMET)) {
                            return new ItemStack(pieces.get(ArmorItem.Type.HELMET).get());
                        }
                        return new ItemStack(Items.BARRIER);
                    })
                    .displayItems((parameters, output) -> {
                        RegisterItems.ARMOR_REGISTRY.values().stream()
                                .sorted(Comparator.comparing(ArmorProperties::getId))
                                .forEach(properties -> {
                                    Map<ArmorItem.Type, RegistryObject<Item>> pieces = RegisterItems.ARMOR_PIECES.get(properties.getId());
                                    if (pieces == null) return;
                                    if (pieces.containsKey(ArmorItem.Type.HELMET)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.HELMET).get()));
                                    if (pieces.containsKey(ArmorItem.Type.CHESTPLATE)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.CHESTPLATE).get()));
                                    if (pieces.containsKey(ArmorItem.Type.LEGGINGS)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.LEGGINGS).get()));
                                    if (pieces.containsKey(ArmorItem.Type.BOOTS)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.BOOTS).get()));
                                });
                    })
                    .build()
    );

    private static int getBlockCategoryPriority(Block block) {
        String path = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(block)).getPath();

        if (path.endsWith("_stairs")) return 5;
        if (path.endsWith("_slab")) return 6;
        if (path.startsWith("raw_")) return 3;
        if (path.endsWith("_ore")) return 2;
        if (path.endsWith("_block") || path.contains("_block_")) return 4;

        return 1;
    }

    private static int getItemCategoryPriority(Item item) {
        String path = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).getPath();

        if (path.startsWith("raw_") || path.endsWith("_raw")) return 1;
        if (path.endsWith("_ingot")) return 2;
        if (path.endsWith("_nugget")) return 3;

        return 4;
    }

    public static void register(IEventBus event) {
        TABS.register(event);
    }
}