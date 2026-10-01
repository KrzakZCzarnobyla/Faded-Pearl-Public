package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelationshipBuildArmorIntegrationTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");
    private static final Path EVENTS = Path.of("src/main/java/pl/fadedpearl/event/CommonEvents.java");

    @Test
    void completedBuildUsesRelationshipMemoryAndKeepsItsFirstTrustReward() throws IOException {
        String source = Files.readString(ENTITY);
        int methodStart = source.indexOf("public void observeCompletedBuild(Player builder)");
        int methodEnd = source.indexOf("private boolean observeAwareness(", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("!worldAwarenessMemory.hasSeen("));
        assertTrue(method.contains("FadedRelationshipDialoguePolicy.Context.BUILD"));
        assertTrue(method.contains("if (firstCompletedBuild && reacted) addTrust(1)"));
        assertFalse(method.contains("FadedDialogue.Category.AWARE_BUILD"));
    }

    @Test
    void armorUsesSessionHighWaterAndTheRelationshipPolicy() throws IOException {
        String source = Files.readString(ENTITY);

        assertTrue(source.contains("FadedArmorProgressPolicy.isUpgrade(friendArmorHighWater, armor)"));
        assertTrue(source.contains("FadedRelationshipDialoguePolicy.Context.ARMOR_UPGRADE"));
        assertTrue(source.contains("friendArmorHighWater = FadedArmorProgressPolicy.nextMaximum("));
        assertFalse(source.contains("friendArmorValue"));
        assertFalse(source.contains("FadedDialogue.Category.AWARE_ARMOR_UPGRADE"));
    }

    @Test
    void qualifyingBuildThresholdAndObservationWindowStayUnchanged() throws IOException {
        String source = Files.readString(EVENTS);

        assertTrue(source.contains("if (idle < 300L) continue"));
        assertTrue(source.contains("observation.blocks >= 20"));
        assertTrue(source.contains("player.getBoundingBox().inflate(24.0D)"));
        assertTrue(source.contains("companion.observeCompletedBuild(player)"));
    }
}
