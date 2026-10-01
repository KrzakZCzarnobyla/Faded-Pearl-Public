package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.fadedpearl.entity.behavior.FadedWorldInterestPolicy.Decision.INSPECT;
import static pl.fadedpearl.entity.behavior.FadedWorldInterestPolicy.Decision.NONE;

final class FadedWorldInterestPolicyTest {
    @Test
    void knowledgeInterestStartsAtTwentyFiveTrust() {
        assertAll(
                () -> assertEquals(NONE, FadedWorldInterestPolicy.decide(snapshot(24, null))),
                () -> assertEquals(INSPECT, FadedWorldInterestPolicy.decide(snapshot(25, null))));
    }

    @Test
    void everySpecifiedBlockerPreventsKnowledgeInterest() {
        for (Blocker blocker : Blocker.values()) {
            assertEquals(NONE, FadedWorldInterestPolicy.decide(snapshot(25, blocker)), blocker.name());
        }
    }

    @Test
    void blockedDecisionDoesNotConsumeCooldownRng() {
        AtomicInteger calls = new AtomicInteger();
        FadedWorldInterestPolicy.Decision blocked = FadedWorldInterestPolicy.decide(
                snapshot(25, Blocker.COOLDOWN_NOT_READY));

        assertAll(
                () -> assertEquals(NONE, blocked),
                () -> assertEquals(0, FadedWorldInterestPolicy.rollCompletionCooldown(
                        blocked, calls::incrementAndGet)),
                () -> assertEquals(0, calls.get()));
    }

    @Test
    void acceptedStartRollsOneInclusiveCompletionCooldown() {
        AtomicInteger calls = new AtomicInteger();

        assertAll(
                () -> assertEquals(2400, FadedWorldInterestPolicy.rollCompletionCooldown(
                        INSPECT, () -> {
                            calls.incrementAndGet();
                            return 0;
                        })),
                () -> assertEquals(4800, FadedWorldInterestPolicy.rollCompletionCooldown(
                        INSPECT, () -> {
                            calls.incrementAndGet();
                            return 2400;
                        })),
                () -> assertEquals(2, calls.get()));
    }

    @Test
    void acceptedStartRejectsValuesOutsideTheCooldownBoundAfterOneRead() {
        AtomicInteger calls = new AtomicInteger();

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> FadedWorldInterestPolicy.rollCompletionCooldown(INSPECT, () -> {
                            calls.incrementAndGet();
                            return 2401;
                        })),
                () -> assertEquals(1, calls.get()));
    }

    @Test
    void timingConstantsMatchTheWorldInterestContract() {
        assertAll(
                () -> assertEquals(100, FadedWorldInterestPolicy.SCAN_CADENCE_TICKS),
                () -> assertEquals(200, FadedWorldInterestPolicy.APPROACH_TIMEOUT_TICKS),
                () -> assertEquals(100, FadedWorldInterestPolicy.INSPECT_DURATION_TICKS),
                () -> assertEquals(2400, FadedWorldInterestPolicy.COOLDOWN_MIN_TICKS),
                () -> assertEquals(4800, FadedWorldInterestPolicy.COOLDOWN_MAX_TICKS),
                () -> assertEquals(400, FadedWorldInterestPolicy.SHORT_FAILURE_COOLDOWN_TICKS));
    }

    private static FadedWorldInterestPolicy.Snapshot snapshot(int trust, Blocker blocker) {
        return new FadedWorldInterestPolicy.Snapshot(
                trust,
                blocker != Blocker.NON_FOLLOW_COMMAND,
                blocker != Blocker.UNHEALED,
                blocker != Blocker.NOT_CALM,
                blocker != Blocker.NOT_ON_DRY_SAFE_GROUND,
                blocker != Blocker.OWNER_MISSING_OR_TOO_FAR,
                blocker != Blocker.COOLDOWN_NOT_READY,
                blocker == Blocker.DOWNED,
                blocker == Blocker.HEALING,
                blocker == Blocker.RESCUE,
                blocker == Blocker.RECOVERY,
                blocker == Blocker.COMBAT,
                blocker == Blocker.THREAT,
                blocker == Blocker.IN_WATER,
                blocker == Blocker.ON_FIRE,
                blocker == Blocker.HARMFUL_RAIN,
                blocker == Blocker.UNSAFE_ENVIRONMENT,
                blocker == Blocker.CARRYING_PLAYER,
                blocker == Blocker.CARRYING_ANIMAL,
                blocker == Blocker.OWNER_INTERACTION,
                blocker == Blocker.ITEM_CURIOSITY,
                blocker == Blocker.FADE_MEETING,
                blocker == Blocker.OTHER_ROUTINE,
                blocker == Blocker.SOCIAL_ACTION,
                blocker == Blocker.HIGHER_PRIORITY_MOVEMENT,
                blocker != Blocker.NO_KNOWLEDGE_CANDIDATE);
    }

    private enum Blocker {
        NON_FOLLOW_COMMAND,
        UNHEALED,
        NOT_CALM,
        NOT_ON_DRY_SAFE_GROUND,
        OWNER_MISSING_OR_TOO_FAR,
        COOLDOWN_NOT_READY,
        DOWNED,
        HEALING,
        RESCUE,
        RECOVERY,
        COMBAT,
        THREAT,
        IN_WATER,
        ON_FIRE,
        HARMFUL_RAIN,
        UNSAFE_ENVIRONMENT,
        CARRYING_PLAYER,
        CARRYING_ANIMAL,
        OWNER_INTERACTION,
        ITEM_CURIOSITY,
        FADE_MEETING,
        OTHER_ROUTINE,
        SOCIAL_ACTION,
        HIGHER_PRIORITY_MOVEMENT,
        NO_KNOWLEDGE_CANDIDATE
    }
}
