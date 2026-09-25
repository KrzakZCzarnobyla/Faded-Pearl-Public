package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CombatBlinkPolicyTest {
    @Test
    void onlyShortUnladenChaseCanBlink() {
        assertTrue(CombatBlinkPolicy.canAttempt(0, false, true, 16.0D));
        assertTrue(CombatBlinkPolicy.canAttempt(0, false, true, 100.0D));
        assertFalse(CombatBlinkPolicy.canAttempt(1, false, true, 16.0D));
        assertFalse(CombatBlinkPolicy.canAttempt(0, true, true, 16.0D));
        assertFalse(CombatBlinkPolicy.canAttempt(0, false, false, 16.0D));
        assertFalse(CombatBlinkPolicy.canAttempt(0, false, true, 15.9D));
        assertFalse(CombatBlinkPolicy.canAttempt(0, false, true, 100.1D));
    }
}
