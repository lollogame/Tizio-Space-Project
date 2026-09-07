package tizio.dev.tsp.resources;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class ArmorProperties {
    private final String displayName;
    private final String id;
    private final ArmorTier type;
    private final ArmorMaterial material;
    private final boolean custom3DModel;

    private final Map<ArmorItem.Type, String> customItemModels = new EnumMap<>(ArmorItem.Type.class);

    private ArmorProperties(Builder builder) {
        this.displayName = builder.displayName;
        this.id = builder.id;
        this.type = builder.type;
        this.material = builder.material;
        this.custom3DModel = builder.custom3DModel;
        this.customItemModels.putAll(builder.customItemModels);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ArmorTier getType() { return type; }
    public ArmorMaterial getMaterial() { return material; }
    public boolean hasCustom3DModel() { return custom3DModel; }

    @Nullable
    public String getCustomItemModel(ArmorItem.Type pieceType) {
        return customItemModels.get(pieceType);
    }

    public static Builder create(String displayName, String id, ArmorTier type, ArmorMaterial material) {
        return new Builder(displayName, id, type, material);
    }

    public static class Builder {
        private final String displayName;
        private final String id;
        private final ArmorTier type;
        private final ArmorMaterial material;
        private boolean custom3DModel = false;
        private final Map<ArmorItem.Type, String> customItemModels = new EnumMap<>(ArmorItem.Type.class);

        public Builder(String displayName, String id, ArmorTier type, ArmorMaterial material) {
            this.displayName = displayName;
            this.id = id;
            this.type = type;
            this.material = material;
        }

        public Builder with3DModel() {
            this.custom3DModel = true;
            return this;
        }

        public Builder withCustomHelmet(String jsonModel) {
            customItemModels.put(ArmorItem.Type.HELMET, jsonModel);
            return this;
        }

        public Builder withCustomChestplate(String jsonModel) {
            customItemModels.put(ArmorItem.Type.CHESTPLATE, jsonModel);
            return this;
        }

        public Builder withCustomLeggings(String jsonModel) {
            customItemModels.put(ArmorItem.Type.LEGGINGS, jsonModel);
            return this;
        }

        public Builder withCustomBoots(String jsonModel) {
            customItemModels.put(ArmorItem.Type.BOOTS, jsonModel);
            return this;
        }

        public ArmorProperties build() {
            return new ArmorProperties(this);
        }
    }
}
