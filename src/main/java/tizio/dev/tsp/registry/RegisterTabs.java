package tizio.dev.tsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.resources.ArmorProperties;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class RegisterTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MainClass.MODID);

    public static final RegistryObject<CreativeModeTab> BLOCKS_TAB = TABS.register("blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tsp_tabs.blocks"))
                    .icon(() -> new ItemStack(RegisterBlocks.BLOCKS.getEntries().stream().findFirst().get().get()))
                    .displayItems((parameters, output) -> {
                        RegisterBlocks.BLOCKS.getEntries().stream()
                                .sorted(Comparator.comparing(entry ->
                                        Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(entry.get())).getPath()
                                ))
                                .forEach(entry -> output.accept(new ItemStack(entry.get())));
                    })
                    .build()
    );


    public static final RegistryObject<CreativeModeTab> ARMORS_TAB = TABS.register("armors",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tsp_tabs.armors"))

                    .icon(() -> {
                        var firstArmor = RegisterItems.ARMOR_PIECES.values().stream().findFirst();
                        if (firstArmor.isPresent()) {
                            var helmet = firstArmor.get().get(ArmorItem.Type.HELMET);
                            if (helmet != null) return new ItemStack(helmet.get());
                        }
                        return ItemStack.EMPTY;
                    })
                    .displayItems((parameters, output) -> {
                        for (ArmorProperties properties : RegisterItems.ARMOR_REGISTRY.values()) {
                            Map<ArmorItem.Type, RegistryObject<Item>> pieces = RegisterItems.ARMOR_PIECES.get(properties.getId());
                            if (pieces != null) {
                                if (pieces.containsKey(ArmorItem.Type.HELMET)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.HELMET).get()));
                                if (pieces.containsKey(ArmorItem.Type.CHESTPLATE)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.CHESTPLATE).get()));
                                if (pieces.containsKey(ArmorItem.Type.LEGGINGS)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.LEGGINGS).get()));
                                if (pieces.containsKey(ArmorItem.Type.BOOTS)) output.accept(new ItemStack(pieces.get(ArmorItem.Type.BOOTS).get()));
                            }
                        }
                    })
                    .build()
    );

    public static void register(IEventBus event) {
        TABS.register(event);
    }
}