package pl.fadedpearl.entity.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class WorldAwarenessMemoryTest {
    @Test
    void milestoneIsGrantedOnlyOnceAndSurvivesSave() {
        WorldAwarenessMemory source = new WorldAwarenessMemory();
        for (WorldAwarenessMemory.Milestone milestone : WorldAwarenessMemory.Milestone.values())
            assertTrue(source.markFirst(milestone));
        assertFalse(source.markFirst(WorldAwarenessMemory.Milestone.VILLAGE));
        CompoundTag tag = new CompoundTag();
        source.write(tag);
        WorldAwarenessMemory loaded = new WorldAwarenessMemory();
        loaded.read(tag);
        assertEquals(Set.of(WorldAwarenessMemory.Milestone.values()), loaded.snapshot());
        assertEquals(WorldAwarenessMemory.Milestone.values().length,
                tag.getList(WorldAwarenessMemory.NBT_KEY, net.minecraft.nbt.Tag.TAG_STRING).size());
    }

    @Test
    void unknownAndDamagedValuesAreIgnored() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("PLAYER_DIAMOND"));
        list.add(StringTag.valueOf("PLAYER_DIAMOND"));
        list.add(StringTag.valueOf("FUTURE_VALUE"));
        tag.put(WorldAwarenessMemory.NBT_KEY, list);
        WorldAwarenessMemory memory = new WorldAwarenessMemory();
        memory.read(tag);
        assertEquals(Set.of(WorldAwarenessMemory.Milestone.PLAYER_DIAMOND), memory.snapshot());
        tag.putString(WorldAwarenessMemory.NBT_KEY, "damaged");
        memory.read(tag);
        assertTrue(memory.snapshot().isEmpty());
    }

    @Test
    void oldSaveWithoutAwarenessDoesNotRetainPreviouslyLoadedMilestones() {
        WorldAwarenessMemory memory = new WorldAwarenessMemory();
        memory.markFirst(WorldAwarenessMemory.Milestone.BUILD_COMPLETED);

        memory.read(new CompoundTag());

        assertTrue(memory.snapshot().isEmpty());
    }
}
