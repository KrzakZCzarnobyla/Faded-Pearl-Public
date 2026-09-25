package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlinkCooldownHudStateTest {
    @Test
    void successfulBlinkStartsEmptyAndFillsOverEightyTicks() {
        assertEquals(0, BlinkCooldownHudState.filledPixels(80, 80));
        assertEquals(40, BlinkCooldownHudState.filledPixels(40, 80));
        assertEquals(79, BlinkCooldownHudState.filledPixels(1, 80));
        assertEquals(80, BlinkCooldownHudState.filledPixels(0, 80));
    }

    @Test
    void readyStateStaysFullAtZeroOrBelow() {
        assertTrue(BlinkCooldownHudState.isReady(0));
        assertTrue(BlinkCooldownHudState.isReady(-5));
        assertEquals(77, BlinkCooldownHudState.filledPixels(-5, 77));
    }

    @Test
    void activeCooldownIsNotReadyAndValuesAreClamped() {
        assertFalse(BlinkCooldownHudState.isReady(1));
        assertFalse(BlinkCooldownHudState.isReady(80));
        assertEquals(80, BlinkCooldownHudState.clampedCooldown(200));
        assertEquals(0, BlinkCooldownHudState.filledPixels(200, 80));
        assertEquals(0, BlinkCooldownHudState.filledPixels(20, 0));
    }

    @Test
    void interpolatesFillColorFromPurpleToTurquoise() {
        assertEquals(0xFF5A287D, BlinkCooldownHudState.fillColor(80));
        assertEquals(0xFF3E7EA0, BlinkCooldownHudState.fillColor(40));
        assertEquals(0xFF21D4C3, BlinkCooldownHudState.fillColor(0));
    }

    @Test
    void interpolatesBorderColorWithoutWhiteReadyFlash() {
        assertEquals(0xFF351445, BlinkCooldownHudState.borderColor(80));
        assertEquals(0xFF205267, BlinkCooldownHudState.borderColor(40));
        assertEquals(0xFF0B8F88, BlinkCooldownHudState.borderColor(0));
    }

    @Test
    void clampsColorInterpolationOutsideCooldownRange() {
        assertEquals(0xFF5A287D, BlinkCooldownHudState.fillColor(200));
        assertEquals(0xFF21D4C3, BlinkCooldownHudState.fillColor(-1));
        assertEquals(0xFF351445, BlinkCooldownHudState.borderColor(200));
        assertEquals(0xFF0B8F88, BlinkCooldownHudState.borderColor(-1));
    }
}
