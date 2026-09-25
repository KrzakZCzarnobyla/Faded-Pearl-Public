package pl.fadedpearl.entity.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.fadedpearl.entity.protection.EscapePearlPolicy.Decision.*;

final class EscapePearlPolicyTest {
    @Test
    void onlyAnOwnerAttackOnAPermanentlyProtectedHealedFadeCanTrigger() {
        assertEquals(PROTECT_AND_EVADE, decide(true, true, true, false));
        assertEquals(IGNORE, decide(false, true, true, false));
        assertEquals(IGNORE, decide(true, false, true, false));
        assertEquals(IGNORE, decide(true, true, false, false));
        assertEquals(IGNORE, decide(true, true, true, true));
    }

    private static EscapePearlPolicy.Decision decide(boolean armed, boolean healed,
                                                      boolean ownerAttack, boolean bypass) {
        return EscapePearlPolicy.decide(armed, healed, ownerAttack, bypass);
    }
}
