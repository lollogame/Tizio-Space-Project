package tizio.dev.tsp.core.handlers.temperature;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.tsp.MainClass;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class TemperatureEventHandler {

    private TemperatureEventHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (event.player instanceof ServerPlayer serverPlayer) {
            TemperatureManager.tickTemperature(serverPlayer);
        }
    }
}
