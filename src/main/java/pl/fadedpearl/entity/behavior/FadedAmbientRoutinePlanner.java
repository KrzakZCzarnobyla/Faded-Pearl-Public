package pl.fadedpearl.entity.behavior;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Pure selection policy for calm companion routines. All world reads and effects stay outside. */
public final class FadedAmbientRoutinePlanner {
    public static final int SHARED_GAZE_MIN_TRUST = 12;
    public static final int REST_NEAR_MIN_TRUST = 35;
    public static final int REST_NEAR_CALM_TICKS = 160;
    public static final double REST_NEAR_MAX_DISTANCE_SQR = 9.0D;
    public static final int SOCIAL_REPOSITION_MIN_TRUST = 60;
    public static final int HOME_SETTLE_CALM_TICKS = 160;
    public static final double HOME_SETTLE_MAX_DISTANCE_SQR = 64.0D;

    public enum Routine {
        NONE,
        SHARED_GAZE,
        REST_NEAR,
        SOCIAL_REPOSITION,
        HOME_SETTLE
    }

    public enum Command {
        FOLLOW,
        STAY,
        REST,
        HOME
    }

    public record Decision(Routine routine) {
        public Decision {
            Objects.requireNonNull(routine, "routine");
        }
    }

    public record Snapshot(
            int trust,
            Command command,
            boolean healed,
            boolean livingFriendNearby,
            boolean friendStationary,
            boolean cooldownReady,
            boolean downed,
            boolean healing,
            boolean playerRescueActive,
            boolean recoveryActive,
            boolean combatActive,
            boolean threatPresent,
            boolean unsafeEnvironment,
            boolean carryingPlayer,
            boolean carryingAnimal,
            boolean playerInteractionActive,
            boolean itemCuriosityActive,
            boolean worldCuriosityActive,
            boolean socialActionActive,
            boolean fadeMeetingActive,
            boolean routineActive,
            boolean sharedGazeAvailable,
            boolean restNearAvailable,
            boolean socialRepositionAvailable,
            boolean hasHome,
            boolean sameHomeDimension,
            boolean nearHome,
            boolean homeSettleAvailable
    ) {
        public Snapshot {
            Objects.requireNonNull(command, "command");
        }
    }

    /**
     * Selects at most one routine. The supplier must return a bounded choice in {@code 0..1},
     * and is called exactly once only when both non-contextual candidates are eligible.
     */
    public static Decision plan(Snapshot state, IntSupplier boundedChoiceSupplier) {
        Objects.requireNonNull(state, "state");
        if (isCommonBlocked(state)) return decision(Routine.NONE);

        if (state.command() == Command.HOME) {
            return state.hasHome() && state.sameHomeDimension() && state.nearHome()
                    && state.homeSettleAvailable()
                    ? decision(Routine.HOME_SETTLE)
                    : decision(Routine.NONE);
        }

        if (isFollowBlocked(state)) return decision(Routine.NONE);

        if (state.sharedGazeAvailable() && state.trust() >= SHARED_GAZE_MIN_TRUST)
            return decision(Routine.SHARED_GAZE);

        boolean restNear = state.restNearAvailable() && state.trust() >= REST_NEAR_MIN_TRUST;
        boolean socialReposition = state.socialRepositionAvailable()
                && state.trust() >= SOCIAL_REPOSITION_MIN_TRUST;

        if (restNear && socialReposition) {
            int choice = Objects.requireNonNull(boundedChoiceSupplier, "boundedChoiceSupplier").getAsInt();
            return switch (choice) {
                case 0 -> decision(Routine.REST_NEAR);
                case 1 -> decision(Routine.SOCIAL_REPOSITION);
                default -> throw new IllegalArgumentException("Routine choice must be in 0..1: " + choice);
            };
        }
        if (restNear) return decision(Routine.REST_NEAR);
        if (socialReposition) return decision(Routine.SOCIAL_REPOSITION);
        return decision(Routine.NONE);
    }

    public static boolean canRestNear(int calmTicks, double distanceSqr) {
        return calmTicks >= REST_NEAR_CALM_TICKS && Double.isFinite(distanceSqr)
                && distanceSqr <= REST_NEAR_MAX_DISTANCE_SQR;
    }

    public static boolean canAccumulateRestNear(int trust, double distanceSqr) {
        return trust >= REST_NEAR_MIN_TRUST && Double.isFinite(distanceSqr)
                && distanceSqr <= REST_NEAR_MAX_DISTANCE_SQR;
    }

    public static boolean canSettleAtHome(int calmTicks, double distanceSqr) {
        return calmTicks >= HOME_SETTLE_CALM_TICKS && Double.isFinite(distanceSqr)
                && distanceSqr <= HOME_SETTLE_MAX_DISTANCE_SQR;
    }

    private static boolean isCommonBlocked(Snapshot state) {
        return !state.healed()
                || !state.cooldownReady()
                || state.downed()
                || state.healing()
                || state.playerRescueActive()
                || state.recoveryActive()
                || state.combatActive()
                || state.threatPresent()
                || state.unsafeEnvironment()
                || state.carryingPlayer()
                || state.carryingAnimal()
                || state.playerInteractionActive()
                || state.itemCuriosityActive()
                || state.worldCuriosityActive()
                || state.socialActionActive()
                || state.fadeMeetingActive()
                || state.routineActive();
    }

    private static boolean isFollowBlocked(Snapshot state) {
        return state.command() != Command.FOLLOW
                || !state.livingFriendNearby()
                || !state.friendStationary();
    }

    private static Decision decision(Routine routine) {
        return new Decision(routine);
    }

    private FadedAmbientRoutinePlanner() {}
}
