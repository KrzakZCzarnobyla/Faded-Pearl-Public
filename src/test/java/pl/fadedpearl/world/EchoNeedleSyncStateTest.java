package pl.fadedpearl.world;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class EchoNeedleSyncStateTest {
    private static final EchoNeedleLocator.Reading NORTH =
            new EchoNeedleLocator.Reading(0, EchoNeedleLocator.Height.SIMILAR);

    @Test
    void sendsOnAppearanceChangeHeartbeatAndClearOnly() {
        EchoNeedleSyncState state = new EchoNeedleSyncState();
        assertTrue(state.update(Optional.empty(), 0L).isEmpty());
        assertTrue(state.update(Optional.of(NORTH), 10L).orElseThrow().active());
        assertTrue(state.update(Optional.of(NORTH), 20L).isEmpty());
        assertTrue(state.update(Optional.of(NORTH), 49L).isEmpty());
        assertTrue(state.update(Optional.of(NORTH), 50L).orElseThrow().active());

        var changed = state.update(Optional.of(new EchoNeedleLocator.Reading(
                1, EchoNeedleLocator.Height.ABOVE)), 60L).orElseThrow();
        assertEquals(1, changed.sector());
        assertEquals(EchoNeedleLocator.Height.ABOVE, changed.height());

        assertFalse(state.update(Optional.empty(), 70L).orElseThrow().active());
        assertTrue(state.update(Optional.empty(), 80L).isEmpty());
    }
}
