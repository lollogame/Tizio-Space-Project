package tizio.dev.tsp.core.handlers.gravity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class GravityEventHandler {

    private GravityEventHandler() {}

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        CelestialJsonLoader.ensureLoaded();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.isRemoved()) return;

        applyLivingGravity(entity);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            applyLivingGravity(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            applyLivingGravity(player);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        float scale = GravityManager.getFallDamageScale(entity);
        if (scale <= 0.0F) {
            event.setCanceled(true);
            event.setDistance(0.0F);
            return;
        }

        if (scale < 1.0F) {
            event.setDistance(event.getDistance() * scale);
        }
    }

    private static void applyLivingGravity(LivingEntity entity) {
        AttributeInstance gravityAttr = entity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravityAttr == null) return;

        double targetGravity = GravityManager.getLivingGravity(entity);
        if (Math.abs(gravityAttr.getBaseValue() - targetGravity) > 1e-5) {
            gravityAttr.setBaseValue(targetGravity);
        }
    }
}
