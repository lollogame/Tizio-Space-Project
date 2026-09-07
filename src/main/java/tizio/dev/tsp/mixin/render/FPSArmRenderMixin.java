package tizio.dev.tsp.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.client.ArmorRenderRegistry;
import tizio.dev.tsp.resources.ArmorProperties;
import tizio.dev.tsp.resources.armor.CustomArmorItem;

@Mixin(ItemInHandRenderer.class)
public class FPSArmRenderMixin {

    @Inject(method = "renderPlayerArm", at = @At("TAIL"))
    private void tsp$renderArmorSleeve(PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, float equipAnim, float attackAnim, HumanoidArm arm, CallbackInfo ci) {
        AbstractClientPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (chestplate.getItem() instanceof CustomArmorItem armorItem) {
            ArmorProperties properties = armorItem.getArmorProperties();

            if (properties.hasCustom3DModel()) {
                HumanoidModel<?> customModel = ArmorRenderRegistry.getBakedModel(properties.getId());

                if (customModel != null) {
                    boolean isRightArm = arm == HumanoidArm.RIGHT;
                    ModelPart armPart = isRightArm ? customModel.rightArm : customModel.leftArm;

                    armPart.visible = true;

                    armPart.resetPose();
                    armPart.x = isRightArm ? -5.0F : 5.0F;
                    armPart.y = 2.0F;
                    armPart.z = 0.0F;

                    ResourceLocation armorTexture = ResourceLocation.fromNamespaceAndPath(
                            MainClass.MODID, "textures/armors/" + properties.getId() + ".png"
                    );

                    VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(armorTexture));

                    poseStack.pushPose();
                    armPart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                    poseStack.popPose();
                }
            }
        }
    }
}
