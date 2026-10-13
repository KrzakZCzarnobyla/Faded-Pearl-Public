package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RespawnFollowTeleportTrackerTest {
    @Test
    void blocksOnlyFirstSixtyTicksAfterRespawn() {
        assertTrue(RespawnFollowTeleportTracker.isBlockedAt(200, 200));
        assertTrue(RespawnFollowTeleportTracker.isBlockedAt(200, 259));
        assertFalse(RespawnFollowTeleportTracker.isBlockedAt(200, 260));
    }

    @Test
    void ordinaryTimeBeforeRespawnMarkerIsNotTreatedAsExpired() {
        assertTrue(RespawnFollowTeleportTracker.isBlockedAt(200, 199));
    }

    @Test
    void arrivalPresentationExpiresInsteadOfMarkingARegularLaterTeleport() {
        assertFalse(RespawnFollowTeleportTracker.isPresentationCurrentAt(200, 259));
        assertTrue(RespawnFollowTeleportTracker.isPresentationCurrentAt(200, 260));
        assertTrue(RespawnFollowTeleportTracker.isPresentationCurrentAt(200, 400));
        assertFalse(RespawnFollowTeleportTracker.isPresentationCurrentAt(200, 401));
    }
}
