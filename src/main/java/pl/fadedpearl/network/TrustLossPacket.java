package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.TrustLossToast;
import pl.fadedpearl.entity.trust.TrustLossNotice;
import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.function.Supplier;

/** Server-authored, bounded loss cue. No trust value is accepted from the client. */
public record TrustLossPacket(int points, TrustLossNotice.Reason reason) {
    static void encode(TrustLossPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.points);
        buffer.writeVarInt(packet.reason.ordinal());
    }

    static TrustLossPacket decode(FriendlyByteBuf buffer) {
        int points = Math.max(0, Math.min(FadedTrustManager.MAX_TRUST, buffer.readVarInt()));
        int ordinal = buffer.readVarInt();
        TrustLossNotice.Reason[] values = TrustLossNotice.Reason.values();
        TrustLossNotice.Reason reason = ordinal >= 0 && ordinal < values.length
                ? values[ordinal] : TrustLossNotice.Reason.OTHER;
        return new TrustLossPacket(points, reason);
    }

    static void handle(TrustLossPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> TrustLossToast.show(packet.points, packet.reason)));
        context.get().setPacketHandled(true);
    }
}
