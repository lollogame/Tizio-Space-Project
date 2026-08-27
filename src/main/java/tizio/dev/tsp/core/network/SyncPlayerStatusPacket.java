package tizio.dev.tsp.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPlayerStatusPacket {

    private final float oxygen;
    private final float temperature;

    public SyncPlayerStatusPacket(float oxygen, float temperature) {
        this.oxygen = oxygen;
        this.temperature = temperature;
    }

    public SyncPlayerStatusPacket(FriendlyByteBuf buf) {
        this.oxygen = buf.readFloat();
        this.temperature = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.oxygen);
        buf.writeFloat(this.temperature);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandler.handlePlayerStatus(this.oxygen, this.temperature)
        ));
        ctx.setPacketHandled(true);
    }
}
