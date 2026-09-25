package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.CommandWheelScreen;

import java.util.function.Supplier;

public record OpenCommandMenuPacket(int entityId, int activeCommand) {
    static void encode(OpenCommandMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId); buffer.writeVarInt(packet.activeCommand);
    }
    static OpenCommandMenuPacket decode(FriendlyByteBuf buffer) {
        return new OpenCommandMenuPacket(buffer.readVarInt(), buffer.readVarInt());
    }
    static void handle(OpenCommandMenuPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CommandWheelScreen.open(packet.entityId, packet.activeCommand)));
        context.get().setPacketHandled(true);
    }
}
