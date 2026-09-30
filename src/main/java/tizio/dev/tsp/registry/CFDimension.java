package tizio.dev.tsp.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.data.CelestialJsonLoader;
import tizio.dev.tsp.engine.lighting.LightShadeManager;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class CFDimension {

    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class SkySetup {

        private static RegisterDimensionSpecialEffectsEvent provider = null;

        public static void register(ResourceKey<Level> dimension, DimensionSpecialEffects effects) {
            provider.register(dimension.location(), effects);
        }

        public static DimensionSpecialEffects createOverworldEffects(final boolean constantWhiteLight, final boolean constantAmbientLight, final boolean fog) {
            return new TspDimensionEffects(192.0F, true, DimensionSpecialEffects.SkyType.NORMAL, constantWhiteLight, constantAmbientLight) {
                @Override
                public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
                    return color.multiply(sunHeight * 0.94F + 0.06F, sunHeight * 0.94F + 0.06F, sunHeight * 0.91F + 0.09F);
                }

                @Override
                public boolean isFoggyAt(int x, int y) {
                    return fog;
                }
            };
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void setupDimensions(RegisterDimensionSpecialEffectsEvent event) {
            provider = event;
            run(event);
        }

        public static void run() {
            run(null);
        }

        private static void run(@Nullable Event event) {
            DimensionSpecialEffects effects = createOverworldEffects(false, false, false);
            register(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MainClass.MODID, "space")), effects);
            register(Level.OVERWORLD, effects);

            for (String dimId : CelestialJsonLoader.getAllConfiguredSpaceDimensions()) {
                ResourceLocation location = ResourceLocation.tryParse(dimId);
                if (location != null) {
                    register(ResourceKey.create(Registries.DIMENSION, location), effects);
                }
            }
        }

        public static abstract class TspDimensionEffects extends DimensionSpecialEffects {
            public TspDimensionEffects(float cloudHeight, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
                super(cloudHeight, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
            }

            @Override
            public void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken, float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors) {
                LightShadeManager.adjustLightmapColors(level, partialTick, skyDarken, blockLightRedFlicker, skyLight, pixelX, pixelY, colors);
            }

            @Override
            public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
                return tizio.dev.tsp.engine.celestial.renderer.environment.SkyEnvironmentRenderer.render(
                        level, ticks, partialTick, poseStack, camera, projectionMatrix, setupFog
                );
            }
        }
    }
}
