package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelationshipAnimalIntegrationTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void tamingUsesRelationshipPolicyAndKeepsTheSingleGlobalTrustReward() throws IOException {
        String source = Files.readString(ENTITY);

        assertTrue(source.contains("FadedRelationshipDialoguePolicy.Context.TAMED_WOLF"));
        assertTrue(source.contains("FadedRelationshipDialoguePolicy.Context.TAMED_CAT"));
        assertTrue(source.contains("FadedRelationshipDialoguePolicy.Context.TAMED_PARROT"));
        assertTrue(source.contains("FadedRelationshipDialoguePolicy.Context.TAMED_OTHER"));
        assertEquals(1, occurrences(source,
                "markFirst(WorldAwarenessMemory.Milestone.TAMED_ANY)"));
        assertEquals(1, occurrences(source, "if (firstTamedAnimal) addTrust(1)"));
    }

    @Test
    void oneVisiblePetScanPrioritizesNewNamesBeforeCalmRepeats() throws IOException {
        String source = Files.readString(ENTITY);

        assertEquals(1, occurrences(source, "getEntitiesOfClass(TamableAnimal.class"));
        int newName = source.indexOf("nameLearningMemory.isNewPetName(");
        int repeatedName = source.indexOf("nameLearningMemory.canRepeatPetName(");
        assertTrue(newName >= 0 && repeatedName > newName);
        assertTrue(source.contains("nameLearningMemory.tickPetRepeatCooldown()"));
        assertTrue(source.contains("nameLearningMemory.ensurePetRepeatCooldown(random::nextInt)"));
        assertTrue(source.contains("FadedDialogue.Category.NAMED_PET_REPEAT_LEARNING"));
        assertTrue(source.contains("FadedDialogue.Category.NAMED_PET_REPEAT_BONDED"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
