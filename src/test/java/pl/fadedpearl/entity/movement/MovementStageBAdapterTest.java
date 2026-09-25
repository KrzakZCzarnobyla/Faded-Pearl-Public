package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementStageBAdapterTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");
    private static final Path COMMAND_PACKET =
            Path.of("src/main/java/pl/fadedpearl/network/SetCompanionCommandPacket.java");

    @Test
    void serverCycleHasExactlyOneDecisionAndOneApplySite() throws IOException {
        String source = Files.readString(ENTITY);
        assertEquals(1, occurrences(source, "movementCoordinator.resolve("));
        assertEquals(1, occurrences(source, "if (!level().isClientSide) applyMovementDecision();"));
        assertEquals(1, occurrences(source,
                "pendingLocomotion.stream().filter(pending -> pending.candidate().equals(decision.locomotion()))"));
        assertEquals(1, occurrences(source,
                "pendingLook.stream().filter(pending -> pending.candidate().equals(decision.look()))"));
        assertEquals(1, occurrences(source,
                "pendingTarget.stream().filter(pending -> pending.candidate().equals(decision.targetPolicy()))"));
    }

    @Test
    void interactionStopIsImmediateLatchedAndConsumedByTheSingleNextApply() throws IOException {
        String source = Files.readString(ENTITY);
        int writer = source.indexOf("private void applyNavigationStop()");
        int request = source.indexOf("public void requestInteractionMovementStop(String reason)");
        int apply = source.indexOf("private void applyMovementDecision()");
        int clear = source.indexOf("pendingLocomotion.clear();", apply);
        assertTrue(writer >= 0 && request > writer && apply > request && clear > apply);
        String beforeGoals = source.substring(0, source.indexOf("private final class FollowFriendGoal"));
        assertEquals(1, occurrences(beforeGoals, "getNavigation().stop();"));
        String immediateWriter = source.substring(writer, source.indexOf("private void applyImmediateStop"));
        assertEquals(1, occurrences(immediateWriter, "getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);"));
        assertTrue(source.substring(writer, request).contains("ImmediateStopIntent.INTERACTION_LATCH"));
        assertTrue(source.substring(writer, request).contains("Locomotion.EXTERNAL_INTERACTION_STOP"));
        assertTrue(source.contains("requestInteractionMovementStop(\"open command menu\")"));
        assertTrue(source.indexOf("requestInteractionMovementStop(\"open command menu\")")
                < source.indexOf("ModNetwork.openCommandMenu"));
        assertTrue(source.contains("interactionMovementStopLatched = false;"));
        assertEquals(1, occurrences(source.substring(apply, clear),
                "pendingLocomotion.stream().filter"));
        assertEquals(1, occurrences(source, "pendingLocomotion.clear();"));
    }

    @Test
    void commandTransitionUsesImmediateWriterWithoutInteractionLatch() throws IOException {
        String source = Files.readString(ENTITY);
        String method = source.substring(source.indexOf("public void requestCommandTransitionStop"),
                source.indexOf("private void applyMovementDecision()"));
        assertTrue(method.contains("ImmediateStopIntent.COMMAND_TRANSITION"));
        assertEquals(0, occurrences(method, "EXTERNAL_INTERACTION_STOP"));
        assertTrue(occurrences(source, "interactionMovementStopLatched") >= 10);
        assertTrue(source.contains("!interactionMovementStopLatched"));
        assertTrue(source.contains("interactionMovementStopLatched || !isHealed()"));

        String packet = Files.readString(COMMAND_PACKET);
        int stopOldRoute = packet.indexOf("requestCommandTransitionStop(\"command packet\")");
        int installNewCommand = packet.indexOf("setCommand(command)");
        assertTrue(stopOldRoute >= 0 && installNewCommand > stopOldRoute);
    }

    @Test
    void menuLatchBlocksHomePathWanderAndDimensionTransferUntilApply() throws IOException {
        String source = Files.readString(ENTITY);
        String homeDimension = source.substring(source.indexOf("private final class ReturnHomeDimensionGoal"),
                source.indexOf("private final class ReturnHomeGoal"));
        String homePath = source.substring(source.indexOf("private final class ReturnHomeGoal"),
                source.indexOf("private BlockPos findSafeHomeLanding"));
        String homeWander = source.substring(source.indexOf("private final class WanderNearAnchorGoal"),
                source.indexOf("private final class WanderNearFriendGoal"));
        assertTrue(homeDimension.contains("interactionMovementStopLatched"));
        assertTrue(homeDimension.indexOf("interactionMovementStopLatched")
                < homeDimension.indexOf("transferHomeDimension"));
        assertTrue(homePath.contains("interactionMovementStopLatched"));
        assertTrue(homeWander.contains("interactionMovementStopLatched"));
    }

    @Test
    void manualSourcesProduceTypedCandidatesWithBaselineSpeeds() throws IOException {
        String source = Files.readString(ENTITY);
        for (String preserved : new String[]{
                "1.0D, \"persistent stare recoil\"",
                "0.9D, \"deliver flower\"",
                "1.15D, \"continue worried\"",
                "1.05D, \"rain shelter\"",
                ".72D, \"ground flower\"",
                ".72D, \"watch sleeping\"",
                ".78D, \"night close\"",
                ".65D, \"rest near\"",
                "1.2D, \"guard interpose\""}) {
            assertTrue(source.contains(preserved), preserved);
        }
        assertTrue(source.contains("Locomotion.RESCUE_FALL"));
        assertTrue(source.contains("Locomotion.RESCUE_FIRE"));
        assertTrue(source.contains("Locomotion.RESCUE_LOW_HEALTH"));
        assertTrue(source.contains("Locomotion.ESCAPE_WATER"));
        assertTrue(source.contains("Locomotion.PASSENGER_CONTROLLED"));
    }

    @Test
    void manualBlinkHudAndNbtRemainUntouchedAfterStageCCutover() throws IOException {
        String source = Files.readString(ENTITY);
        assertTrue(source.contains("if (blinkWithRider(player)) setCarriedBlinkCooldown(80);"));
        assertTrue(source.contains("goalSelector.addGoal(1, new ArbitratedMeleeAttackGoal())"));
        assertEquals(0, occurrences(source, "new LookAtTarget<>()"));
        assertEquals(0, occurrences(source, "new MoveToWalkTarget<>()"));
        assertEquals(1, occurrences(source, "Locomotion.FOLLOW_WATER_BARRIER_RECOVERY"));
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
