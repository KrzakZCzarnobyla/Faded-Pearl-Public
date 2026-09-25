package pl.fadedpearl.entity.social;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class SmallAnimalCarryPolicyTest {
    @Test
    void trustAndSafetyGate() {
        assertFalse(SmallAnimalCarryPolicy.canStart(context(59, false, 0)));
        assertTrue(SmallAnimalCarryPolicy.canStart(context(60, false, 0)));
        assertFalse(SmallAnimalCarryPolicy.canStart(context(60, true, 0)));
        assertFalse(SmallAnimalCarryPolicy.canStart(context(60, false, 1)));
    }

    @Test
    void babyAndSmallAdultHaveDifferentSizeLimits() {
        assertTrue(SmallAnimalCarryPolicy.canCarry(candidate(true, 1.0F, 1.3F)));
        assertFalse(SmallAnimalCarryPolicy.canCarry(candidate(false, 1.0F, 1.3F)));
        assertTrue(SmallAnimalCarryPolicy.canCarry(candidate(false, 0.7F, 0.9F)));
    }

    @Test
    void unsafeOrSomeoneElsesAnimalIsRejected() {
        SmallAnimalCarryPolicy.Candidate safe = candidate(false, 0.7F, 0.9F);
        assertFalse(SmallAnimalCarryPolicy.canCarry(new SmallAnimalCarryPolicy.Candidate(
                true, true, false, safe.width(), safe.height(), true,
                false, false, false, false, false)));
        assertFalse(SmallAnimalCarryPolicy.canCarry(new SmallAnimalCarryPolicy.Candidate(
                true, true, false, safe.width(), safe.height(), true,
                true, false, false, false, true)));
        assertFalse(SmallAnimalCarryPolicy.canCarry(new SmallAnimalCarryPolicy.Candidate(
                true, true, false, safe.width(), safe.height(), false,
                false, false, false, false, true)));
    }

    @Test
    void dangerAndTravelReleaseButCalmDoesNot() {
        assertFalse(SmallAnimalCarryPolicy.shouldRelease(new SmallAnimalCarryPolicy.Interruption(
                false, false, false, false, false, false, false, false)));
        assertTrue(SmallAnimalCarryPolicy.shouldRelease(new SmallAnimalCarryPolicy.Interruption(
                false, true, false, false, false, false, false, false)));
        assertTrue(SmallAnimalCarryPolicy.shouldRelease(new SmallAnimalCarryPolicy.Interruption(
                false, false, false, true, false, false, false, false)));
        assertTrue(SmallAnimalCarryPolicy.shouldRelease(new SmallAnimalCarryPolicy.Interruption(
                false, false, false, false, true, false, false, false)));
    }

    @Test
    void escapeCanOnlyBeTriedAfterInitialCalmPeriod() {
        assertFalse(SmallAnimalCarryPolicy.canTryEscape(0));
        assertFalse(SmallAnimalCarryPolicy.canTryEscape(79));
        assertTrue(SmallAnimalCarryPolicy.canTryEscape(80));
        assertFalse(SmallAnimalCarryPolicy.canTryEscape(81));
        assertTrue(SmallAnimalCarryPolicy.canTryEscape(100));
        assertTrue(SmallAnimalCarryPolicy.CARRY_MIN_TICKS > 80);
        assertTrue(SmallAnimalCarryPolicy.GLOBAL_COOLDOWN_MIN_TICKS
                > SmallAnimalCarryPolicy.CARRY_MIN_TICKS);
    }

    @Test
    void completedCarryWaitsTwoToFourMinutes() {
        assertEquals(2400, SmallAnimalCarryPolicy.nextGlobalCooldown(bound -> 0));
        assertEquals(4800, SmallAnimalCarryPolicy.nextGlobalCooldown(bound -> bound - 1));
    }

    @Test
    void sameAnimalWaitsLongerButOthersRemainEligible() {
        UUID carried = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertFalse(SmallAnimalCarryPolicy.canSelectAnimal(carried, carried, 1));
        assertTrue(SmallAnimalCarryPolicy.canSelectAnimal(other, carried, 1));
        assertTrue(SmallAnimalCarryPolicy.canSelectAnimal(carried, carried, 0));
        assertTrue(SmallAnimalCarryPolicy.SAME_ANIMAL_COOLDOWN_TICKS > 4800);
    }

    private static SmallAnimalCarryPolicy.Context context(int trust, boolean threat, int cooldown) {
        return new SmallAnimalCarryPolicy.Context(true, false, trust, true, true,
                threat, false, true, true, cooldown);
    }

    private static SmallAnimalCarryPolicy.Candidate candidate(boolean baby, float width, float height) {
        return new SmallAnimalCarryPolicy.Candidate(true, true, baby, width, height,
                true, false, false, false, false, true);
    }
}
