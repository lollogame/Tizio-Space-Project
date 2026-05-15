package tizio.dev.tsp.mixin.entity;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.tsp.utils.GravityUtil;

@Mixin(ItemEntity.class)
public class ItemGravity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        ItemEntity itemTarget = (ItemEntity) (Object) this;
        if (itemTarget.isNoGravity()) return;

        itemTarget.setDeltaMovement(
                itemTarget.getDeltaMovement().x,
                (itemTarget.getDeltaMovement().y + GravityUtil.Item.getGravity(itemTarget)),
                itemTarget.getDeltaMovement().z
        );

    }
}
