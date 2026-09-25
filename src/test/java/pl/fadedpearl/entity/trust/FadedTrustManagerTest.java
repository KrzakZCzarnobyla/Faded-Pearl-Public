package pl.fadedpearl.entity.trust;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FadedTrustManagerTest {
    @Test
    void classifiesEveryStageBoundary() {
        int[] values = {5, 6, 11, 12, 24, 25, 34, 35, 59, 60, 74, 75, 84, 85};
        int[] stages = {0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7};
        for (int index = 0; index < values.length; index++)
            assertEquals(stages[index], FadedTrustManager.stageIndex(values[index]), "trust=" + values[index]);
    }

    @Test
    void clampsOnlyTheModificationResult() {
        assertEquals(0, FadedTrustManager.applyModifier(1, FadedTrustManager.PLAYER_ATTACK));
        assertEquals(100, FadedTrustManager.applyModifier(100, FadedTrustManager.FLOWER_ACCEPTED));
        assertEquals(55, FadedTrustManager.applyModifier(50, 5));
    }

    @Test
    void exposesEveryExistingNamedModifier() {
        assertAll(
                () -> assertEquals(1, FadedTrustManager.FLOWER_ACCEPTED),
                () -> assertEquals(2, FadedTrustManager.CARRY_STARTED),
                () -> assertEquals(-8, FadedTrustManager.PLAYER_ATTACK),
                () -> assertEquals(-1, FadedTrustManager.PERSISTENT_STARE),
                () -> assertEquals(-3, FadedTrustManager.RAIN_EXPOSURE),
                () -> assertEquals(2, FadedTrustManager.SHARED_SHELTER),
                () -> assertEquals(-10, FadedTrustManager.ENTERED_WATER),
                () -> assertEquals(1, FadedTrustManager.NEW_LIGHT),
                () -> assertEquals(3, FadedTrustManager.RETURN_AFTER_ABSENCE),
                () -> assertEquals(1, FadedTrustManager.PROXIMITY),
                () -> assertEquals(1, FadedTrustManager.FLOWER_GIFT),
                () -> assertEquals(1, FadedTrustManager.TOUCH),
                () -> assertEquals(2, FadedTrustManager.FALL_RESCUE),
                () -> assertEquals(3, FadedTrustManager.LAVA_RESCUE),
                () -> assertEquals(2, FadedTrustManager.LOW_HEALTH_RESCUE),
                () -> assertEquals(1, FadedTrustManager.ANCHOR_RECALL));
    }

    @Test
    void identifiesOnlyPositiveModifiersForInteractionTimeUpdates() {
        assertTrue(FadedTrustManager.isPositiveModifier(1));
        assertFalse(FadedTrustManager.isPositiveModifier(0));
        assertFalse(FadedTrustManager.isPositiveModifier(-1));
    }

    @Test
    void scalesPositiveAndNegativeModifiersWithoutChangingTheirSign() {
        assertEquals(2, FadedTrustManager.scaleModifier(1, 200, 100));
        assertEquals(-4, FadedTrustManager.scaleModifier(-8, 100, 50));
        assertEquals(1, FadedTrustManager.scaleModifier(1, 25, 100));
        assertEquals(-1, FadedTrustManager.scaleModifier(-1, 100, 25));
        assertEquals(0, FadedTrustManager.scaleModifier(3, 0, 100));
        assertEquals(0, FadedTrustManager.scaleModifier(-3, 100, 0));
        assertEquals(0, FadedTrustManager.scaleModifier(0, 500, 500));
    }

    @Test
    void preservesKeyThresholdPredicatesAndTouchOrdering() {
        assertFalse(FadedTrustManager.canCarry(74)); assertTrue(FadedTrustManager.canCarry(75));
        assertTrue(FadedTrustManager.usesLowGestureResponse(34)); assertFalse(FadedTrustManager.usesLowGestureResponse(35));
        assertTrue(FadedTrustManager.isWorldCautious(11)); assertFalse(FadedTrustManager.isWorldCautious(12));
        assertFalse(FadedTrustManager.isWorldLearning(11)); assertTrue(FadedTrustManager.isWorldLearning(12));
        assertTrue(FadedTrustManager.isWorldLearning(29)); assertFalse(FadedTrustManager.isWorldLearning(30));
        assertFalse(FadedTrustManager.allowsTouchWound(59)); assertTrue(FadedTrustManager.allowsTouchWound(60));
        assertFalse(FadedTrustManager.allowsAffection(34)); assertTrue(FadedTrustManager.allowsAffection(35));
        assertTrue(FadedTrustManager.usesTouchRecoil(5)); assertFalse(FadedTrustManager.usesTouchRecoil(6));
        assertTrue(FadedTrustManager.usesTouchHesitation(6)); assertFalse(FadedTrustManager.usesTouchHesitation(12));
        assertTrue(FadedTrustManager.usesTouchLearning(12)); assertTrue(FadedTrustManager.usesTouchLearning(39));
        assertFalse(FadedTrustManager.usesTouchLearning(40));
        assertTrue(FadedTrustManager.usesTouchChestExpose(60)); assertTrue(FadedTrustManager.usesTouchChestExpose(84));
        assertFalse(FadedTrustManager.usesTouchChestExpose(85)); assertTrue(FadedTrustManager.usesTouchDevotion(85));
        assertFalse(FadedTrustManager.canRescueFall(34)); assertTrue(FadedTrustManager.canRescueFall(35));
        assertFalse(FadedTrustManager.canRescueLowHealth(59)); assertTrue(FadedTrustManager.canRescueLowHealth(60));
    }
}
