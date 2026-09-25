package pl.fadedpearl.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.journal.EndermanJournalSnapshot;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class OpenEndermanJournalPacketTest {
    @Test
    void roundTripCarriesOnlyBoundedPresentationData() {
        EndermanJournalSnapshot snapshot = new EndermanJournalSnapshot(
                Set.of(EndermanJournalSnapshot.DayEntry.MEETING,
                        EndermanJournalSnapshot.DayEntry.FIRST_BUILD),
                Set.of(EndermanJournalSnapshot.BehaviorEntry.CURIOSITY,
                        EndermanJournalSnapshot.BehaviorEntry.ANIMAL_CARRY,
                        EndermanJournalSnapshot.BehaviorEntry.ESCAPE_PEARL), "Lumen", 0xE33A4E, true, 75);
        OpenEndermanJournalPacket source = OpenEndermanJournalPacket.from(snapshot);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        OpenEndermanJournalPacket.encode(source, buffer);

        OpenEndermanJournalPacket decoded = OpenEndermanJournalPacket.decode(buffer);
        assertAll(() -> assertEquals(source.dayMask(), decoded.dayMask()),
                () -> assertEquals(source.behaviorMask(), decoded.behaviorMask()),
                () -> assertTrue((decoded.behaviorMask()
                        & (1 << EndermanJournalSnapshot.BehaviorEntry.ANIMAL_CARRY.ordinal())) != 0),
                () -> assertTrue((decoded.behaviorMask()
                        & (1 << EndermanJournalSnapshot.BehaviorEntry.ESCAPE_PEARL.ordinal())) != 0),
                () -> assertEquals("Lumen", decoded.knownName()),
                () -> assertEquals(0xE33A4E, decoded.healingColor()),
                () -> assertTrue(decoded.fadeOriginKnown()));
        assertEquals(75, decoded.trust());
    }

    @Test
    void decoderRejectsUnknownBitsAndBoundsDynamicValues() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(-1);
        buffer.writeVarInt(-1);
        buffer.writeUtf("x".repeat(80), 256);
        buffer.writeInt(-1);
        buffer.writeBoolean(false);
        buffer.writeVarInt(200);

        OpenEndermanJournalPacket decoded = OpenEndermanJournalPacket.decode(buffer);
        assertAll(
                () -> assertEquals((1 << EndermanJournalSnapshot.DayEntry.values().length) - 1, decoded.dayMask()),
                () -> assertEquals((1 << EndermanJournalSnapshot.BehaviorEntry.values().length) - 1,
                        decoded.behaviorMask()),
                () -> assertEquals(64, decoded.knownName().length()),
                () -> assertEquals(0xFFFFFF, decoded.healingColor()),
                () -> assertFalse(decoded.fadeOriginKnown()));
        assertEquals(100, decoded.trust());
    }
}
