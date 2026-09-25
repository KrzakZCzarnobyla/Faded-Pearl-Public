package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.entity.FadedEnderman;

import java.util.function.Supplier;

public record PassengerBlinkPacket() {
    public static void send() {
        ModNetwork.CHANNEL.sendToServer(new PassengerBlinkPacket());
    }

    static void encode(PassengerBlinkPacket packet, FriendlyByteBuf buffer) {}

    static PassengerBlinkPacket decode(FriendlyByteBuf buffer) {
        return new PassengerBlinkPacket();
    }

    static void handle(PassengerBlinkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof FadedEnderman companion)
                companion.requestPassengerBlink(player);
        });
        context.setPacketHandled(true);
    }
}
