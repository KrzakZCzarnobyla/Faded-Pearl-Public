package pl.fadedpearl.entity.journal;

/** A notification follows only a newly unlocked entry, never a changed display value. */
public final class JournalNotificationPolicy {
    public static boolean hasNewEntry(EndermanJournalSnapshot before, EndermanJournalSnapshot after) {
        if (before == null || after == null) return false;
        return !before.days().containsAll(after.days())
                || !before.behaviors().containsAll(after.behaviors())
                || !before.fadeOriginKnown() && after.fadeOriginKnown();
    }

    private JournalNotificationPolicy() {}
}
