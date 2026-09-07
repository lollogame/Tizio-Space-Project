package tizio.dev.tsp.mixin.render.lighting;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tizio.dev.tsp.core.celestial.lighting.LightShadeManager;

@OnlyIn(Dist.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ShadingLevelMixin {

    @Overwrite
    public float getShade(Direction direction, boolean shade) {
        if (!shade) return 1.0F;
        if (LightShadeManager.isSpaceLightingActive()) {
            return LightShadeManager.getBlockFaceShade(direction);
        }
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }
}
