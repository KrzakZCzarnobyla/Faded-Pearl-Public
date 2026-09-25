package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JournalPageNavigationTest {
    @Test
    void spreadTurnsTwoPagesAtOnceWithoutSkippingTheLastOddPage() {
        assertEquals(0, JournalPageNavigation.maxStartPage(1, true));
        assertEquals(0, JournalPageNavigation.maxStartPage(2, true));
        assertEquals(2, JournalPageNavigation.maxStartPage(3, true));
        assertEquals(2, JournalPageNavigation.maxStartPage(4, true));
        assertEquals(4, JournalPageNavigation.maxStartPage(5, true));
        assertEquals(2, JournalPageNavigation.move(0, 1, 3, true));
        assertEquals(2, JournalPageNavigation.move(2, 1, 3, true));
        assertEquals(0, JournalPageNavigation.move(2, -1, 3, true));
    }

    @Test
    void compactScreenStillTurnsOnePageAtATime() {
        assertEquals(3, JournalPageNavigation.maxStartPage(4, false));
        assertEquals(1, JournalPageNavigation.move(0, 1, 4, false));
        assertEquals(2, JournalPageNavigation.move(1, 1, 4, false));
        assertEquals(3, JournalPageNavigation.move(3, 1, 4, false));
        assertEquals(0, JournalPageNavigation.move(0, -1, 4, false));
    }

    @Test
    void resizeAlignmentAndEmptyFallbackStayInBounds() {
        assertEquals(2, JournalPageNavigation.clampStartPage(3, 5, true));
        assertEquals(3, JournalPageNavigation.clampStartPage(3, 5, false));
        assertEquals(0, JournalPageNavigation.clampStartPage(7, 0, true));
        assertEquals(0, JournalPageNavigation.move(0, 1, 0, true));
    }
}
