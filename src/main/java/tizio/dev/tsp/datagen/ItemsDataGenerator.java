package tizio.dev.tsp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.registry.RegisterItems;
import tizio.dev.tsp.resources.ArmorProperties;

import java.util.Arrays;
import java.util.Map;

public class ItemsDataGenerator extends ItemModelProvider {

    private final String[] itemBlackList = {"debris"};

    public ItemsDataGenerator(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MainClass.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        RegisterBlocks.BLOCKS.getEntries().forEach(block -> {
            String name = block.getId().getPath();
            if (Arrays.asList(itemBlackList).contains(name)) return;
            withExistingParent(name, ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "block/" + name));
        });

        RegisterItems.ITEMS.getEntries().forEach(itemObject -> {
            String name = itemObject.getId().getPath();

            boolean isBlockItem = RegisterBlocks.BLOCKS.getEntries().stream()
                    .anyMatch(b -> b.getId().getPath().equals(name));
            boolean isArmor = RegisterItems.ARMOR_PIECES.values().stream()
                    .anyMatch(map -> map.containsValue(itemObject));

            if (!isBlockItem && !isArmor) {
                basicItem(itemObject.get());
            }
        });

        for (Map.Entry<String, ArmorProperties> entry : RegisterItems.ARMOR_REGISTRY.entrySet()) {
            ArmorProperties properties = entry.getValue();
            Map<ArmorItem.Type, RegistryObject<Item>> pieces = RegisterItems.ARMOR_PIECES.get(properties.getId());

            if (pieces == null) continue;

            for (Map.Entry<ArmorItem.Type, RegistryObject<Item>> pieceEntry : pieces.entrySet()) {
                ArmorItem.Type pieceType = pieceEntry.getKey();
                String itemName = pieceEntry.getValue().getId().getPath();
                String customJson = properties.getCustomItemModel(pieceType);

                if (customJson != null) {
                    withExistingParent(itemName, ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "custom/armor_items/" + customJson))
                            .texture("0", ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "item/armor/" + itemName));
                } else {
                    withExistingParent(itemName, ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"))
                            .texture("layer0", ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "item/armor/" + itemName));
                }
            }
        }
    }
}
