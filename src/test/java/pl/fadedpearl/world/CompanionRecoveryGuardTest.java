package pl.fadedpearl.world;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionRecoveryGuardTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void ownedCompanionRejectsMissingOrForeignFriendBeforeEpochSelection() {
        assertTrue(CompanionRecoveryGuard.matchesOwner(Optional.of(OWNER), Optional.of(OWNER)));
        assertFalse(CompanionRecoveryGuard.matchesOwner(Optional.of(OWNER), Optional.of(FOREIGN)));
        assertFalse(CompanionRecoveryGuard.matchesOwner(Optional.of(OWNER), Optional.empty()));
        assertTrue(CompanionRecoveryGuard.matchesOwner(Optional.empty(), Optional.of(FOREIGN)));
        assertTrue(CompanionRecoveryGuard.matchesOwner(Optional.empty(), Optional.empty()));
    }

    @Test
    void newerEpochWinsAndEqualEpochIsDeferredOnlyForAnActiveTransition() {
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.KEEP_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(5, 4, false));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.REPLACE_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(4, 5, false));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.REPLACE_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(4, 5, true));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.KEEP_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(5, 4, true));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.KEEP_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(5, 5, false));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.DEFER_EQUAL_DURING_TRANSITION,
                CompanionRecoveryGuard.chooseDuplicate(5, 5, true));
    }

    @Test
    void equalEpochCleanupResumesAsSoonAsTransitionGraceEnds() {
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.DEFER_EQUAL_DURING_TRANSITION,
                CompanionRecoveryGuard.chooseDuplicate(8, 8, true));
        assertEquals(CompanionRecoveryGuard.DuplicateDecision.KEEP_CURRENT,
                CompanionRecoveryGuard.chooseDuplicate(8, 8, false));
    }
}
