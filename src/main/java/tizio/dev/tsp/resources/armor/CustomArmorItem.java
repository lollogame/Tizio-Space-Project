package tizio.dev.tsp.resources.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.client.ArmorRenderRegistry;
import tizio.dev.tsp.resources.ArmorProperties;

import java.util.function.Consumer;

public class CustomArmorItem extends ArmorItem {
    private final ArmorProperties properties;

    public CustomArmorItem(Type type, ArmorProperties properties, Properties itemProperties) {
        super(properties.getMaterial(), type, itemProperties);
        this.properties = properties;
    }

    public ArmorProperties getArmorProperties() {
        return properties;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (properties.hasCustom3DModel()) {
            return MainClass.MODID + ":textures/armors/" + properties.getId() + ".png";
        }

        String layer = (slot == EquipmentSlot.LEGS) ? "2" : "1";
        return MainClass.MODID + ":textures/armors/" + properties.getId() + "_layer_" + layer + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ArmorRenderRegistry.CLIENT_ARMOR_EXTENSION);
    }
}