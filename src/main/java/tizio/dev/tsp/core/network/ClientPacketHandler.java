package tizio.dev.tsp.core.network;

import net.minecraft.client.Minecraft;
import tizio.dev.tsp.core.handlers.oxygen.OxygenManager;
import tizio.dev.tsp.core.handlers.temperature.TemperatureManager;

public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void handlePlayerStatus(float oxygen, float temperature) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            OxygenManager.setOxygen(mc.player, oxygen);
            TemperatureManager.setTemperature(mc.player, temperature);
        }
    }
}
