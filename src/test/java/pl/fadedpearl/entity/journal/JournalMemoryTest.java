package pl.fadedpearl.entity.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import pl.fadedpearl.entity.persistence.FadedPersistenceCodec;

import static org.junit.jupiter.api.Assertions.*;

final class JournalMemoryTest {
    @Test
    void roundTripsDiscoveriesAndIsIdempotent() {
        JournalMemory source = new JournalMemory();
        for (JournalMemory.Discovery discovery : JournalMemory.Discovery.values())
            assertTrue(source.discover(discovery));
        assertFalse(source.discover(JournalMemory.Discovery.DAY_MEETING));
        assertFalse(source.discover(null));
        CompoundTag tag = new CompoundTag();
        source.write(tag);

        JournalMemory loaded = new JournalMemory();
        loaded.read(tag);
        assertEquals(Set.of(JournalMemory.Discovery.values()), loaded.snapshot());
        assertEquals(JournalMemory.Discovery.values().length,
                tag.getList(JournalMemory.NBT_KEY, net.minecraft.nbt.Tag.TAG_STRING).size());
    }

    @Test
    void missingWrongAndUnknownDataHaveSafeBoundedFallback() {
        JournalMemory memory = new JournalMemory();
        memory.discover(JournalMemory.Discovery.DAY_HEALING);
        memory.read(new CompoundTag());
        assertTrue(memory.snapshot().isEmpty());

        CompoundTag damaged = new CompoundTag();
        damaged.putString(JournalMemory.NBT_KEY, "broken");
        memory.read(damaged);
        assertTrue(memory.snapshot().isEmpty());

        ListTag values = new ListTag();
        values.add(StringTag.valueOf("UNKNOWN_FROM_FUTURE"));
        for (JournalMemory.Discovery discovery : JournalMemory.Discovery.values()) {
            values.add(StringTag.valueOf(discovery.name()));
            values.add(StringTag.valueOf(discovery.name()));
        }
        values.add(StringTag.valueOf("EXTRA"));
        damaged.put(JournalMemory.NBT_KEY, values);
        memory.read(damaged);
        assertEquals(JournalMemory.Discovery.values().length, memory.snapshot().size());
    }

    @Test
    void emptyMemoryDoesNotCreateRequiredNbt() {
        CompoundTag tag = new CompoundTag();
        new JournalMemory().write(tag);
        assertFalse(tag.contains(JournalMemory.NBT_KEY));
        assertTrue(FadedPersistenceCodec.RECOVERY_KEYS.contains(JournalMemory.NBT_KEY));
    }
}
