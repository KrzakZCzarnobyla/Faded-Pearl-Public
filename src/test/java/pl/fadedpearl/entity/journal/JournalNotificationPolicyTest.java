package pl.fadedpearl.entity.journal;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class JournalNotificationPolicyTest {
    @Test
    void onlyNewCatalogEntriesTriggerNotification() {
        EndermanJournalSnapshot empty = snapshot(Set.of(), Set.of(), "", 0, false);
        EndermanJournalSnapshot meeting = snapshot(Set.of(EndermanJournalSnapshot.DayEntry.MEETING), Set.of(), "", 0, false);
        EndermanJournalSnapshot named = snapshot(meeting.days(), Set.of(EndermanJournalSnapshot.BehaviorEntry.OWN_NAME), "Lumen", 0, false);
        EndermanJournalSnapshot fade = snapshot(named.days(), named.behaviors(), "Lumen", 0, true);
        assertAll(
                () -> assertTrue(JournalNotificationPolicy.hasNewEntry(empty, meeting)),
                () -> assertTrue(JournalNotificationPolicy.hasNewEntry(meeting, named)),
                () -> assertTrue(JournalNotificationPolicy.hasNewEntry(named, fade)),
                () -> assertFalse(JournalNotificationPolicy.hasNewEntry(fade, fade)),
                () -> assertFalse(JournalNotificationPolicy.hasNewEntry(fade,
                        snapshot(fade.days(), fade.behaviors(), "Nova", 0x3366CC, true))),
                () -> assertFalse(JournalNotificationPolicy.hasNewEntry(fade,
                        new EndermanJournalSnapshot(fade.days(), fade.behaviors(), "Lumen", 0, true, 85))),
                () -> assertFalse(JournalNotificationPolicy.hasNewEntry(fade, meeting)),
                () -> assertFalse(JournalNotificationPolicy.hasNewEntry(null, fade)));
    }

    private static EndermanJournalSnapshot snapshot(Set<EndermanJournalSnapshot.DayEntry> days,
                                                     Set<EndermanJournalSnapshot.BehaviorEntry> behaviors,
                                                     String name, int color, boolean fade) {
        return new EndermanJournalSnapshot(days, behaviors, name, color, fade, 35);
    }
}
