package pl.fadedpearl.entity.behavior;

import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;

import java.util.Optional;
import java.util.Set;

/** Pure selection and short deferral policy for the five one-time End memories. */
public final class FadedEndEchoPolicy {
    public static final double MAX_SHARED_DISTANCE_SQR = 144.0D;
    public static final int MAX_PENDING_TICKS = 200;

    public enum DragonState {
        UNAVAILABLE,
        LIVE_FIRST_FIGHT,
        PREVIOUSLY_DEFEATED
    }

    public enum Reaction {
        END_PORTAL,
        END_ARRIVAL_LIVE,
        END_ARRIVAL_DEFEATED,
        DRAGON_DEFEATED,
        DRAGON_EGG,
        END_RETURN
    }

    public enum PendingDecision {
        WAIT,
        CANCEL,
        COMPLETE
    }

    public record Observation(
            boolean visibleEndThreshold,
            boolean togetherInEnd,
            DragonState dragonState,
            boolean dragonEggObserved,
            boolean togetherInOverworld
    ) {
        public Observation {
            if (dragonState == null) throw new IllegalArgumentException("Dragon state cannot be null");
        }
    }

    public static Optional<Reaction> select(
            Observation observation,
            Set<WorldAwarenessMemory.Milestone> seen
    ) {
        Set<WorldAwarenessMemory.Milestone> safeSeen = seen == null ? Set.of() : seen;
        if (observation.visibleEndThreshold() && observation.togetherInOverworld()
                && !safeSeen.contains(WorldAwarenessMemory.Milestone.END_PORTAL))
            return Optional.of(Reaction.END_PORTAL);
        if (observation.togetherInEnd()
                && !safeSeen.contains(WorldAwarenessMemory.Milestone.END_ARRIVAL)) {
            if (safeSeen.contains(WorldAwarenessMemory.Milestone.END_DRAGON_LIVE_WITNESSED)
                    || observation.dragonState() == DragonState.LIVE_FIRST_FIGHT)
                return Optional.of(Reaction.END_ARRIVAL_LIVE);
            if (observation.dragonState() == DragonState.PREVIOUSLY_DEFEATED)
                return Optional.of(Reaction.END_ARRIVAL_DEFEATED);
        }
        if (observation.togetherInEnd()
                && observation.dragonState() == DragonState.PREVIOUSLY_DEFEATED
                && safeSeen.contains(WorldAwarenessMemory.Milestone.END_DRAGON_LIVE_WITNESSED)
                && !safeSeen.contains(WorldAwarenessMemory.Milestone.DRAGON_DEFEATED))
            return Optional.of(Reaction.DRAGON_DEFEATED);
        if (observation.dragonEggObserved()
                && !safeSeen.contains(WorldAwarenessMemory.Milestone.DRAGON_EGG))
            return Optional.of(Reaction.DRAGON_EGG);
        if (observation.togetherInOverworld()
                && safeSeen.contains(WorldAwarenessMemory.Milestone.END_ARRIVAL)
                && !safeSeen.contains(WorldAwarenessMemory.Milestone.END_RETURN))
            return Optional.of(Reaction.END_RETURN);
        return Optional.empty();
    }

    public static boolean shouldScanEndPortal(
            Set<WorldAwarenessMemory.Milestone> seen,
            boolean sharedContact,
            boolean inOverworld,
            int tickCount
    ) {
        Set<WorldAwarenessMemory.Milestone> safeSeen = seen == null ? Set.of() : seen;
        return !safeSeen.contains(WorldAwarenessMemory.Milestone.END_PORTAL)
                && sharedContact && inOverworld && tickCount % 20 == 0;
    }

    public static boolean contextPresent(
            Reaction reaction,
            Observation observation,
            Set<WorldAwarenessMemory.Milestone> seen
    ) {
        if (reaction == null || observation == null) return false;
        Set<WorldAwarenessMemory.Milestone> safeSeen = seen == null ? Set.of() : seen;
        return switch (reaction) {
            case END_PORTAL -> observation.togetherInOverworld();
            case END_ARRIVAL_LIVE, END_ARRIVAL_DEFEATED -> observation.togetherInEnd();
            case DRAGON_DEFEATED -> observation.togetherInEnd()
                    && observation.dragonState() == DragonState.PREVIOUSLY_DEFEATED
                    && safeSeen.contains(WorldAwarenessMemory.Milestone.END_DRAGON_LIVE_WITNESSED);
            case DRAGON_EGG -> observation.dragonEggObserved();
            case END_RETURN -> observation.togetherInOverworld()
                    && safeSeen.contains(WorldAwarenessMemory.Milestone.END_ARRIVAL);
        };
    }

    public static boolean stillRelevant(
            Reaction reaction,
            Observation observation,
            Set<WorldAwarenessMemory.Milestone> seen
    ) {
        return contextPresent(reaction, observation, seen)
                && (reaction != Reaction.END_PORTAL || observation.visibleEndThreshold());
    }

    public static WorldAwarenessMemory.Milestone milestone(Reaction reaction) {
        if (reaction == null) throw new IllegalArgumentException("End reaction cannot be null");
        return switch (reaction) {
            case END_PORTAL -> WorldAwarenessMemory.Milestone.END_PORTAL;
            case END_ARRIVAL_LIVE, END_ARRIVAL_DEFEATED -> WorldAwarenessMemory.Milestone.END_ARRIVAL;
            case DRAGON_DEFEATED -> WorldAwarenessMemory.Milestone.DRAGON_DEFEATED;
            case DRAGON_EGG -> WorldAwarenessMemory.Milestone.DRAGON_EGG;
            case END_RETURN -> WorldAwarenessMemory.Milestone.END_RETURN;
        };
    }

    public static PendingDecision advancePending(int pendingTicks, boolean safe) {
        if (pendingTicks >= MAX_PENDING_TICKS) return PendingDecision.CANCEL;
        return safe ? PendingDecision.COMPLETE : PendingDecision.WAIT;
    }

    private FadedEndEchoPolicy() {}
}
