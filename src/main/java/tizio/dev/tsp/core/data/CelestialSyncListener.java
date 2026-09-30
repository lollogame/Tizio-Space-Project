package tizio.dev.tsp.core.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.core.network.SyncSystemDataPacket;
import tizio.dev.tsp.registry.RegisterPackets;

@Mod.EventBusSubscriber(modid = MainClass.MODID)
public final class CelestialSyncListener {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        ResourceManager resourceManager = event.getPlayerList().getServer().getResourceManager();
        ServerPlayer player = event.getPlayer();

        if (player != null) {
            RegisterPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSystemDataPacket(CelestialJsonLoader.loadFromDatapacks(resourceManager).solarSystems()));
        } else {
            RegisterPackets.CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncSystemDataPacket(CelestialJsonLoader.loadFromDatapacks(resourceManager).solarSystems()));
        }
    }
}

