package pl.fadedpearl.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.fadedpearl.network.EchoNeedlePacket;
import pl.fadedpearl.world.EchoNeedleLocator;

import static org.junit.jupiter.api.Assertions.*;

final class EchoNeedleClientStateTest {
    @AfterEach
    void clear() {
        EchoNeedleClientState.clear();
    }

    @Test
    void activeStateExpiresAfterFiftyTicksAndRejectsClockRollback() {
        EchoNeedleClientState.accept(new EchoNeedlePacket(true, 4, EchoNeedleLocator.Height.BELOW), 100L);
        assertTrue(EchoNeedleClientState.current(150L).isPresent());
        assertTrue(EchoNeedleClientState.current(151L).isEmpty());

        EchoNeedleClientState.accept(new EchoNeedlePacket(true, 4, EchoNeedleLocator.Height.BELOW), 100L);
        assertTrue(EchoNeedleClientState.current(99L).isEmpty());
    }

    @Test
    void explicitClearRemovesStateImmediately() {
        EchoNeedleClientState.accept(new EchoNeedlePacket(true, 4, EchoNeedleLocator.Height.BELOW), 100L);
        EchoNeedleClientState.accept(EchoNeedlePacket.clear(), 101L);
        assertTrue(EchoNeedleClientState.current(101L).isEmpty());
    }
}
