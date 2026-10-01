package pl.fadedpearl.entity.behavior;

import java.util.Objects;

/** Pure, transient state machine for one sleeping owner's night-watch session. */
public final class FadedNightWatchPolicy {
    public static final int MIN_WATCH_TICKS = 100;

    public enum Phase {
        IDLE,
        WATCHING_SESSION,
        ABORTED_SESSION
    }

    public enum CompletionDecision {
        NONE,
        WATCH_COMPLETED
    }

    public record State(Phase phase, int watchTicks) {
        private static final State INITIAL = new State(Phase.IDLE, 0);

        public State {
            Objects.requireNonNull(phase, "phase");
            if (watchTicks < 0) {
                throw new IllegalArgumentException("Night-watch ticks cannot be negative: " + watchTicks);
            }
            if (phase == Phase.IDLE && watchTicks != 0) {
                throw new IllegalArgumentException("Idle night-watch state must have zero ticks");
            }
        }

        public static State initial() {
            return INITIAL;
        }
    }

    public record Snapshot(
            boolean sleeping,
            boolean watching,
            boolean hardBlocked,
            boolean canReact
    ) {}

    public record Transition(State state, CompletionDecision decision) {
        public Transition {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(decision, "decision");
        }
    }

    /**
     * Advances one server tick. Approach and other non-watching pauses preserve the current
     * session, while any hard blocker permanently aborts it until the owner wakes.
     */
    public static Transition advance(State state, Snapshot snapshot) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(snapshot, "snapshot");

        if (!snapshot.sleeping()) {
            boolean completed = state.phase() == Phase.WATCHING_SESSION
                    && state.watchTicks() >= MIN_WATCH_TICKS
                    && snapshot.canReact();
            return transition(State.initial(), completed
                    ? CompletionDecision.WATCH_COMPLETED
                    : CompletionDecision.NONE);
        }

        if (state.phase() == Phase.ABORTED_SESSION || snapshot.hardBlocked()) {
            return transition(new State(Phase.ABORTED_SESSION, state.watchTicks()),
                    CompletionDecision.NONE);
        }

        int watchTicks = state.watchTicks();
        if (snapshot.watching() && watchTicks < Integer.MAX_VALUE) {
            watchTicks++;
        }
        return transition(new State(Phase.WATCHING_SESSION, watchTicks), CompletionDecision.NONE);
    }

    private static Transition transition(State state, CompletionDecision decision) {
        return new Transition(state, decision);
    }

    private FadedNightWatchPolicy() {}
}
