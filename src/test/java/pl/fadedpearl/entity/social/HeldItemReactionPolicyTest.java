package pl.fadedpearl.entity.social;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HeldItemReactionPolicyTest {
    @Test
    void sameHeldItemCannotRepeatBeforeNineHundredTicks() {
        int cooldown = HeldItemReactionPolicy.startCooldown();
        for (int tick = 1; tick < HeldItemReactionPolicy.COOLDOWN_TICKS; tick++) {
            cooldown = HeldItemReactionPolicy.tickCooldown(cooldown);
            assertFalse(HeldItemReactionPolicy.canReact(true, false, tick, cooldown));
        }
        cooldown = HeldItemReactionPolicy.tickCooldown(cooldown);
        assertTrue(HeldItemReactionPolicy.canReact(true, false,
                HeldItemReactionPolicy.COOLDOWN_TICKS, cooldown));
    }

    @Test
    void changingItemDoesNotBypassGlobalCooldown() {
        int cooldown = HeldItemReactionPolicy.startCooldown();
        cooldown = HeldItemReactionPolicy.tickCooldown(cooldown);
        assertFalse(HeldItemReactionPolicy.canReact(true, true, 1, cooldown));
    }

    @Test
    void emptyHandsNeverTriggerGesture() {
        assertFalse(HeldItemReactionPolicy.canReact(false, true, 0, 0));
    }
}
