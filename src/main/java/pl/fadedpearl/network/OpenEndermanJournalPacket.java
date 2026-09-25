package pl.fadedpearl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import pl.fadedpearl.client.EndermanJournalScreen;
import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.journal.EndermanJournalSnapshot;
import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

public record OpenEndermanJournalPacket(int dayMask, int behaviorMask, String knownName,
                                       int healingColor, boolean fadeOriginKnown, int trust) {
    static OpenEndermanJournalPacket from(EndermanJournalSnapshot snapshot) {
        return new OpenEndermanJournalPacket(mask(snapshot.days()), mask(snapshot.behaviors()),
                snapshot.knownName(), snapshot.healingColor(), snapshot.fadeOriginKnown(), snapshot.trust());
    }

    static void encode(OpenEndermanJournalPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.dayMask);
        buffer.writeVarInt(packet.behaviorMask);
        buffer.writeUtf(packet.knownName, NameLearningMemory.MAX_NAME_LENGTH * 4);
        buffer.writeInt(packet.healingColor & 0xFFFFFF);
        buffer.writeBoolean(packet.fadeOriginKnown);
        buffer.writeVarInt(packet.trust);
    }

    static OpenEndermanJournalPacket decode(FriendlyByteBuf buffer) {
        int validDays = (1 << EndermanJournalSnapshot.DayEntry.values().length) - 1;
        int validBehaviors = (1 << EndermanJournalSnapshot.BehaviorEntry.values().length) - 1;
        return new OpenEndermanJournalPacket(buffer.readVarInt() & validDays, buffer.readVarInt() & validBehaviors,
                NameLearningMemory.sanitizeName(buffer.readUtf(NameLearningMemory.MAX_NAME_LENGTH * 4)),
                buffer.readInt() & 0xFFFFFF, buffer.readBoolean(),
                Math.max(FadedTrustManager.MIN_TRUST,
                        Math.min(FadedTrustManager.MAX_TRUST, buffer.readVarInt())));
    }

    static void handle(OpenEndermanJournalPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EndermanJournalScreen.open(packet.snapshot())));
        context.get().setPacketHandled(true);
    }

    private EndermanJournalSnapshot snapshot() {
        return new EndermanJournalSnapshot(entries(EndermanJournalSnapshot.DayEntry.class, dayMask),
                entries(EndermanJournalSnapshot.BehaviorEntry.class, behaviorMask), knownName,
                healingColor, fadeOriginKnown, trust);
    }

    private static int mask(Set<? extends Enum<?>> values) {
        int result = 0;
        for (Enum<?> value : values) result |= 1 << value.ordinal();
        return result;
    }

    private static <E extends Enum<E>> Set<E> entries(Class<E> type, int mask) {
        EnumSet<E> result = EnumSet.noneOf(type);
        for (E value : type.getEnumConstants()) if ((mask & (1 << value.ordinal())) != 0) result.add(value);
        return result;
    }
}
