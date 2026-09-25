package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.EndermanJournalToast;

import java.util.function.Supplier;

/** Payload-free cue: the server alone decides whether a real entry was unlocked. */
public record JournalDiscoveryPacket() {
    static void encode(JournalDiscoveryPacket packet, FriendlyByteBuf buffer) {}

    static JournalDiscoveryPacket decode(FriendlyByteBuf buffer) {
        return new JournalDiscoveryPacket();
    }

    static void handle(JournalDiscoveryPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> EndermanJournalToast::show));
        context.get().setPacketHandled(true);
    }
}
