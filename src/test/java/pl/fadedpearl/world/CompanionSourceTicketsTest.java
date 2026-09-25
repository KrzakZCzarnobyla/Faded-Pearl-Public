package pl.fadedpearl.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CompanionSourceTicketsTest {
    @Test
    void sharedTicketIsCreatedOnceAndRemovedAfterTheLastUser() {
        CompanionSourceTickets.ReferenceCounter<String> counter =
                new CompanionSourceTickets.ReferenceCounter<>();

        assertTrue(counter.acquire("same-source"));
        assertFalse(counter.acquire("same-source"));
        assertEquals(2, counter.users("same-source"));

        assertFalse(counter.release("same-source"));
        assertEquals(1, counter.users("same-source"));
        assertTrue(counter.release("same-source"));
        assertEquals(0, counter.users("same-source"));
    }

    @Test
    void independentSourcesAndRepeatedReleaseRemainIsolatedAndIdempotent() {
        CompanionSourceTickets.ReferenceCounter<String> counter =
                new CompanionSourceTickets.ReferenceCounter<>();

        assertTrue(counter.acquire("first"));
        assertTrue(counter.acquire("second"));
        assertTrue(counter.release("first"));
        assertFalse(counter.release("first"));
        assertEquals(0, counter.users("first"));
        assertEquals(1, counter.users("second"));
        assertTrue(counter.release("second"));
    }
}
