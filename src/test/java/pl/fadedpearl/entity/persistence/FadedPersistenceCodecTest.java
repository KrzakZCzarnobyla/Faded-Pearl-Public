package pl.fadedpearl.entity.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.dialogue.FadedDialogue;
import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;
import pl.fadedpearl.entity.journal.JournalMemory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FadedPersistenceCodecTest {
    @Test
    void writesAndReadsRepresentativeFullStateWithoutLoss() {
        UUID friend = UUID.fromString("12345678-1234-5678-9abc-def012345678");
        CompoundTag flower = new CompoundTag();
        flower.putString("id", "minecraft:poppy");
        flower.putByte("Count", (byte) 1);
        FadedPersistenceCodec.PersistentState source = new FadedPersistenceCodec.PersistentState(
                true, true, 101, 202, Optional.of(friend), 3, true, 17, 61, true, 19, 22.5F, true,
                7, 23, 83, 2, 29, 31, 67, 37, 41, 43, 47, true, 53L, 59, 61, 67L,
                Optional.of(new FadedPersistenceCodec.Home(71L, "minecraft:the_nether")), Optional.of(flower),
                73, 401, "rabbit", 79);
        CompoundTag tag = new CompoundTag();

        FadedPersistenceCodec.write(tag, source);
        AtomicInteger rngCalls = new AtomicInteger();
        FadedPersistenceCodec.DecodedState decoded = FadedPersistenceCodec.read(tag, bound -> {
            rngCalls.incrementAndGet();
            return 0;
        });

        assertAll(
                () -> assertTrue(decoded.pearlGiven()), () -> assertTrue(decoded.healed()),
                () -> assertEquals(202, decoded.healingColor().orElseThrow()),
                () -> assertEquals(friend, decoded.friendId().orElseThrow()),
                () -> assertEquals(3, decoded.command().orElseThrow()), () -> assertTrue(decoded.downed()),
                () -> assertEquals(17, decoded.downedTicks()), () -> assertEquals(61, decoded.cryCooldown()),
                () -> assertTrue(decoded.crying()), () -> assertEquals(19, decoded.cryingTicks()),
                () -> assertEquals(22.5F, decoded.seatedYaw()), () -> assertTrue(decoded.seatedYawSet()),
                () -> assertEquals(7, decoded.socialAction().orElseThrow()),
                () -> assertEquals(23, decoded.socialActionTicks()), () -> assertEquals(83, decoded.socialActionCooldown()),
                () -> assertEquals(2, decoded.carryAction().orElseThrow()), () -> assertEquals(29, decoded.carryActionTicks()),
                () -> assertEquals(31, decoded.carriedBlinkCooldown()), () -> assertEquals(67, decoded.trust()),
                () -> assertEquals(37, decoded.crouchTimer()), () -> assertEquals(41, decoded.stareTimer()),
                () -> assertEquals(43, decoded.jumpCount()), () -> assertEquals(47, decoded.jumpWindowTimer()),
                () -> assertTrue(decoded.rainSheltered()), () -> assertEquals(true, decoded.downedOverride().orElseThrow()),
                () -> assertEquals(53L, decoded.lastInteractionTick()),
                () -> assertEquals(59, decoded.trustInteractionCooldown()), () -> assertEquals(61, decoded.flowerCooldown()),
                () -> assertEquals(67L, decoded.lastFriendSeenTime()), () -> assertEquals(71L, decoded.home().orElseThrow().pos()),
                () -> assertEquals("minecraft:the_nether", decoded.home().orElseThrow().dimension()),
                () -> assertEquals("minecraft:poppy", decoded.carriedFlower().orElseThrow().getString("id")),
                () -> assertEquals(73, decoded.protectorCooldown()), () -> assertEquals(401, decoded.worldReactionCooldown()),
                () -> assertEquals("rabbit", decoded.lastWorldReaction()), () -> assertEquals(79, decoded.recoveryEpoch()),
                () -> assertEquals(101, tag.getInt("HealingColor")), () -> assertEquals(202, tag.getInt("FlowerColor")),
                () -> assertEquals(67, tag.getInt("Trust")), () -> assertEquals(67, tag.getInt("faded_pearl:trust_level")),
                () -> assertEquals(2, tag.getInt("TrustSystemVersion")), () -> assertEquals(0, rngCalls.get()));
        assertTagType(tag, Tag.TAG_BYTE, FadedPersistenceCodec.PEARL_GIVEN, FadedPersistenceCodec.HEALED,
                FadedPersistenceCodec.DOWNED, FadedPersistenceCodec.CRYING, FadedPersistenceCodec.SEATED_YAW_SET,
                FadedPersistenceCodec.RAIN_SHELTERED, FadedPersistenceCodec.IS_DOWNED);
        assertTagType(tag, Tag.TAG_INT, FadedPersistenceCodec.HEALING_COLOR, FadedPersistenceCodec.FLOWER_COLOR,
                FadedPersistenceCodec.COMMAND, FadedPersistenceCodec.DOWNED_TICKS, FadedPersistenceCodec.CRY_COOLDOWN,
                FadedPersistenceCodec.CRYING_TICKS, FadedPersistenceCodec.SOCIAL_ACTION,
                FadedPersistenceCodec.SOCIAL_ACTION_TICKS, FadedPersistenceCodec.SOCIAL_ACTION_COOLDOWN,
                FadedPersistenceCodec.CARRY_ACTION, FadedPersistenceCodec.CARRY_ACTION_TICKS,
                FadedPersistenceCodec.CARRIED_BLINK_COOLDOWN, FadedPersistenceCodec.TRUST,
                FadedPersistenceCodec.TRUST_LEVEL, FadedPersistenceCodec.CROUCH_TIMER, FadedPersistenceCodec.STARE_TIMER,
                FadedPersistenceCodec.JUMP_COUNT, FadedPersistenceCodec.JUMP_WINDOW_TIMER,
                FadedPersistenceCodec.TRUST_INTERACTION_COOLDOWN, FadedPersistenceCodec.FLOWER_COOLDOWN,
                FadedPersistenceCodec.TRUST_SYSTEM_VERSION_KEY, FadedPersistenceCodec.PROTECTOR_COOLDOWN,
                FadedPersistenceCodec.WORLD_REACTION_COOLDOWN, FadedPersistenceCodec.RECOVERY_EPOCH);
        assertTagType(tag, Tag.TAG_LONG, FadedPersistenceCodec.LAST_INTERACTION_TICK,
                FadedPersistenceCodec.LAST_FRIEND_SEEN_TIME, FadedPersistenceCodec.HOME_POS);
        assertTagType(tag, Tag.TAG_FLOAT, FadedPersistenceCodec.SEATED_YAW);
        assertTagType(tag, Tag.TAG_STRING, FadedPersistenceCodec.HOME_DIMENSION,
                FadedPersistenceCodec.LAST_WORLD_REACTION);
        assertTagType(tag, Tag.TAG_INT_ARRAY, FadedPersistenceCodec.FRIEND);
        assertTagType(tag, Tag.TAG_COMPOUND, FadedPersistenceCodec.CARRIED_FLOWER);
    }

    @Test
    void preservesMissingTagFallbacksAndConditionalPresence() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("HealingColor", 12);
        tag.putInt("Trust", 88);
        tag.putInt("TrustSystemVersion", 2);
        tag.putInt("faded_pearl:trust_interaction_cooldown", -3);
        tag.putInt("faded_pearl:flower_cooldown", -4);
        tag.putInt("RecoveryEpoch", -5);
        tag.putLong("HomePos", 1L);
        AtomicInteger calls = new AtomicInteger();

        FadedPersistenceCodec.DecodedState decoded = FadedPersistenceCodec.read(tag, bound -> {
            assertEquals(81, bound);
            calls.incrementAndGet();
            return 7;
        });

        assertAll(
                () -> assertEquals(12, decoded.healingColor().orElseThrow()),
                () -> assertEquals(88, decoded.trust()), () -> assertEquals(47, decoded.cryCooldown()),
                () -> assertEquals(1, calls.get()), () -> assertEquals(80, decoded.socialActionCooldown()),
                () -> assertEquals(400, decoded.worldReactionCooldown()),
                () -> assertEquals(0, decoded.trustInteractionCooldown()), () -> assertEquals(0, decoded.flowerCooldown()),
                () -> assertEquals(0, decoded.recoveryEpoch()), () -> assertTrue(decoded.friendId().isEmpty()),
                () -> assertTrue(decoded.command().isEmpty()), () -> assertTrue(decoded.socialAction().isEmpty()),
                () -> assertTrue(decoded.carryAction().isEmpty()), () -> assertTrue(decoded.home().isEmpty()),
                () -> assertTrue(decoded.carriedFlower().isEmpty()), () -> assertTrue(decoded.downedOverride().isEmpty()));
    }

    @Test
    void prefersNamespacedTrustAndRejectsUnversionedLegacyTrust() {
        CompoundTag namespaced = new CompoundTag();
        namespaced.putInt("Trust", 11);
        namespaced.putInt("faded_pearl:trust_level", 22);
        CompoundTag oldLegacy = new CompoundTag();
        oldLegacy.putInt("Trust", 33);

        assertEquals(22, FadedPersistenceCodec.read(namespaced, bound -> 0).trust());
        assertEquals(0, FadedPersistenceCodec.read(oldLegacy, bound -> 0).trust());
    }

    @Test
    void leavesHealingColorAbsentWhenNeitherColorTagExists() {
        assertTrue(FadedPersistenceCodec.read(new CompoundTag(), bound -> 0).healingColor().isEmpty());
    }

    @Test
    void emptyLegacyTagUsesSafeDefaultsWithoutInventingOptionalState() {
        AtomicInteger calls = new AtomicInteger();
        FadedPersistenceCodec.DecodedState decoded = FadedPersistenceCodec.read(new CompoundTag(), bound -> {
            assertEquals(81, bound);
            calls.incrementAndGet();
            return 0;
        });

        assertAll(
                () -> assertFalse(decoded.pearlGiven()), () -> assertFalse(decoded.healed()),
                () -> assertTrue(decoded.healingColor().isEmpty()), () -> assertTrue(decoded.friendId().isEmpty()),
                () -> assertTrue(decoded.command().isEmpty()), () -> assertFalse(decoded.downed()),
                () -> assertEquals(40, decoded.cryCooldown()), () -> assertEquals(80, decoded.socialActionCooldown()),
                () -> assertTrue(decoded.socialAction().isEmpty()), () -> assertTrue(decoded.carryAction().isEmpty()),
                () -> assertEquals(0, decoded.trust()), () -> assertEquals(0, decoded.trustInteractionCooldown()),
                () -> assertEquals(0, decoded.flowerCooldown()), () -> assertTrue(decoded.home().isEmpty()),
                () -> assertTrue(decoded.carriedFlower().isEmpty()), () -> assertEquals(400, decoded.worldReactionCooldown()),
                () -> assertEquals(0, decoded.recoveryEpoch()), () -> assertEquals(1, calls.get()));
    }

    @Test
    void writesOptionalValuesOnlyWhenPresentAndHomeOnlyAsPair() {
        FadedPersistenceCodec.PersistentState state = new FadedPersistenceCodec.PersistentState(
                false, false, 0, 0, Optional.empty(), 0, false, 0, 0, false, 0, 0F, false,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0L, 0, 0, 0L,
                Optional.empty(), Optional.empty(), 0, 0, "", 0);
        CompoundTag tag = new CompoundTag();

        FadedPersistenceCodec.write(tag, state);

        assertAll(() -> assertFalse(tag.contains("Friend")), () -> assertFalse(tag.contains("HomePos")),
                () -> assertFalse(tag.contains("HomeDimension")), () -> assertFalse(tag.contains("CarriedFlower")));
    }

    @Test
    void recoveryCopyUsesExactAllowlistAndCopiesValues() {
        CompoundTag raw = new CompoundTag();
        for (int index = 0; index < FadedPersistenceCodec.RECOVERY_KEYS.size(); index++)
            raw.put(FadedPersistenceCodec.RECOVERY_KEYS.get(index), IntTag.valueOf(index));
        raw.put("PearlGiven", new CompoundTag());
        raw.putInt("Health", 99);
        raw.putInt("Unknown", 100);

        CompoundTag safe = FadedPersistenceCodec.copyRecoveryKeys(raw);

        assertEquals(52, FadedPersistenceCodec.RECOVERY_KEYS.size());
        assertEquals(FadedPersistenceCodec.RECOVERY_KEYS.size(), safe.getAllKeys().size());
        assertTrue(safe.getAllKeys().containsAll(FadedPersistenceCodec.RECOVERY_KEYS));
        assertFalse(safe.contains("Health"));
        assertFalse(safe.contains("Unknown"));
        assertNotSame(raw.get("PearlGiven"), safe.get("PearlGiven"));
    }

    @Test
    void recoveryAllowlistHasEveryCurrentEntityFieldExactlyOnce() {
        Set<String> expected = Set.of(
                FadedPersistenceCodec.PEARL_GIVEN, FadedPersistenceCodec.HEALED,
                FadedPersistenceCodec.HEALING_COLOR, FadedPersistenceCodec.FLOWER_COLOR,
                FadedPersistenceCodec.FRIEND, FadedPersistenceCodec.COMMAND, FadedPersistenceCodec.DOWNED,
                FadedPersistenceCodec.DOWNED_TICKS, FadedPersistenceCodec.CRY_COOLDOWN,
                FadedPersistenceCodec.CRYING, FadedPersistenceCodec.CRYING_TICKS,
                FadedPersistenceCodec.SEATED_YAW, FadedPersistenceCodec.SEATED_YAW_SET,
                FadedPersistenceCodec.SOCIAL_ACTION, FadedPersistenceCodec.SOCIAL_ACTION_TICKS,
                FadedPersistenceCodec.SOCIAL_ACTION_COOLDOWN, FadedPersistenceCodec.CARRY_ACTION,
                FadedPersistenceCodec.CARRY_ACTION_TICKS, FadedPersistenceCodec.CARRIED_BLINK_COOLDOWN,
                FadedPersistenceCodec.TRUST, FadedPersistenceCodec.TRUST_LEVEL,
                FadedPersistenceCodec.CROUCH_TIMER, FadedPersistenceCodec.STARE_TIMER,
                FadedPersistenceCodec.JUMP_COUNT, FadedPersistenceCodec.JUMP_WINDOW_TIMER,
                FadedPersistenceCodec.RAIN_SHELTERED, FadedPersistenceCodec.IS_DOWNED,
                FadedPersistenceCodec.LAST_INTERACTION_TICK,
                FadedPersistenceCodec.TRUST_INTERACTION_COOLDOWN, FadedPersistenceCodec.FLOWER_COOLDOWN,
                FadedPersistenceCodec.TRUST_SYSTEM_VERSION_KEY, FadedPersistenceCodec.LAST_FRIEND_SEEN_TIME,
                FadedPersistenceCodec.HOME_POS, FadedPersistenceCodec.HOME_DIMENSION,
                FadedPersistenceCodec.CARRIED_FLOWER, FadedPersistenceCodec.PROTECTOR_COOLDOWN,
                FadedPersistenceCodec.WORLD_REACTION_COOLDOWN, FadedPersistenceCodec.LAST_WORLD_REACTION,
                FadedPersistenceCodec.RECOVERY_EPOCH, FadedPersistenceCodec.CURIOSITY_STACK,
                FadedPersistenceCodec.CURIOSITY_SEEN, FadedPersistenceCodec.CURIOSITY_COOLDOWN,
                FadedPersistenceCodec.CURIOSITY_RETURN_POS,
                FadedPersistenceCodec.CURIOSITY_RETURN_DIMENSION,
                FadedPersistenceCodec.ANIMAL_CARRY_COOLDOWN, FadedPersistenceCodec.ANIMAL_SAME_COOLDOWN,
                FadedPersistenceCodec.ANIMAL_LAST_ID, FadedPersistenceCodec.ESCAPE_PEARL_ARMED,
                FadedDialogue.MEMORY_NBT_KEY, WorldAwarenessMemory.NBT_KEY,
                NameLearningMemory.NBT_KEY, JournalMemory.NBT_KEY);

        assertEquals(expected, Set.copyOf(FadedPersistenceCodec.RECOVERY_KEYS));
        assertEquals(expected.size(), FadedPersistenceCodec.RECOVERY_KEYS.size(),
                "Recovery allowlist must not contain duplicate keys");
    }

    @Test
    void recoveryCopyPreservesEscapePearlAndRelationshipMemoriesExactly() {
        CompoundTag raw = new CompoundTag();
        raw.putBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED, true);

        JournalMemory journal = new JournalMemory();
        for (JournalMemory.Discovery discovery : JournalMemory.Discovery.values()) journal.discover(discovery);
        journal.write(raw);

        WorldAwarenessMemory awareness = new WorldAwarenessMemory();
        for (WorldAwarenessMemory.Milestone milestone : WorldAwarenessMemory.Milestone.values())
            awareness.markFirst(milestone);
        awareness.write(raw);

        NameLearningMemory names = new NameLearningMemory();
        names.rememberOwnName("Lumen", bound -> 17);
        names.rememberPet(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), "Mochi");
        names.write(raw);

        ListTag seen = new ListTag();
        seen.add(StringTag.valueOf("minecraft:diamond_sword"));
        raw.put(FadedPersistenceCodec.CURIOSITY_SEEN, seen);
        raw.putInt(FadedPersistenceCodec.CURIOSITY_COOLDOWN, 3600);
        raw.putInt(FadedPersistenceCodec.ANIMAL_CARRY_COOLDOWN, 1200);
        raw.putInt(FadedPersistenceCodec.ANIMAL_SAME_COOLDOWN, 7200);
        raw.putUUID(FadedPersistenceCodec.ANIMAL_LAST_ID,
                UUID.fromString("11111111-2222-3333-4444-555555555555"));

        CompoundTag safe = FadedPersistenceCodec.copyRecoveryKeys(raw);

        for (String key : raw.getAllKeys()) assertEquals(raw.get(key), safe.get(key), key);
        assertTrue(safe.getBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED));
        assertNotSame(raw.get(JournalMemory.NBT_KEY), safe.get(JournalMemory.NBT_KEY));
        assertNotSame(raw.get(WorldAwarenessMemory.NBT_KEY), safe.get(WorldAwarenessMemory.NBT_KEY));
        assertNotSame(raw.get(NameLearningMemory.NBT_KEY), safe.get(NameLearningMemory.NBT_KEY));
    }

    @Test
    void unknownTopLevelFieldsNeverEnterRecoverySnapshot() {
        CompoundTag raw = new CompoundTag();
        raw.putString("FutureEntityField", "must stay outside the recovery contract");
        raw.putInt("EscapePearlProtected", 1);
        raw.putBoolean(FadedPersistenceCodec.ESCAPE_PEARL_ARMED, true);

        CompoundTag safe = FadedPersistenceCodec.copyRecoveryKeys(raw);

        assertEquals(Set.of(FadedPersistenceCodec.ESCAPE_PEARL_ARMED), safe.getAllKeys());
    }

    @Test
    void curiosityStackAndIdsAreCopiedExactlyIntoRecoverySnapshot() {
        CompoundTag raw = new CompoundTag();
        CompoundTag exactStack = new CompoundTag();
        exactStack.putString("id", "minecraft:diamond_sword");
        exactStack.putByte("Count", (byte) 1);
        CompoundTag exactTag = new CompoundTag();
        exactTag.putString("display", "custom");
        exactStack.put("tag", exactTag);
        ListTag ids = new ListTag();
        ids.add(StringTag.valueOf("minecraft:diamond_sword"));
        raw.put(FadedPersistenceCodec.CURIOSITY_STACK, exactStack);
        raw.put(FadedPersistenceCodec.CURIOSITY_SEEN, ids);

        CompoundTag safe = FadedPersistenceCodec.copyRecoveryKeys(raw);

        assertEquals(exactStack, safe.getCompound(FadedPersistenceCodec.CURIOSITY_STACK));
        assertEquals(ids, safe.getList(FadedPersistenceCodec.CURIOSITY_SEEN, Tag.TAG_STRING));
        assertNotSame(exactStack, safe.get(FadedPersistenceCodec.CURIOSITY_STACK));
    }

    private static void assertTagType(CompoundTag tag, int expectedType, String... keys) {
        for (String key : keys) assertEquals(expectedType, tag.getTagType(key), key);
    }
}
