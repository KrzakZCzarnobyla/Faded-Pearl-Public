package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedArmorProgressPolicyTest {
    @Test
    void requiresAnEstablishedNonzeroBaselineAndANewMaximum() {
        assertFalse(FadedArmorProgressPolicy.isUpgrade(0, 15));
        assertFalse(FadedArmorProgressPolicy.isUpgrade(15, 15));
        assertFalse(FadedArmorProgressPolicy.isUpgrade(15, 8));
        assertTrue(FadedArmorProgressPolicy.isUpgrade(15, 18));
    }

    @Test
    void removingAndReequippingArmorNeverLowersTheObservedMaximum() {
        int maximum = FadedArmorProgressPolicy.nextMaximum(15, 0);
        assertEquals(15, maximum);
        assertFalse(FadedArmorProgressPolicy.isUpgrade(maximum, 15));
        assertEquals(15, FadedArmorProgressPolicy.nextMaximum(maximum, 15));
    }

    @Test
    void clampsInvalidNegativeInputsToAZeroFloor() {
        assertEquals(0, FadedArmorProgressPolicy.nextMaximum(-4, -1));
        assertEquals(7, FadedArmorProgressPolicy.nextMaximum(-4, 7));
    }
}
