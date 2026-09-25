package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.entity.FadedEnderman;

import java.util.function.Supplier;

public record SetCompanionCommandPacket(int entityId, int command) {
    public static void send(int entityId, int command) {
        ModNetwork.CHANNEL.sendToServer(new SetCompanionCommandPacket(entityId, command));
    }
    static void encode(SetCompanionCommandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId); buffer.writeVarInt(packet.command);
    }
    static SetCompanionCommandPacket decode(FriendlyByteBuf buffer) {
        return new SetCompanionCommandPacket(buffer.readVarInt(), buffer.readVarInt());
    }
    static void handle(SetCompanionCommandPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(packet.entityId);
            if (!(entity instanceof FadedEnderman companion) || !companion.isFriend(player)
                    || player.distanceToSqr(companion) > 64.0D) return;
            FadedEnderman.CompanionCommand[] commands = FadedEnderman.CompanionCommand.values();
            if (packet.command < 0 || packet.command >= commands.length) return;
            FadedEnderman.CompanionCommand command = commands[packet.command];
            if (command == FadedEnderman.CompanionCommand.HOME && !companion.hasHome()) {
                player.sendSystemMessage(Component.translatable("message.faded_pearl.anchor.no_home"), true);
                return;
            }
            companion.requestCommandTransitionStop("command packet");
            companion.setCommand(command);
            companion.respondToCommand(player, command);
        });
        context.get().setPacketHandled(true);
    }
}
