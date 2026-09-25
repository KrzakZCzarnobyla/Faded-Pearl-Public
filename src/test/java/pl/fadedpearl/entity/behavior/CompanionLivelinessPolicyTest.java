package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionLivelinessPolicyTest {
    @Test
    void socialMoveStartsAtSixtyOnlyInCalmFollow() {
        assertFalse(CompanionLivelinessPolicy.canSocialMove(calm(59)));
        assertTrue(CompanionLivelinessPolicy.canSocialMove(calm(60)));
        assertFalse(CompanionLivelinessPolicy.canSocialMove(new CompanionLivelinessPolicy.SocialMoveSnapshot(
                60, false, true, true, true, false, false, false, false, false,
                false, false, false, false)));
    }

    @Test
    void everyHigherPriorityOrUnsafeStateBlocksSocialMove() {
        assertAll(
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(0))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(1))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(2))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(3))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(4))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(5))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(6))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(7))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(8))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(9))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(10))),
                () -> assertFalse(CompanionLivelinessPolicy.canSocialMove(blocked(11))));
    }

    @Test
    void targetRingAndRareCooldownAreBounded() {
        assertFalse(CompanionLivelinessPolicy.isInSocialRing(8.99D));
        assertTrue(CompanionLivelinessPolicy.isInSocialRing(9.0D));
        assertTrue(CompanionLivelinessPolicy.isInSocialRing(49.0D));
        assertFalse(CompanionLivelinessPolicy.isInSocialRing(49.01D));
        assertEquals(1200, CompanionLivelinessPolicy.socialCooldown(0));
        assertEquals(2400, CompanionLivelinessPolicy.socialCooldown(1200));
        assertThrows(IllegalArgumentException.class,
                () -> CompanionLivelinessPolicy.socialCooldown(1201));
    }

    private static CompanionLivelinessPolicy.SocialMoveSnapshot calm(int trust) {
        return new CompanionLivelinessPolicy.SocialMoveSnapshot(trust, true, true, true, true,
                false, false, false, false, false, false, false, false, false);
    }

    private static CompanionLivelinessPolicy.SocialMoveSnapshot blocked(int index) {
        boolean[] blocked = new boolean[12];
        blocked[index] = true;
        return new CompanionLivelinessPolicy.SocialMoveSnapshot(60, true, !blocked[0], !blocked[1], !blocked[2],
                blocked[3], blocked[4], blocked[5], blocked[6], blocked[7], blocked[8], blocked[9],
                blocked[10], blocked[11]);
    }
}
