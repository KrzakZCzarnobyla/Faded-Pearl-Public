package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CompanionRecoveryStatusTest {
    @Test
    void standCountdownRoundsUpAndClamps() {
        assertEquals(30, CompanionRecoveryStatus.secondsUntilStand(0));
        assertEquals(30, CompanionRecoveryStatus.secondsUntilStand(1));
        assertEquals(29, CompanionRecoveryStatus.secondsUntilStand(20));
        assertEquals(1, CompanionRecoveryStatus.secondsUntilStand(599));
        assertEquals(0, CompanionRecoveryStatus.secondsUntilStand(600));
    }

    @Test
    void healthEstimateReflectsPassiveHealInterval() {
        assertEquals(80, CompanionRecoveryStatus.approximateSecondsUntilFull(20, 40));
        assertEquals(4, CompanionRecoveryStatus.approximateSecondsUntilFull(39, 40));
        assertEquals(0, CompanionRecoveryStatus.approximateSecondsUntilFull(40, 40));
    }
}
