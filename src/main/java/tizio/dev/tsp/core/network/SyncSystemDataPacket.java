package tizio.dev.tsp.core.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import tizio.dev.tsp.core.data.CelestialJsonLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncSystemDataPacket {

    private final List<CelestialJsonLoader.LoadedJson> solarSystems;

    public SyncSystemDataPacket(List<CelestialJsonLoader.LoadedJson> solarSystems) {
        this.solarSystems = solarSystems;
    }

    public SyncSystemDataPacket(FriendlyByteBuf buf) {
        this.solarSystems = readList(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        writeList(buf, solarSystems);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> CelestialJsonLoader.applyDatapackData(
                new CelestialJsonLoader.LoadedData(solarSystems, List.of())
        ));
        ctx.setPacketHandled(true);
    }

    private static void writeList(FriendlyByteBuf buf, List<CelestialJsonLoader.LoadedJson> list) {
        buf.writeVarInt(list.size());
        for (CelestialJsonLoader.LoadedJson entry : list) {
            buf.writeResourceLocation(entry.id());
            buf.writeUtf(entry.root().toString(), Short.MAX_VALUE);
        }
    }

    private static List<CelestialJsonLoader.LoadedJson> readList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();

        List<CelestialJsonLoader.LoadedJson> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            JsonObject root = JsonParser.parseString(buf.readUtf(Short.MAX_VALUE)).getAsJsonObject();
            result.add(new CelestialJsonLoader.LoadedJson(id, root));
        }
        return result;
    }

}
