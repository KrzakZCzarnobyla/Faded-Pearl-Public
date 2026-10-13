package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.EchoNeedleHud;

import java.util.function.Supplier;

/** Payload-free, server-gated first-use hint. */
public record EchoNeedleHintPacket() {
    static void encode(EchoNeedleHintPacket packet, FriendlyByteBuf buffer) {}

    static EchoNeedleHintPacket decode(FriendlyByteBuf buffer) {
        return new EchoNeedleHintPacket();
    }

    static void handle(EchoNeedleHintPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> EchoNeedleHud::showFirstHint));
        context.get().setPacketHandled(true);
    }
}
