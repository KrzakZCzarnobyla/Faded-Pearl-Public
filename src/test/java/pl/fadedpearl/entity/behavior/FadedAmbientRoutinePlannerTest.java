package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Command.FOLLOW;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Command.HOME;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Routine.HOME_SETTLE;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Routine.NONE;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Routine.REST_NEAR;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Routine.SHARED_GAZE;
import static pl.fadedpearl.entity.behavior.FadedAmbientRoutinePlanner.Routine.SOCIAL_REPOSITION;

final class FadedAmbientRoutinePlannerTest {
    @Test
    void everyPriorityBlockReturnsNoneWithoutConsumingChoiceRng() {
        for (Blocker blocker : Blocker.values()) {
            AtomicInteger calls = new AtomicInteger();
            FadedAmbientRoutinePlanner.Decision decision = FadedAmbientRoutinePlanner.plan(
                    snapshot(100, FOLLOW, blocker, true, true, true),
                    () -> {
                        calls.incrementAndGet();
                        return 0;
                    });

            assertAll(blocker.name(),
                    () -> assertEquals(NONE, decision.routine()),
                    () -> assertEquals(0, calls.get()));
        }
    }

    @Test
    void everyNonFollowCommandBlocksWithoutConsumingChoiceRng() {
        for (FadedAmbientRoutinePlanner.Command command : FadedAmbientRoutinePlanner.Command.values()) {
            if (command == FOLLOW) continue;
            AtomicInteger calls = new AtomicInteger();

            assertAll(command.name(),
                    () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                            snapshot(100, command, null, true, true, true),
                            calls::incrementAndGet).routine()),
                    () -> assertEquals(0, calls.get()));
        }
    }

    @Test
    void sharedGazeStartsAtTwelveWithContextualPriorityAndWithoutConsumingChoiceRng() {
        AtomicInteger calls = new AtomicInteger();

        assertAll(
                () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                        snapshot(11, FOLLOW, null, true, true, true),
                        calls::incrementAndGet).routine()),
                () -> assertEquals(SHARED_GAZE, FadedAmbientRoutinePlanner.plan(
                        snapshot(12, FOLLOW, null, true, true, true),
                        calls::incrementAndGet).routine()),
                () -> assertEquals(0, calls.get()));
    }

    @Test
    void restNearStartsAtThirtyFiveAndDoesNotNeedRngWhenItIsTheOnlyCandidate() {
        AtomicInteger calls = new AtomicInteger();

        assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                snapshot(34, FOLLOW, null, false, true, false), calls::incrementAndGet).routine());
        assertEquals(REST_NEAR, FadedAmbientRoutinePlanner.plan(
                snapshot(35, FOLLOW, null, false, true, false), calls::incrementAndGet).routine());
        assertEquals(0, calls.get());
    }

    @Test
    void socialRepositionStartsAtSixtyAndDoesNotNeedRngWhenItIsTheOnlyCandidate() {
        AtomicInteger calls = new AtomicInteger();

        assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                snapshot(59, FOLLOW, null, false, false, true), calls::incrementAndGet).routine());
        assertEquals(SOCIAL_REPOSITION, FadedAmbientRoutinePlanner.plan(
                snapshot(60, FOLLOW, null, false, false, true), calls::incrementAndGet).routine());
        assertEquals(0, calls.get());
    }

    @Test
    void bothNonContextualCandidatesUseExactlyOneBoundedChoice() {
        AtomicInteger restCalls = new AtomicInteger();
        AtomicInteger repositionCalls = new AtomicInteger();
        FadedAmbientRoutinePlanner.Snapshot both = snapshot(60, FOLLOW, null, false, true, true);

        assertAll(
                () -> assertEquals(REST_NEAR, FadedAmbientRoutinePlanner.plan(both, () -> {
                    restCalls.incrementAndGet();
                    return 0;
                }).routine()),
                () -> assertEquals(1, restCalls.get()),
                () -> assertEquals(SOCIAL_REPOSITION, FadedAmbientRoutinePlanner.plan(both, () -> {
                    repositionCalls.incrementAndGet();
                    return 1;
                }).routine()),
                () -> assertEquals(1, repositionCalls.get()));
    }

    @Test
    void rejectsAnUnboundedChoiceAfterExactlyOneRead() {
        AtomicInteger calls = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> FadedAmbientRoutinePlanner.plan(
                snapshot(60, FOLLOW, null, false, true, true),
                () -> {
                    calls.incrementAndGet();
                    return 2;
                }));
        assertEquals(1, calls.get());
    }

    @Test
    void noAvailableCandidateReturnsNoneWithoutRequiringRng() {
        assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                snapshot(100, FOLLOW, null, false, false, false), null).routine());
    }

    @Test
    void restContextRequiresEightCalmSecondsAndAThreeBlockDistance() {
        assertFalse(FadedAmbientRoutinePlanner.canRestNear(159, 9.0D));
        assertTrue(FadedAmbientRoutinePlanner.canRestNear(160, 9.0D));
        assertFalse(FadedAmbientRoutinePlanner.canRestNear(160, 9.01D));
        assertFalse(FadedAmbientRoutinePlanner.canRestNear(160, Double.NaN));
    }

    @Test
    void calmRestReservationUsesTheSameTrustAndDistanceBoundaries() {
        assertFalse(FadedAmbientRoutinePlanner.canAccumulateRestNear(34, 9.0D));
        assertTrue(FadedAmbientRoutinePlanner.canAccumulateRestNear(35, 9.0D));
        assertFalse(FadedAmbientRoutinePlanner.canAccumulateRestNear(35, 9.01D));
        assertFalse(FadedAmbientRoutinePlanner.canAccumulateRestNear(35, Double.NaN));
    }

    @Test
    void homeSettleRequiresEightCalmSecondsAndAnEightBlockDistance() {
        assertFalse(FadedAmbientRoutinePlanner.canSettleAtHome(159, 64.0D));
        assertTrue(FadedAmbientRoutinePlanner.canSettleAtHome(160, 64.0D));
        assertFalse(FadedAmbientRoutinePlanner.canSettleAtHome(160, 64.01D));
        assertFalse(FadedAmbientRoutinePlanner.canSettleAtHome(160, Double.NaN));
    }

    @Test
    void validHomeContextSelectsHomeSettleWithoutFriendOrChoiceRng() {
        AtomicInteger calls = new AtomicInteger();

        FadedAmbientRoutinePlanner.Decision decision = FadedAmbientRoutinePlanner.plan(
                homeSnapshot(null, true, true, true, true), calls::incrementAndGet);

        assertAll(
                () -> assertEquals(HOME_SETTLE, decision.routine()),
                () -> assertEquals(0, calls.get()));
    }

    @Test
    void incompleteHomeContextDoesNotSettle() {
        assertAll(
                () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                        homeSnapshot(null, false, true, true, true), null).routine()),
                () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                        homeSnapshot(null, true, false, true, true), null).routine()),
                () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                        homeSnapshot(null, true, true, false, true), null).routine()),
                () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                        homeSnapshot(null, true, true, true, false), null).routine()));
    }

    @Test
    void commonPriorityBlockersAlsoBlockHomeSettleWithoutChoiceRng() {
        for (Blocker blocker : Blocker.values()) {
            if (blocker == Blocker.FRIEND_MISSING || blocker == Blocker.FRIEND_MOVING) continue;
            AtomicInteger calls = new AtomicInteger();

            assertAll(blocker.name(),
                    () -> assertEquals(NONE, FadedAmbientRoutinePlanner.plan(
                            homeSnapshot(blocker, true, true, true, true), calls::incrementAndGet).routine()),
                    () -> assertEquals(0, calls.get()));
        }
    }

    private static FadedAmbientRoutinePlanner.Snapshot snapshot(
            int trust,
            FadedAmbientRoutinePlanner.Command command,
            Blocker blocker,
            boolean sharedGaze,
            boolean restNear,
            boolean socialReposition
    ) {
        return new FadedAmbientRoutinePlanner.Snapshot(
                trust,
                command,
                blocker != Blocker.UNHEALED,
                blocker != Blocker.FRIEND_MISSING,
                blocker != Blocker.FRIEND_MOVING,
                blocker != Blocker.COOLDOWN_NOT_READY,
                blocker == Blocker.DOWNED,
                blocker == Blocker.HEALING,
                blocker == Blocker.PLAYER_RESCUE,
                blocker == Blocker.RECOVERY,
                blocker == Blocker.COMBAT,
                blocker == Blocker.THREAT,
                blocker == Blocker.UNSAFE_ENVIRONMENT,
                blocker == Blocker.CARRYING_PLAYER,
                blocker == Blocker.CARRYING_ANIMAL,
                blocker == Blocker.PLAYER_INTERACTION,
                blocker == Blocker.ITEM_CURIOSITY,
                blocker == Blocker.WORLD_CURIOSITY,
                blocker == Blocker.SOCIAL_ACTION,
                blocker == Blocker.FADE_MEETING,
                blocker == Blocker.OTHER_ROUTINE,
                sharedGaze,
                restNear,
                socialReposition,
                false,
                false,
                false,
                false);
    }

    private static FadedAmbientRoutinePlanner.Snapshot homeSnapshot(
            Blocker blocker,
            boolean hasHome,
            boolean sameDimension,
            boolean nearHome,
            boolean available
    ) {
        return new FadedAmbientRoutinePlanner.Snapshot(
                0,
                HOME,
                blocker != Blocker.UNHEALED,
                false,
                false,
                blocker != Blocker.COOLDOWN_NOT_READY,
                blocker == Blocker.DOWNED,
                blocker == Blocker.HEALING,
                blocker == Blocker.PLAYER_RESCUE,
                blocker == Blocker.RECOVERY,
                blocker == Blocker.COMBAT,
                blocker == Blocker.THREAT,
                blocker == Blocker.UNSAFE_ENVIRONMENT,
                blocker == Blocker.CARRYING_PLAYER,
                blocker == Blocker.CARRYING_ANIMAL,
                blocker == Blocker.PLAYER_INTERACTION,
                blocker == Blocker.ITEM_CURIOSITY,
                blocker == Blocker.WORLD_CURIOSITY,
                blocker == Blocker.SOCIAL_ACTION,
                blocker == Blocker.FADE_MEETING,
                blocker == Blocker.OTHER_ROUTINE,
                false,
                false,
                false,
                hasHome,
                sameDimension,
                nearHome,
                available);
    }

    private enum Blocker {
        UNHEALED,
        FRIEND_MISSING,
        FRIEND_MOVING,
        COOLDOWN_NOT_READY,
        DOWNED,
        HEALING,
        PLAYER_RESCUE,
        RECOVERY,
        COMBAT,
        THREAT,
        UNSAFE_ENVIRONMENT,
        CARRYING_PLAYER,
        CARRYING_ANIMAL,
        PLAYER_INTERACTION,
        ITEM_CURIOSITY,
        WORLD_CURIOSITY,
        SOCIAL_ACTION,
        FADE_MEETING,
        OTHER_ROUTINE
    }
}
