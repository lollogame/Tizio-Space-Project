package tizio.dev.tsp.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.client.models.ModelBipedLayered;
import tizio.dev.tsp.registry.RegisterItems;
import tizio.dev.tsp.resources.ArmorProperties;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorRenderRegistry {

    private static final Map<String, HumanoidModel<?>> BAKED_MODELS = new HashMap<>();
    private static final Map<String, Supplier<LayerDefinition>> CUSTOM_LAYER_DEFINITIONS = new HashMap<>();
    private static final Map<String, Function<ModelPart, HumanoidModel<?>>> CUSTOM_MODEL_FACTORIES = new HashMap<>();


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        //modelli alle armor

        ArmorRenderRegistry.registerCustomModel("space_suit_1_orange", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_red", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_yellow", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_green", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_blue", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_acqua", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_pink", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_purple", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);
        ArmorRenderRegistry.registerCustomModel("space_suit_1_white", ModelBipedLayered::createBodyLayer, ModelBipedLayered::new);

    }

    public static ModelLayerLocation getLayerLocation(String armorId) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, armorId), "main");
    }

    public static void registerCustomModel(String armorId, Supplier<LayerDefinition> layerDef, Function<ModelPart, HumanoidModel<?>> factory) {
        CUSTOM_LAYER_DEFINITIONS.put(armorId, layerDef);
        CUSTOM_MODEL_FACTORIES.put(armorId, factory);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (ArmorProperties armor : RegisterItems.ARMOR_REGISTRY.values()) {
            if (armor.hasCustom3DModel()) {
                Supplier<LayerDefinition> layerSupplier = CUSTOM_LAYER_DEFINITIONS.getOrDefault(armor.getId(), ModelBipedLayered::createBodyLayer);
                event.registerLayerDefinition(getLayerLocation(armor.getId()), layerSupplier);
            }
        }

    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        for (ArmorProperties armor : RegisterItems.ARMOR_REGISTRY.values()) {
            if (armor.hasCustom3DModel()) {
                ModelPart root = modelSet.bakeLayer(getLayerLocation(armor.getId()));
                Function<ModelPart, HumanoidModel<?>> factory = CUSTOM_MODEL_FACTORIES.getOrDefault(armor.getId(), ModelBipedLayered::new);
                HumanoidModel<?> baked = factory.apply(root);
                BAKED_MODELS.put(armor.getId(), baked);
            }
        }
    }

    @Nullable
    public static HumanoidModel<?> getBakedModel(String id) {
        return BAKED_MODELS.get(id);
    }

    public static final IClientItemExtensions CLIENT_ARMOR_EXTENSION = new IClientItemExtensions() {
        @Override
        @SuppressWarnings({"rawtypes"})
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
            if (itemStack.getItem() instanceof CustomArmorItem customArmorItem) {
                ArmorProperties properties = customArmorItem.getArmorProperties();
                if (properties != null && properties.hasCustom3DModel()) {
                    HumanoidModel customModel = getBakedModel(properties.getId());

                    if (customModel != null) {
                        if (customModel instanceof ModelBipedLayered<?> layeredModel) {
                            layeredModel.setTexture(ResourceLocation.fromNamespaceAndPath(MainClass.MODID, "textures/armors/" + properties.getId() + ".png"));
                        }

                        customModel.young = original.young;
                        customModel.crouching = original.crouching;
                        customModel.riding = original.riding;
                        customModel.rightArmPose = original.rightArmPose;
                        customModel.leftArmPose = original.leftArmPose;

                        customModel.head.visible = equipmentSlot == EquipmentSlot.HEAD;
                        customModel.hat.visible = equipmentSlot == EquipmentSlot.HEAD;
                        customModel.body.visible = equipmentSlot == EquipmentSlot.CHEST || equipmentSlot == EquipmentSlot.LEGS;
                        customModel.rightArm.visible = equipmentSlot == EquipmentSlot.CHEST;
                        customModel.leftArm.visible = equipmentSlot == EquipmentSlot.CHEST;
                        customModel.rightLeg.visible = equipmentSlot == EquipmentSlot.LEGS || equipmentSlot == EquipmentSlot.FEET;
                        customModel.leftLeg.visible = equipmentSlot == EquipmentSlot.LEGS || equipmentSlot == EquipmentSlot.FEET;

                        return customModel;
                    }
                }
            }

            return original;
        }
    };
}