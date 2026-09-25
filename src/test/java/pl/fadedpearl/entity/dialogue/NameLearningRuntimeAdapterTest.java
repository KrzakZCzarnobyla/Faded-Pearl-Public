package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class NameLearningRuntimeAdapterTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void serverAdapterCoversOwnershipRangeVisibilityPriorityLookAndPersistence() throws IOException {
        String source = Files.readString(ENTITY);
        assertTrue(source.contains("private void tickNameLearning()"));
        assertTrue(source.contains("animal instanceof") || source.contains("getEntitiesOfClass(TamableAnimal.class"));
        assertTrue(source.contains("serverFriend.getUUID().equals(animal.getOwnerUUID())"));
        assertTrue(source.contains("distanceToSqr(animal) <= 144.0D"));
        assertTrue(source.contains("hasLineOfSight(animal)"));
        assertTrue(source.contains("curiosityPhase != CuriosityPhase.NONE"));
        assertTrue(source.contains("getActiveHostileReactionTarget() != null"));
        assertTrue(source.contains("|| getCommand() == CompanionCommand.HOME;"));
        assertTrue(source.contains("queueLookAt(lookTarget"));
        assertTrue(source.contains("setSocialAction(SocialAction.AFFECTION)"));
        assertEquals(1, occurrences(source, "nameLearningMemory.write(tag)"));
        assertEquals(1, occurrences(source, "nameLearningMemory.read(tag)"));
    }

    @Test
    void dynamicNameUsesLiteralTranslationArgumentAndNeverRawConcatenation() throws IOException {
        String dialogue = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/dialogue/FadedDialogue.java"));
        assertTrue(dialogue.contains("Component.literal(NameLearningMemory.sanitizeName(visibleName))"));
        assertTrue(dialogue.contains("Component.translatable(dialogue.key(), safeName)"));
        assertFalse(dialogue.contains("dialogue.key() + visibleName"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
