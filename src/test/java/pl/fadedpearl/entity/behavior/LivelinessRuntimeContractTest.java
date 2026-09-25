package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class LivelinessRuntimeContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void calmRepositionUsesOnlyCoordinatorAndNeverTeleports() throws Exception {
        String source = Files.readString(ENTITY);
        String implementation = source.substring(source.indexOf("private void tickSocialReposition"),
                source.indexOf("private void performAffection"));
        assertTrue(implementation.contains("Locomotion.SOCIAL_MOVE"));
        assertTrue(implementation.contains("CompanionLivelinessPolicy.canSocialMove"));
        assertTrue(implementation.contains("createDryPathTo"));
        assertTrue(implementation.contains("isSafeHomeLanding"));
        assertFalse(implementation.contains("trySafeTeleport"));
        assertFalse(implementation.contains("teleportTo"));
        assertFalse(implementation.contains("changeDimension"));
    }

    @Test
    void oldFriendWanderGoalRemainsInactiveAndFollowYieldsToActiveReposition() throws Exception {
        String source = Files.readString(ENTITY);
        String registrations = source.substring(source.indexOf("protected void registerGoals()"),
                source.indexOf("protected Brain.Provider"));
        assertEquals(0, occurrences(registrations, "new WanderNearFriendGoal"));
        assertTrue(source.contains("interactionMovementStopLatched || socialRepositionTarget != null"));
        assertTrue(source.contains("!interactionMovementStopLatched && socialRepositionTarget == null"));
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
