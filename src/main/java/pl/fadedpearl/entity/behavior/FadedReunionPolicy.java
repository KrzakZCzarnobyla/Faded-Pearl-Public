package pl.fadedpearl.entity.behavior;

/** Pure timing and trust-tier policy for a reunion after a long absence. */
public final class FadedReunionPolicy {
    public static final long MIN_ABSENCE_TICKS = 24_000L;
    public static final double MAX_CONTACT_DISTANCE_SQR = 144.0D;
    public static final int MAX_PENDING_TICKS = 200;
    public static final int BONDED_MIN_TRUST = 60;

    public enum ContactDecision {
        NONE,
        INITIALIZE,
        REFRESH,
        ARM_REUNION
    }

    public enum CompletionDecision {
        NONE,
        WAITING,
        CANCELLED,
        COMPLETE_LEARNING,
        COMPLETE_BONDED
    }

    public static ContactDecision observeContact(
            long lastContactGameTime,
            long currentGameTime,
            boolean inContact
    ) {
        if (!inContact) return ContactDecision.NONE;
        if (lastContactGameTime <= 0L) return ContactDecision.INITIALIZE;
        if (currentGameTime - lastContactGameTime > MIN_ABSENCE_TICKS)
            return ContactDecision.ARM_REUNION;
        return ContactDecision.REFRESH;
    }

    public static CompletionDecision advance(
            boolean pending,
            int pendingTicks,
            boolean inContact,
            boolean safe,
            int trust
    ) {
        if (!pending) return CompletionDecision.NONE;
        if (pendingTicks >= MAX_PENDING_TICKS) return CompletionDecision.CANCELLED;
        if (!inContact || !safe) return CompletionDecision.WAITING;
        return trust >= BONDED_MIN_TRUST
                ? CompletionDecision.COMPLETE_BONDED
                : CompletionDecision.COMPLETE_LEARNING;
    }

    private FadedReunionPolicy() {}
}
