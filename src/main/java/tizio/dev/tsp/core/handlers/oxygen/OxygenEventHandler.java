package tizio.dev.tsp.core.handlers.oxygen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.handlers.temperature.TemperatureManager;
import tizio.dev.tsp.core.network.PacketsRegistry;
import tizio.dev.tsp.core.network.SyncPlayerStatusPacket;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class OxygenEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (event.player instanceof ServerPlayer serverPlayer) {
            OxygenManager.tickOxygen(serverPlayer);

            if (serverPlayer.tickCount % 10 == 0) {
                syncStatusToClient(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncStatusToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            OxygenManager.setOxygen(serverPlayer, OxygenManager.MAX_OXYGEN);
            TemperatureManager.setTemperature(serverPlayer, TemperatureManager.NEUTRAL_TEMPERATURE);
            syncStatusToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncStatusToClient(serverPlayer);
        }
    }

    public static void syncStatusToClient(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        float oxygen = OxygenManager.getOxygen(player);
        float temperature = TemperatureManager.getTemperature(player);
        PacketsRegistry.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerStatusPacket(oxygen, temperature));
    }
}
