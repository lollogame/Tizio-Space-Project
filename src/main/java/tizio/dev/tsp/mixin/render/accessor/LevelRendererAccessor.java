package tizio.dev.tsp.mixin.render.accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("transparencyChain")
    @Nullable
    PostChain getTransparencyChain();

    @Accessor("entityEffect")
    @Nullable
    PostChain getEntityEffect();
}
