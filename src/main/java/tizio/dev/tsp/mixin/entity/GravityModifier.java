package tizio.dev.tsp.mixin.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tizio.dev.tsp.utils.GravityUtil;

@Mixin(LivingEntity.class)
public class GravityModifier {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tsp$applyDimensionGravity(CallbackInfo ci) {

        LivingEntity entity = (LivingEntity) (Object) this;
        AttributeInstance gravity = entity.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("forge", "entity_gravity")));
        if (gravity == null) return;
        double value = GravityUtil.getGravity(entity);
        gravity.setBaseValue(value);

    }

    //FALL DAMAGE FIXER

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void tsp$modifyFallDamage(float fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        float scale = GravityUtil.FallDamage.getDamageScale(entity);

        if (scale <= 0f) {
            cir.setReturnValue(false);
            return;
        }

        float newDistance = fallDistance * scale;

        if (newDistance <= 3.0F) {
            cir.setReturnValue(false);
            return;
        }

        entity.hurt(source, (newDistance - 3.0F) * damageMultiplier);

        cir.setReturnValue(true);
    }
}
