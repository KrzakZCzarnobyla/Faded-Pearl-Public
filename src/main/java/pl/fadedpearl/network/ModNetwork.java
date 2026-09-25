package pl.fadedpearl.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pl.fadedpearl.FadedPearl;

public final class ModNetwork {
    private static final String VERSION = "5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(FadedPearl.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();
    private static int id;

    public static void register() {
        CHANNEL.registerMessage(id++, OpenCommandMenuPacket.class, OpenCommandMenuPacket::encode,
                OpenCommandMenuPacket::decode, OpenCommandMenuPacket::handle);
        CHANNEL.registerMessage(id++, SetCompanionCommandPacket.class, SetCompanionCommandPacket::encode,
                SetCompanionCommandPacket::decode, SetCompanionCommandPacket::handle);
        CHANNEL.registerMessage(id++, PassengerBlinkPacket.class, PassengerBlinkPacket::encode,
                PassengerBlinkPacket::decode, PassengerBlinkPacket::handle);
        CHANNEL.registerMessage(id++, OpenEndermanJournalPacket.class, OpenEndermanJournalPacket::encode,
                OpenEndermanJournalPacket::decode, OpenEndermanJournalPacket::handle);
        CHANNEL.registerMessage(id++, JournalDiscoveryPacket.class, JournalDiscoveryPacket::encode,
                JournalDiscoveryPacket::decode, JournalDiscoveryPacket::handle);
        CHANNEL.registerMessage(id++, TrustLossPacket.class, TrustLossPacket::encode,
                TrustLossPacket::decode, TrustLossPacket::handle);
    }

    public static void openCommandMenu(ServerPlayer player, int entityId, int activeCommand) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCommandMenuPacket(entityId, activeCommand));
    }

    public static void openEndermanJournal(ServerPlayer player,
                                            pl.fadedpearl.entity.journal.EndermanJournalSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenEndermanJournalPacket.from(snapshot));
    }

    public static void notifyJournalDiscovery(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new JournalDiscoveryPacket());
    }

    public static void notifyTrustLoss(ServerPlayer player, int points,
                                       pl.fadedpearl.entity.trust.TrustLossNotice.Reason reason) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TrustLossPacket(points, reason));
    }

    private ModNetwork() {}
}
