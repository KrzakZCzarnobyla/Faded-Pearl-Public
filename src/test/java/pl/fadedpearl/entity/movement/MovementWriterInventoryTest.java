package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the completed Stage C navigation/look cutover. */
class MovementWriterInventoryTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");
    private static final Path COMMAND_PACKET =
            Path.of("src/main/java/pl/fadedpearl/network/SetCompanionCommandPacket.java");

    @Test
    void directWritersExistOnlyInCentralizedAdapter() throws IOException {
        String source = Files.readString(ENTITY);
        assertEquals(4, occurrences(source, "getNavigation().moveTo"));
        assertEquals(1, occurrences(source, "getNavigation().stop"));
        assertEquals(3, occurrences(source, "getLookControl().setLookAt"));
        assertEquals(1, occurrences(source, "setTarget("));
        assertEquals(1, occurrences(source, "if (!level().isClientSide) applyMovementDecision();"));
        assertEquals(1, occurrences(source, "Locomotion.FOLLOW_WATER_BARRIER_RECOVERY"));
    }

    @Test
    void commandPacketQueuesStopInsteadOfWritingNavigation() throws IOException {
        String source = Files.readString(COMMAND_PACKET);
        assertEquals(0, occurrences(source, "getNavigation().stop"));
        assertEquals(1, occurrences(source, "requestCommandTransitionStop(\"command packet\")"));
    }

    @Test
    void smartBrainKeepsMemoryProducersButNotMovementOrLookExecutors() throws IOException {
        String source = Files.readString(ENTITY);
        assertEquals(0, occurrences(source, "new LookAtTarget<>"));
        assertEquals(0, occurrences(source, "new MoveToWalkTarget<>"));
        assertTrue(source.contains("new SetPlayerLookTarget<"));
        assertTrue(source.contains("new SetRandomLookTarget<"));
        assertTrue(source.contains("new SetRandomWalkTarget<"));
        assertTrue(source.contains("queueBrainMemoryCandidates();"));
    }

    @Test
    void manualBehaviorRegionContainsOnlyDeferredAdapterActions() throws IOException {
        String source = Files.readString(ENTITY);
        String manual = source.substring(source.indexOf("private void tickExpansionDetectors()"),
                source.indexOf("private final class FollowFriendGoal"));
        assertEquals(0, occurrences(manual, "getNavigation().moveTo"));
        assertEquals(0, occurrences(manual, "getNavigation().stop"));
        assertEquals(0, occurrences(manual, "getLookControl().setLookAt"));
        assertEquals(0, occurrences(manual, "setTarget("));
        assertTrue(manual.contains("queueLocomotion(FadedMovementCoordinator.Locomotion.GUARD_INTERPOSE"));
        assertTrue(manual.contains("queueLocomotion(FadedMovementCoordinator.Locomotion.ESCAPE_WATER"));
        assertTrue(manual.contains("applyNavigationStop();"));
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
