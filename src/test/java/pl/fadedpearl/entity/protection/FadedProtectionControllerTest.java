package pl.fadedpearl.entity.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.protection.FadedProtectionController.RescueIntent.*;

class FadedProtectionControllerTest {
    @Test
    void missingFriendAndDownedBlockAllProtection() {
        assertEquals(new FadedProtectionController.Decision(false, NONE), resolve(false, false, true, false,
                7.0F, 100, 100.0D, true, 100, 0, 1.0F, 20.0F));
        assertEquals(new FadedProtectionController.Decision(false, NONE), resolve(true, true, true, false,
                7.0F, 100, 100.0D, true, 100, 0, 1.0F, 20.0F));
    }

    @Test
    void guardRequiresAttackerAndNoPassenger() {
        assertTrue(resolve(true, false, true, false, 0, 0, 0, false, 0, 1, 20, 20).guardAttacker());
        assertFalse(resolve(true, false, false, false, 0, 0, 0, false, 0, 1, 20, 20).guardAttacker());
        assertFalse(resolve(true, false, true, true, 0, 0, 0, false, 0, 1, 20, 20).guardAttacker());
    }

    @Test
    void guardIsPreludeToEveryRescue() {
        assertDecision(resolve(true, false, true, false, 6.1F, 35, 899, false, 0, 0, 20, 20), FALL);
        assertDecision(resolve(true, false, true, false, 0, 0, 0, true, 0, 0, 20, 20), LAVA_OR_FIRE);
        assertDecision(resolve(true, false, true, false, 0, 60, 0, false, 0, 0, 4, 20), LOW_HEALTH);
    }

    @Test
    void fallUsesExactDistanceTrustAndRangeThresholdsWithoutCooldown() {
        assertEquals(NONE, resolve(true, false, false, false, 6.0F, 35, 899, false, 0, 0, 20, 20).rescue());
        assertEquals(FALL, resolve(true, false, false, false, 6.01F, 35, 899, false, 0, 999, 20, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 6.01F, 34, 899, false, 0, 0, 20, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 6.01F, 35, 900, false, 0, 0, 20, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, true, 6.01F, 35, 899, false, 0, 0, 20, 20).rescue());
    }

    @Test
    void fallWinsOverLavaAndLowHealth() {
        assertEquals(FALL, resolve(true, false, false, false, 7, 60, 100, true, 100, 200, 1, 20).rescue());
    }

    @Test
    void lavaAndFireUseExactThresholdAndCooldownGate() {
        assertEquals(LAVA_OR_FIRE, resolve(true, false, false, true, 0, 0, 0, true, 0, 0, 20, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 0, 0, false, 80, 0, 20, 20).rescue());
        assertEquals(LAVA_OR_FIRE, resolve(true, false, false, false, 0, 0, 0, false, 81, 0, 20, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 0, 0, true, 81, 1, 20, 20).rescue());
    }

    @Test
    void lowHealthUsesFloorPercentageTrustAndCooldownThresholds() {
        assertEquals(LOW_HEALTH, resolve(true, false, false, true, 0, 60, 0, false, 0, 0, 4, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 60, 0, false, 0, 0, 4.01F, 20).rescue());
        assertEquals(LOW_HEALTH, resolve(true, false, false, false, 0, 60, 0, false, 0, 0, 8, 40).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 60, 0, false, 0, 0, 8.01F, 40).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 59, 0, false, 0, 0, 4, 20).rescue());
        assertEquals(NONE, resolve(true, false, false, false, 0, 60, 0, false, 0, 1, 4, 20).rescue());
    }

    @Test
    void noStimulusReturnsNoneWithoutGuard() {
        assertEquals(new FadedProtectionController.Decision(false, NONE), resolve(true, false, false, false,
                0, 100, 0, false, 0, 0, 20, 20));
    }

    @Test
    void fireRescueLandingOffsetKeepsMinimumDistanceAndExistingMaximumRadius() {
        assertFalse(FadedProtectionController.isValidFireRescueLandingOffset(0, 0));
        assertFalse(FadedProtectionController.isValidFireRescueLandingOffset(3, 0));
        assertTrue(FadedProtectionController.isValidFireRescueLandingOffset(4, 0));
        assertTrue(FadedProtectionController.isValidFireRescueLandingOffset(3, 3));
        assertTrue(FadedProtectionController.isValidFireRescueLandingOffset(8, 0));
        assertTrue(FadedProtectionController.isValidFireRescueLandingOffset(8, 8));
        assertFalse(FadedProtectionController.isValidFireRescueLandingOffset(9, 0));
        assertFalse(FadedProtectionController.isValidFireRescueLandingOffset(0, -9));
    }

    private static void assertDecision(FadedProtectionController.Decision decision,
                                       FadedProtectionController.RescueIntent rescue) {
        assertTrue(decision.guardAttacker());
        assertEquals(rescue, decision.rescue());
    }

    private static FadedProtectionController.Decision resolve(boolean friendPresent, boolean downed,
            boolean attackerPresent, boolean carryingPassenger, float fallDistance, int trust, double distanceSquared,
            boolean inLava, int fireTicks, int cooldown, float health, float maxHealth) {
        return FadedProtectionController.resolve(new FadedProtectionController.Snapshot(friendPresent, downed,
                attackerPresent, carryingPassenger, fallDistance, trust, distanceSquared, inLava, fireTicks,
                cooldown, health, maxHealth));
    }
}
