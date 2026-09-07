package tizio.dev.tsp.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.registry.RegisterItems;
import tizio.dev.tsp.registry.RegisterTabs;
import tizio.dev.tsp.resources.ArmorProperties;

import java.util.Map;

public class TSPLanguageGenerator extends LanguageProvider {

    public TSPLanguageGenerator(PackOutput output) {
        super(output, MainClass.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {

        this.add("key.categories.tsp", "T.S.P - Keybinds");
        this.add("key.tsp.planet_editor", "Development Editor");

        for (RegistryObject<Block> blockObject : RegisterBlocks.BLOCKS.getEntries()) {
            Block block = blockObject.get();
            String registryName = blockObject.getId().getPath();
            add(block, formatName(registryName));
        }

        for (RegistryObject<Item> itemObject : RegisterItems.ITEMS.getEntries()) {
            Item item = itemObject.get();
            String registryName = itemObject.getId().getPath();

            if (item instanceof BlockItem) continue;

            boolean isArmorPiece = RegisterItems.ARMOR_PIECES.values().stream()
                    .anyMatch(map -> map.containsValue(itemObject));

            if (!isArmorPiece) {
                add(item, formatName(registryName));
            }
        }

        for (RegistryObject<CreativeModeTab> tabObject : RegisterTabs.TABS.getEntries()) {
            String registryName = tabObject.getId().getPath();
            String translationKey = "tsp_tabs." + registryName;
            add(translationKey, formatTabName(registryName));
        }

        for (Map.Entry<String, ArmorProperties> entry : RegisterItems.ARMOR_REGISTRY.entrySet()) {
            ArmorProperties properties = entry.getValue();
            Map<ArmorItem.Type, RegistryObject<Item>> pieces = RegisterItems.ARMOR_PIECES.get(properties.getId());

            if (pieces == null) continue;

            for (Map.Entry<ArmorItem.Type, RegistryObject<Item>> pieceEntry : pieces.entrySet()) {
                ArmorItem.Type type = pieceEntry.getKey();
                Item pieceItem = pieceEntry.getValue().get();

                String pieceSuffix = switch (type) {
                    case HELMET -> "Helmet";
                    case CHESTPLATE -> "Chestplate";
                    case LEGGINGS -> "Leggings";
                    case BOOTS -> "Boots";
                };

                String fullName = properties.getDisplayName() + " " + pieceSuffix;
                add(pieceItem, fullName);
            }
        }
    }

    private String formatName(String registryName) {
        String[] words = registryName.split("_");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }

        return formattedName.toString().trim();
    }

    private String formatTabName(String registryName) {
        String cleanedName = registryName.replace("tsp_", "").replace("_tab", "");

        if (cleanedName.isEmpty()) {
            cleanedName = registryName;
        }

        return formatName(cleanedName);
    }
}
