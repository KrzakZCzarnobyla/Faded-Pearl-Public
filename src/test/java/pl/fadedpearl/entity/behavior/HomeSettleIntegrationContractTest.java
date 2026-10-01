package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HomeSettleIntegrationContractTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void homeSettleRunsBeforeHomeCommandResetsOrdinarySocialBehavior() throws IOException {
        String source = Files.readString(ENTITY);
        int calmUpdate = source.indexOf("updateAmbientCalmTicks(friend);");
        int settleTick = source.indexOf("tickHomeSettle(friend)", calmUpdate);
        int ordinaryReset = source.indexOf("boolean reset =", settleTick);

        assertTrue(calmUpdate >= 0);
        assertTrue(settleTick > calmUpdate);
        assertTrue(ordinaryReset > settleTick);
    }

    @Test
    void sessionIsTransientAndHasNoTrustOrJournalSideEffects() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("private boolean tickHomeSettle(Player friend)");
        int end = source.indexOf("private boolean isHomeSettleSafe()", start);
        String method = source.substring(start, end);

        assertTrue(method.contains("SocialAction.HOME_SETTLE"));
        assertTrue(method.contains("FadedMovementCoordinator.Locomotion.AMBIENT_REST"));
        assertTrue(method.contains("socialActionTicks = 200 + random.nextInt(101)"));
        assertTrue(method.contains("socialActionCooldown = 2400 + random.nextInt(2401)"));
        assertFalse(method.contains("addTrust("));
        assertFalse(method.contains("modifyTrust("));
        assertFalse(method.contains("journalMemory"));
    }

    @Test
    void safetyGateCoversUrgentStatesAndEnvironment() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("private boolean isHomeSettleSafe()");
        int end = source.indexOf("private void updateAmbientCalmTicks", start);
        String method = source.substring(start, end);

        assertTrue(method.contains("CompanionCommand.HOME"));
        assertTrue(method.contains("isFriendRescueActive()"));
        assertTrue(method.contains("pendingBarrierRecoveryOutcome"));
        assertTrue(method.contains("isInWaterOrBubble()"));
        assertTrue(method.contains("isOnFire()"));
        assertTrue(method.contains("level().isRainingAt(blockPosition())"));
        assertTrue(method.contains("animalCarryTarget == null"));
        assertTrue(method.contains("interactionMovementStopLatched"));
        assertTrue(method.contains("worldInterestPhase == WorldInterestPhase.NONE"));
    }

    @Test
    void homeWanderYieldsWhileSettleIsActive() throws IOException {
        String source = Files.readString(ENTITY);
        int start = source.indexOf("private final class WanderNearAnchorGoal");
        int end = source.indexOf("private final class WanderNearFriendGoal", start);
        String goal = source.substring(start, end);

        assertTrue(goal.contains("getSocialAction() == SocialAction.HOME_SETTLE"));
        assertTrue(goal.contains("getSocialAction() != SocialAction.HOME_SETTLE"));
    }

    @Test
    void readyHomeSettleWindowCannotBeStarvedByAnchorWandering() throws IOException {
        String source = Files.readString(ENTITY);
        int helperStart = source.indexOf("private boolean reservesHomeWanderForSettle()");
        int helperEnd = source.indexOf("private void updateAmbientCalmTicks", helperStart);
        String helper = source.substring(helperStart, helperEnd);
        int goalStart = source.indexOf("private final class WanderNearAnchorGoal");
        int goalEnd = source.indexOf("private final class WanderNearFriendGoal", goalStart);
        String goal = source.substring(goalStart, goalEnd);

        assertTrue(helper.contains("socialActionCooldown <= 0"));
        assertTrue(helper.contains("getSocialAction() == SocialAction.NONE"));
        assertTrue(helper.contains("isHomeSettleSafe()"));
        assertTrue(goal.contains("reservesHomeWanderForSettle()"));
    }

    @Test
    void transientTimerIsNotAddedToPersistenceCodec() throws IOException {
        String codec = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/persistence/FadedPersistenceCodec.java"));

        assertFalse(codec.contains("HomeSettle"));
        assertFalse(codec.contains("homeSettle"));
    }
}
