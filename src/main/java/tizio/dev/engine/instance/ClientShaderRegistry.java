package tizio.dev.engine.instance;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

import java.io.IOException;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientShaderRegistry {

    private static ShaderInstance blackHoleShader;
    private static ShaderInstance atmosphereShader;
    private static ShaderInstance sunShader;
    private static ShaderInstance planetSurface;
    private static ShaderInstance planetRing;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        addShader(event, "black_hole", shader-> blackHoleShader = shader);
        addShader(event, "planet_atmosphere", shader-> atmosphereShader = shader);
        addShader(event, "planet_surface", shader-> planetSurface = shader);
        addShader(event, "planet_ring", shader-> planetRing = shader);
        addShader(event, "sun", shader-> sunShader = shader);
    }

    private static void addShader(RegisterShadersEvent event, String shaderId, Consumer<ShaderInstance> setter) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(MainClass.MODID, shaderId), DefaultVertexFormat.POSITION_TEX),
                setter
        );
    }

    public static ShaderInstance blackHoleShader() { return blackHoleShader; }
    public static ShaderInstance atmosphereShader() { return atmosphereShader; }
    public static ShaderInstance sunShader() { return sunShader; }
    public static ShaderInstance planetSurface() { return planetSurface; }
    public static ShaderInstance planetRing() { return planetRing; }
}
