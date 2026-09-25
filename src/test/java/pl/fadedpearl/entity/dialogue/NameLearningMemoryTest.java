package pl.fadedpearl.entity.dialogue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class NameLearningMemoryTest {
    @Test
    void ownNameIsLearnedOnceAndChangedNameCreatesOneNewEvent() {
        NameLearningMemory memory = new NameLearningMemory();
        assertTrue(memory.isNewOwnName("Pearl"));
        memory.rememberOwnName("Pearl", bound -> 0);
        assertFalse(memory.isNewOwnName("Pearl"));
        assertTrue(memory.isNewOwnName("Echo"));
        memory.rememberOwnName("Echo", bound -> bound - 1);
        assertFalse(memory.isNewOwnName("Echo"));
        assertEquals(NameLearningMemory.MAX_REPEAT_COOLDOWN, memory.repeatCooldown());
    }

    @Test
    void ownNameRepeatWaitsForCooldownAndAbsenceAllowsRelearning() {
        NameLearningMemory memory = new NameLearningMemory();
        memory.rememberOwnName("Pearl", bound -> 0);
        assertFalse(memory.canRepeatOwnName("Pearl"));
        for (int tick = 0; tick < NameLearningMemory.MIN_REPEAT_COOLDOWN; tick++)
            memory.tickRepeatCooldown();
        assertTrue(memory.canRepeatOwnName("Pearl"));
        assertFalse(memory.canRepeatOwnName("Other"));
        memory.forgetOwnNameIfAbsent("");
        assertTrue(memory.isNewOwnName("Pearl"));
    }

    @Test
    void qualificationRejectsEveryBlockedRuntimeCondition() {
        assertTrue(NameLearningMemory.canObserve(true, false, true, true, true, false, false));
        assertFalse(NameLearningMemory.canObserve(false, false, true, true, true, false, false));
        assertFalse(NameLearningMemory.canObserve(true, true, true, true, true, false, false));
        assertFalse(NameLearningMemory.canObserve(true, false, false, true, true, false, false));
        assertFalse(NameLearningMemory.canObserve(true, false, true, false, true, false, false));
        assertFalse(NameLearningMemory.canObserve(true, false, true, true, false, false, false));
        assertFalse(NameLearningMemory.canObserve(true, false, true, true, true, true, false));
        assertFalse(NameLearningMemory.canObserve(true, false, true, true, true, false, true));
        assertTrue(NameLearningMemory.isCalmForRepeat(true, false, false, false));
        assertFalse(NameLearningMemory.isCalmForRepeat(false, false, false, false));
        assertFalse(NameLearningMemory.isCalmForRepeat(true, true, false, false));
        assertFalse(NameLearningMemory.isCalmForRepeat(true, false, true, false));
        assertFalse(NameLearningMemory.isCalmForRepeat(true, false, false, true));
    }

    @Test
    void namedPetMemoryAllowsRenameAndEvictsOldestAtThirtyTwo() {
        NameLearningMemory memory = new NameLearningMemory();
        UUID first = new UUID(0L, 1L);
        memory.rememberPet(first, "First");
        assertFalse(memory.isNewPetName(first, "First"));
        assertTrue(memory.isNewPetName(first, "Renamed"));
        memory.rememberPet(first, "Renamed");
        for (int index = 2; index <= 33; index++)
            memory.rememberPet(new UUID(0L, index), "Pet " + index);
        assertEquals(NameLearningMemory.MAX_PETS, memory.petSnapshot().size());
        assertFalse(memory.petSnapshot().containsKey(first));
        assertTrue(memory.petSnapshot().containsKey(new UUID(0L, 33L)));
    }

    @Test
    void saveRoundTripIsBoundedAndOldOrDamagedNbtFallsBackToEmpty() {
        NameLearningMemory source = new NameLearningMemory();
        source.rememberOwnName("Pearl", bound -> 17);
        UUID pet = UUID.randomUUID();
        source.rememberPet(pet, "Mochi");
        CompoundTag saved = new CompoundTag();
        source.write(saved);

        NameLearningMemory loaded = new NameLearningMemory();
        loaded.read(saved);
        assertEquals(source.ownName(), loaded.ownName());
        assertEquals(source.repeatCooldown(), loaded.repeatCooldown());
        assertEquals(source.petSnapshot(), loaded.petSnapshot());

        loaded.read(new CompoundTag());
        assertTrue(loaded.ownName().isEmpty());
        assertTrue(loaded.petSnapshot().isEmpty());

        CompoundTag damaged = new CompoundTag();
        damaged.put(NameLearningMemory.NBT_KEY, StringTag.valueOf("broken"));
        loaded.read(damaged);
        assertTrue(loaded.ownName().isEmpty());
        assertTrue(loaded.petSnapshot().isEmpty());
    }

    @Test
    void damagedEntriesAndOversizedNamesAreSanitized() {
        String oversized = "x".repeat(70) + "😀";
        CompoundTag memoryTag = new CompoundTag();
        memoryTag.putString("OwnName", oversized);
        memoryTag.putInt("RepeatCooldown", -20);
        ListTag pets = new ListTag();
        for (int index = 0; index < 40; index++) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", new UUID(1L, index));
            entry.putString("Name", oversized);
            pets.add(entry);
        }
        memoryTag.put("Pets", pets);
        CompoundTag parent = new CompoundTag();
        parent.put(NameLearningMemory.NBT_KEY, memoryTag);

        NameLearningMemory loaded = new NameLearningMemory();
        loaded.read(parent);
        assertEquals(64, loaded.ownName().orElseThrow().codePointCount(0, loaded.ownName().orElseThrow().length()));
        assertEquals(0, loaded.repeatCooldown());
        assertEquals(NameLearningMemory.MAX_PETS, loaded.petSnapshot().size());
        assertTrue(loaded.petSnapshot().values().stream().allMatch(name -> name.length() == 64));
    }

    @Test
    void malformedPetEntriesAndDuplicateIdsCannotExceedTheBound() {
        UUID duplicate = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CompoundTag memoryTag = new CompoundTag();
        ListTag pets = new ListTag();
        CompoundTag missingId = new CompoundTag();
        missingId.putString("Name", "Missing id");
        pets.add(missingId);
        CompoundTag wrongNameType = new CompoundTag();
        wrongNameType.putUUID("Id", UUID.randomUUID());
        wrongNameType.putInt("Name", 4);
        pets.add(wrongNameType);
        for (int index = 0; index < 40; index++) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", index < 2 ? duplicate : new UUID(4L, index));
            entry.putString("Name", "Pet " + index);
            pets.add(entry);
        }
        memoryTag.put("Pets", pets);
        CompoundTag parent = new CompoundTag();
        parent.put(NameLearningMemory.NBT_KEY, memoryTag);

        NameLearningMemory loaded = new NameLearningMemory();
        loaded.read(parent);

        assertEquals(NameLearningMemory.MAX_PETS, loaded.petSnapshot().size());
        assertEquals("Pet 1", loaded.petSnapshot().get(duplicate));
        assertFalse(loaded.petSnapshot().containsValue("Missing id"));
    }

    @Test
    void emptyMemoryRemovesPreviouslyWrittenTagAndOldSaveLoadsEmpty() {
        CompoundTag parent = new CompoundTag();
        parent.putString(NameLearningMemory.NBT_KEY, "stale");
        new NameLearningMemory().write(parent);
        assertFalse(parent.contains(NameLearningMemory.NBT_KEY));

        NameLearningMemory loaded = new NameLearningMemory();
        loaded.rememberOwnName("Old", bound -> 0);
        loaded.rememberPet(UUID.randomUUID(), "Old pet");
        loaded.read(new CompoundTag());
        assertTrue(loaded.ownName().isEmpty());
        assertEquals(0, loaded.repeatCooldown());
        assertTrue(loaded.petSnapshot().isEmpty());
    }
}
