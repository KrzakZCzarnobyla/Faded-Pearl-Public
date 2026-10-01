package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedDialogueTest {
    @Test
    void buildsZeroBasedVariantBoundaries() {
        assertEquals("faded_pearl.dialogue.flower.0", FadedDialogue.variantKey("faded_pearl.dialogue.flower.", 0, 4));
        assertEquals("faded_pearl.dialogue.flower.3", FadedDialogue.variantKey("faded_pearl.dialogue.flower.", 3, 4));
    }

    @Test
    void buildsOneBasedVariantBoundaries() {
        assertEquals("dialogue.faded_pearl.world.cautious_1",
                FadedDialogue.variant("dialogue.faded_pearl.world.cautious_", 0, 3, 1,
                        FadedDialogue.Delivery.NORMAL).key());
        assertEquals("dialogue.faded_pearl.world.cautious_3",
                FadedDialogue.variant("dialogue.faded_pearl.world.cautious_", 2, 3, 1,
                        FadedDialogue.Delivery.NORMAL).key());
    }

    @Test
    void rejectsInvalidVariantBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> FadedDialogue.variant("key.", -1, 3, 0,
                FadedDialogue.Delivery.NORMAL));
        assertThrows(IndexOutOfBoundsException.class, () -> FadedDialogue.variant("key.", 3, 3, 0,
                FadedDialogue.Delivery.NORMAL));
        assertThrows(IllegalArgumentException.class, () -> FadedDialogue.variant("key.", 0, 0, 0,
                FadedDialogue.Delivery.NORMAL));
    }

    @Test
    void preservesDeliveryDescriptor() {
        assertFalse(FadedDialogue.normal("normal.key").delivery().overlay());
        assertTrue(FadedDialogue.overlay("overlay.key").delivery().overlay());
        assertEquals("overlay.key", FadedDialogue.overlay("overlay.key").key());
    }

    @Test
    void dynamicNameIsASeparatedLiteralTranslationArgument() {
        String json = Component.Serializer.toJson(FadedDialogue.withName(
                FadedDialogue.normal("dialogue.faded_pearl.name.learned.0"), "%2$s Pearl"));
        assertTrue(json.contains("\"translate\":\"dialogue.faded_pearl.name.learned.0\""));
        assertTrue(json.contains("\"with\":[{\"text\":\"%2$s Pearl\"}]"));
    }

    @Test
    void neverRepeatsPreviousVariantForMultiVariantPools() {
        for (FadedDialogue.Category category : FadedDialogue.Category.values()) {
            FadedDialogue.Memory memory = new FadedDialogue.Memory();
            RandomSource random = RandomSource.create(123456789L + category.ordinal());
            int previous = -1;
            for (int draw = 0; draw < 1000; draw++) {
                memory.next(random, category);
                int selected = memory.lastIndex(category).orElseThrow();
                if (previous >= 0) assertNotEquals(previous, selected, category.name());
                previous = selected;
            }
        }
    }

    @Test
    void singleVariantPoolReturnsOnlyVariantWithoutConsumingRandomness() {
        RandomSource actual = RandomSource.create(987654321L);
        RandomSource control = RandomSource.create(987654321L);

        assertEquals(0, FadedDialogue.chooseVariantIndex(actual, 1, -1));
        assertEquals(0, FadedDialogue.chooseVariantIndex(actual, 1, 0));
        assertEquals(control.nextLong(), actual.nextLong());
    }

    @Test
    void validatesSelectionRanges() {
        RandomSource random = RandomSource.create(1L);
        assertThrows(IllegalArgumentException.class,
                () -> FadedDialogue.chooseVariantIndex(random, 0, -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> FadedDialogue.chooseVariantIndex(random, 3, -2));
        assertThrows(IndexOutOfBoundsException.class,
                () -> FadedDialogue.chooseVariantIndex(random, 3, 3));
        assertThrows(IllegalArgumentException.class,
                () -> FadedDialogue.chooseVariantIndex(null, 3, -1));
    }

    @Test
    void categoryMemoriesAreIndependent() {
        FadedDialogue.Memory memory = new FadedDialogue.Memory();
        RandomSource random = RandomSource.create(42L);

        memory.next(random, FadedDialogue.Category.HEALED);
        int healedFirst = memory.lastIndex(FadedDialogue.Category.HEALED).orElseThrow();
        memory.next(random, FadedDialogue.Category.FLOWER);
        int flowerFirst = memory.lastIndex(FadedDialogue.Category.FLOWER).orElseThrow();
        memory.next(random, FadedDialogue.Category.HEALED);

        assertNotEquals(healedFirst, memory.lastIndex(FadedDialogue.Category.HEALED).orElseThrow());
        assertEquals(flowerFirst, memory.lastIndex(FadedDialogue.Category.FLOWER).orElseThrow());
        assertEquals(2, memory.snapshot().size());
    }

    @Test
    void savesAndLoadsMemoryWhileOldSaveDefaultsToEmpty() {
        FadedDialogue.Memory source = new FadedDialogue.Memory();
        source.next(RandomSource.create(11L), FadedDialogue.Category.HEALED);
        source.next(RandomSource.create(12L), FadedDialogue.Category.FLOWER);
        CompoundTag saved = new CompoundTag();
        source.write(saved);

        FadedDialogue.Memory loaded = new FadedDialogue.Memory();
        loaded.read(saved);
        assertEquals(source.snapshot(), loaded.snapshot());
        int previousHealed = loaded.lastIndex(FadedDialogue.Category.HEALED).orElseThrow();
        loaded.next(RandomSource.create(13L), FadedDialogue.Category.HEALED);
        assertNotEquals(previousHealed, loaded.lastIndex(FadedDialogue.Category.HEALED).orElseThrow());

        loaded.read(new CompoundTag());
        assertEquals(Map.of(), loaded.snapshot());
        CompoundTag emptySave = new CompoundTag();
        loaded.write(emptySave);
        assertFalse(emptySave.contains(FadedDialogue.MEMORY_NBT_KEY));
    }

    @Test
    void sanitizesUnknownAndDamagedMemoryEntries() {
        CompoundTag entries = new CompoundTag();
        entries.putInt(FadedDialogue.Category.FLOWER.id(), 3);
        entries.putInt(FadedDialogue.Category.HEALED.id(), 3);
        entries.putInt(FadedDialogue.Category.CARRY.id(), -1);
        entries.put(FadedDialogue.Category.WEATHER_SNOW.id(), StringTag.valueOf("1"));
        entries.putInt("unknown.category", 1);
        CompoundTag saved = new CompoundTag();
        saved.put(FadedDialogue.MEMORY_NBT_KEY, entries);

        FadedDialogue.Memory memory = new FadedDialogue.Memory();
        memory.read(saved);
        assertEquals(Map.of(FadedDialogue.Category.FLOWER, 3), memory.snapshot());

        saved.put(FadedDialogue.MEMORY_NBT_KEY, StringTag.valueOf("damaged"));
        memory.read(saved);
        assertTrue(memory.snapshot().isEmpty());
    }

    @Test
    void rejectsUnknownWorldReaction() {
        assertThrows(IllegalArgumentException.class, () -> FadedDialogue.worldReaction("unknown"));
    }

    @Test
    void mapsEveryRelationshipSelectionToItsDedicatedDialogueFamily() {
        assertEquals(FadedDialogue.Category.RELATION_KNOWLEDGE_FIRST,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_KNOWLEDGE_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_KNOWLEDGE_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.RELATION_CRAFT_FIRST,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.CRAFT_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_CRAFT_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.CRAFT_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_CRAFT_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.CRAFT_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_VILLAGE,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.VILLAGE_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_VILLAGE_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.VILLAGE_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_VILLAGE_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.VILLAGE_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_PLAYER_DIAMOND,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_PLAYER_DIAMOND_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_PLAYER_DIAMOND_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_ENDERMAN_DIAMOND,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_ENDERMAN_DIAMOND_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_ENDERMAN_DIAMOND_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_TAMED_WOLF,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_WOLF_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_WOLF_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_TAMED_CAT,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_CAT_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_CAT_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_TAMED_PARROT,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_PARROT_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_PARROT_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_TAMED_OTHER,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_OTHER_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_TAMED_OTHER_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_BUILD,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.BUILD_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_BUILD_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.BUILD_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_BUILD_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.BUILD_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.AWARE_ARMOR_UPGRADE,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_ARMOR_UPGRADE_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_ARMOR_UPGRADE_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_REPEAT_BONDED));
        assertEquals(FadedDialogue.Category.RELATION_NIGHT_WATCH_FIRST,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_FIRST));
        assertEquals(FadedDialogue.Category.RELATION_NIGHT_WATCH_REPEAT_LEARNING,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_REPEAT_LEARNING));
        assertEquals(FadedDialogue.Category.RELATION_NIGHT_WATCH_REPEAT_BONDED,
                FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_REPEAT_BONDED));
        assertThrows(IllegalArgumentException.class,
                () -> FadedDialogue.relationshipMemory(FadedRelationshipDialoguePolicy.Selection.NONE));
        assertThrows(IllegalArgumentException.class, () -> FadedDialogue.relationshipMemory(null));
    }
}
