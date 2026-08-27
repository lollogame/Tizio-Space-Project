package tizio.dev.tsp.core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import tizio.dev.tsp.MainClass;

public final class PacketsRegistry {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MainClass.MODID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SyncSystemDataPacket.class, SyncSystemDataPacket::encode, SyncSystemDataPacket::new, SyncSystemDataPacket::handle);
        CHANNEL.registerMessage(id++, SyncPlayerStatusPacket.class, SyncPlayerStatusPacket::encode, SyncPlayerStatusPacket::new, SyncPlayerStatusPacket::handle);
    }
}

