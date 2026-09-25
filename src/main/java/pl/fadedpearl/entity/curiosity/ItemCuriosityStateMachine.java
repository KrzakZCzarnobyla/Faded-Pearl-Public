package pl.fadedpearl.entity.curiosity;

import java.util.Objects;

/** Pure lifecycle/ownership rules used by the world adapter in FadedEnderman. */
public final class ItemCuriosityStateMachine {
    public enum Phase { NONE, POINTING_HAND, POINTING_GROUND, INSPECTING, RETURN_PENDING }
    public enum Removal { KILLED, DISCARDED, UNLOADED, DIMENSION_TRANSFER }
    public enum ReturnResult { INVENTORY_ACCEPTED, DROP_ACCEPTED, FAILED }
    public enum OwnershipAction { KEEP, RELEASE, DROP }

    public record State(Phase phase, int ticks, int cooldown, boolean ownsStack) {
        public State {
            Objects.requireNonNull(phase, "phase");
            if (ticks < 0 || cooldown < 0) throw new IllegalArgumentException("negative curiosity counter");
            if (ownsStack && phase != Phase.INSPECTING && phase != Phase.RETURN_PENDING)
                throw new IllegalArgumentException("owned stack requires inspecting or return-pending phase");
        }
    }

    public record Interruption(boolean healing, boolean guard, boolean combat, boolean carry,
                               boolean rescue, boolean downed, boolean rest, boolean friendUnavailable,
                               boolean dimensionMismatch, boolean unsafeEnvironment) {}

    public static State pointHand() { return new State(Phase.POINTING_HAND, 40, 0, false); }
    public static State restoreOwnedStack() { return new State(Phase.RETURN_PENDING, 0, 0, true); }

    public static boolean shouldInterrupt(Interruption input) {
        return input.healing() || input.guard() || input.combat() || input.carry() || input.rescue()
                || input.downed() || input.rest() || input.friendUnavailable()
                || input.dimensionMismatch() || input.unsafeEnvironment();
    }

    public static boolean grantsTrust(boolean alreadyGrantedForItemId, boolean returnSucceeded) {
        return returnSucceeded && !alreadyGrantedForItemId;
    }

    public static int advanceCooldown(int current, boolean startedThisTick) {
        if (current < 0) throw new IllegalArgumentException("negative cooldown");
        return startedThisTick ? current : Math.max(0, current - 1);
    }

    public static State tickPointing(State state, boolean targetStillMatches) {
        if (state.phase() != Phase.POINTING_HAND && state.phase() != Phase.POINTING_GROUND) return state;
        if (!targetStillMatches || state.ticks() <= 1)
            return new State(Phase.NONE, 0, ItemCuriosityPolicy.COOLDOWN_TICKS, false);
        return new State(state.phase(), state.ticks() - 1, state.cooldown(), false);
    }

    public static State acceptHandoff(State state) {
        if (state.phase() != Phase.POINTING_HAND || state.ticks() <= 0)
            throw new IllegalStateException("handoff outside active pointing window");
        return new State(Phase.INSPECTING, ItemCuriosityPolicy.INSPECT_TICKS, 0, true);
    }

    public static State interrupt(State state, boolean friendOnline) {
        if (!state.ownsStack()) return new State(Phase.NONE, 0, ItemCuriosityPolicy.COOLDOWN_TICKS, false);
        return new State(Phase.RETURN_PENDING, 0, state.cooldown(), true);
    }

    public static State afterReturn(State state, ReturnResult result) {
        if (!state.ownsStack()) return state;
        if (result == ReturnResult.FAILED) return new State(Phase.RETURN_PENDING, 0, state.cooldown(), true);
        return new State(Phase.NONE, 0, ItemCuriosityPolicy.COOLDOWN_TICKS, false);
    }

    public static OwnershipAction removalAction(Removal removal, int entityEpoch, int authoritativeEpoch,
                                                boolean ownsStack) {
        if (!ownsStack) return OwnershipAction.RELEASE;
        if (removal == Removal.KILLED && entityEpoch >= authoritativeEpoch) return OwnershipAction.DROP;
        // Unload/transfer keeps NBT ownership. A stale discard must never materialise its snapshot copy.
        return OwnershipAction.KEEP;
    }

    private ItemCuriosityStateMachine() {}
}
