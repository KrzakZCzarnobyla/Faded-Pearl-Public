package pl.fadedpearl.entity.movement;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pure movement arbitration. This class deliberately has no Minecraft, Forge,
 * GeckoLib or SmartBrainLib dependencies. World adapters are introduced only
 * in later migration stages.
 */
public final class FadedMovementCoordinator {
    public static final int WATER_BARRIER_STUCK_TICKS = 80;
    public static final int RETRY_COOLDOWN_TICKS = 100;
    public static final int REQUIRED_WATER_EVIDENCE_TICKS = 2;

    public enum Command { FOLLOW, STAY, REST, HOME }

    public enum Locomotion {
        ESCAPE_WATER(1), ESCAPE_FIRE(2),
        RESCUE_FALL(3), RESCUE_FIRE(3), RESCUE_LOW_HEALTH(3),
        IMMOBILE_WOUNDED(4), IMMOBILE_DOWNED(4), IMMOBILE_HEALING(4), IMMOBILE_REST(4),
        PASSENGER_CONTROLLED(5),
        GUARD_INTERPOSE(6), COMBAT_CHASE(6),
        EXTERNAL_INTERACTION_STOP(7), CURIOSITY_APPROACH(7),
        HOME_DIMENSION_TRANSFER(8), HOME_RETURN(8),
        FOLLOW_WATER_PAUSE(9), FOLLOW_WATER_BARRIER_RECOVERY(9),
        FOLLOW_DISTANCE_TELEPORT(9), FOLLOW_DISTANCE_STOP(9), FOLLOW_PATH(9),
        SOCIAL_MOVE(10),
        HOME_WANDER(11), IDLE_EXPLORE(11),
        STOP(12), NONE(13);

        private final int priority;

        Locomotion(int priority) { this.priority = priority; }
        public int priority() { return priority; }
    }

    public enum Look {
        LOCK_SEATED(1), LOOK_HOSTILE(2), LOOK_FRIEND(3),
        LOOK_SOCIAL_TARGET(4), LOOK_WALK_TARGET(5), NONE(6);

        private final int priority;

        Look(int priority) { this.priority = priority; }
        public int priority() { return priority; }
    }

    public enum TargetPolicy {
        SET_HOSTILE(1), CLEAR(2), PRESERVE(3);

        private final int priority;

        TargetPolicy(int priority) { this.priority = priority; }
        public int priority() { return priority; }
    }
    public enum RecoveryOutcome { NONE, SUCCESS, NO_TARGET, ROLLBACK }

    public record CombatSnapshot(
            boolean healed,
            boolean downed,
            boolean healing,
            boolean rest,
            boolean vehicle,
            boolean endermanWet,
            boolean endermanOnFire,
            boolean friendRescueActive,
            boolean protectionTargetAccepted) {}

    public static boolean isCombatAllowed(CombatSnapshot state) {
        Objects.requireNonNull(state, "state");
        return state.healed() && !state.downed() && !state.healing() && !state.rest() && !state.vehicle()
                && !state.endermanWet() && !state.endermanOnFire() && !state.friendRescueActive()
                && state.protectionTargetAccepted();
    }

    public record Snapshot(
            Command command,
            boolean commandChanged,
            boolean hasFriend,
            boolean friendInWater,
            boolean friendDryGroundedOnSolidSupport,
            boolean endermanInWater,
            boolean controllingPassenger,
            boolean measurableProgress,
            boolean friendMoved,
            boolean followTargetChanged,
            boolean pathReachable,
            boolean followMovementRequired,
            boolean waterBarrierEvidence,
            boolean waterEvidenceObservationFresh,
            boolean recoveryLandingSafeAndVisible,
            boolean selfRescueSucceeded,
            RecoveryOutcome recoveryOutcome) {
        public Snapshot {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(recoveryOutcome, "recoveryOutcome");
        }

        public Snapshot(Command command, boolean commandChanged, boolean hasFriend, boolean friendInWater,
                        boolean friendDryGroundedOnSolidSupport, boolean endermanInWater,
                        boolean controllingPassenger, boolean measurableProgress, boolean pathReachable,
                        boolean followMovementRequired, boolean waterBarrierEvidence,
                        boolean waterEvidenceObservationFresh, boolean recoveryLandingSafeAndVisible,
                        boolean selfRescueSucceeded, RecoveryOutcome recoveryOutcome) {
            this(command, commandChanged, hasFriend, friendInWater, friendDryGroundedOnSolidSupport,
                    endermanInWater, controllingPassenger, measurableProgress, false, false, pathReachable,
                    followMovementRequired, waterBarrierEvidence, waterEvidenceObservationFresh,
                    recoveryLandingSafeAndVisible, selfRescueSucceeded, recoveryOutcome);
        }
    }

    /** Transient only: never serialize this record to NBT. */
    public record State(
            boolean waterFollowPaused,
            int noProgressTicks,
            int waterEvidenceTicks,
            int recoveryCooldownTicks,
            boolean selfRescuedInFollowEpisode) {
        public State {
            if (noProgressTicks < 0 || waterEvidenceTicks < 0 || recoveryCooldownTicks < 0) {
                throw new IllegalArgumentException("Movement counters cannot be negative");
            }
        }

        public static State initial() { return new State(false, 0, 0, 0, false); }
    }

    public record LocomotionCandidate(Locomotion type, String targetKey, double speed, String reason) {
        public LocomotionCandidate {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(reason, "reason");
            if (!Double.isFinite(speed) || speed < 0.0D) {
                throw new IllegalArgumentException("Speed must be finite and non-negative");
            }
        }
    }

    public record LookCandidate(Look type, String targetKey, String reason) {
        public LookCandidate {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record TargetCandidate(TargetPolicy policy, String targetKey, String reason) {
        public TargetCandidate {
            Objects.requireNonNull(policy, "policy");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record Candidates(
            List<LocomotionCandidate> locomotion,
            List<LookCandidate> look,
            List<TargetCandidate> target) {
        public Candidates {
            locomotion = List.copyOf(locomotion);
            look = List.copyOf(look);
            target = List.copyOf(target);
        }

        public static Candidates empty() { return new Candidates(List.of(), List.of(), List.of()); }
    }

    public record Decision(
            State nextState,
            LocomotionCandidate locomotion,
            LookCandidate look,
            TargetCandidate targetPolicy) {
        public Decision {
            Objects.requireNonNull(nextState, "nextState");
            Objects.requireNonNull(locomotion, "locomotion");
            Objects.requireNonNull(look, "look");
            Objects.requireNonNull(targetPolicy, "targetPolicy");
        }
    }

    public Decision resolve(Snapshot snapshot, State previous, Candidates candidates) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(candidates, "candidates");

        State next = updateState(snapshot, previous);
        LocomotionCandidate locomotion = candidates.locomotion().stream()
                .filter(candidate -> isLocomotionAllowed(candidate.type(), snapshot, previous, next))
                .min(Comparator.comparingInt(candidate -> candidate.type().priority()))
                .orElseGet(FadedMovementCoordinator::noLocomotion);

        LookCandidate look = selectLook(snapshot, locomotion, candidates.look());
        TargetCandidate target = candidates.target().stream()
                .filter(candidate -> isTargetAllowed(candidate.policy(), snapshot))
                .min(Comparator.comparingInt(candidate -> candidate.policy().priority()))
                .orElseGet(FadedMovementCoordinator::preserveTarget);
        return new Decision(next, locomotion, look, target);
    }

    private static State updateState(Snapshot snapshot, State previous) {
        int cooldown = Math.max(0, previous.recoveryCooldownTicks() - 1);
        if (snapshot.recoveryOutcome() != RecoveryOutcome.NONE || snapshot.selfRescueSucceeded()) {
            cooldown = RETRY_COOLDOWN_TICKS;
        }

        boolean activeFollow = snapshot.command() == Command.FOLLOW && snapshot.hasFriend();
        if (!activeFollow || snapshot.commandChanged()) {
            return new State(false, 0, 0, cooldown, false);
        }

        boolean paused = previous.waterFollowPaused();
        if (snapshot.friendInWater()) {
            paused = true;
        } else if (paused && snapshot.friendDryGroundedOnSolidSupport()) {
            paused = false;
        }

        boolean evidenceLost = snapshot.waterEvidenceObservationFresh() && !snapshot.waterBarrierEvidence();
        boolean resetEpisode = snapshot.friendInWater()
                || snapshot.measurableProgress()
                || snapshot.friendMoved()
                || snapshot.followTargetChanged()
                || snapshot.pathReachable()
                || !snapshot.followMovementRequired()
                || evidenceLost;
        int noProgress = resetEpisode ? 0 : previous.noProgressTicks() + 1;
        int evidence = resetEpisode ? 0 : previous.waterEvidenceTicks();
        if (!resetEpisode && snapshot.waterEvidenceObservationFresh() && snapshot.waterBarrierEvidence()) {
            evidence++;
        }
        boolean selfRescued = previous.selfRescuedInFollowEpisode() || snapshot.selfRescueSucceeded();
        if (snapshot.friendInWater() || snapshot.friendMoved() || snapshot.followTargetChanged()
                || snapshot.pathReachable() || !snapshot.followMovementRequired()) {
            selfRescued = false;
        }
        if (snapshot.recoveryOutcome() == RecoveryOutcome.SUCCESS) {
            noProgress = 0;
            evidence = 0;
            selfRescued = false;
        }
        return new State(paused, noProgress, evidence, cooldown, selfRescued);
    }

    private static boolean isLocomotionAllowed(
            Locomotion type, Snapshot snapshot, State previous, State state) {
        if (type == Locomotion.ESCAPE_WATER) return snapshot.endermanInWater();
        if (type == Locomotion.IMMOBILE_REST) return snapshot.command() == Command.REST;
        if (snapshot.command() == Command.REST
                && (type == Locomotion.GUARD_INTERPOSE || type == Locomotion.COMBAT_CHASE)) return false;
        if (type == Locomotion.PASSENGER_CONTROLLED) return snapshot.controllingPassenger();
        if (state.selfRescuedInFollowEpisode()
                && (type == Locomotion.FOLLOW_PATH || type == Locomotion.SOCIAL_MOVE
                || type == Locomotion.IDLE_EXPLORE || type == Locomotion.GUARD_INTERPOSE
                || type == Locomotion.COMBAT_CHASE)) return false;

        boolean follow = snapshot.command() == Command.FOLLOW && snapshot.hasFriend();
        if (type == Locomotion.CURIOSITY_APPROACH) {
            return (snapshot.command() == Command.FOLLOW || snapshot.command() == Command.STAY)
                    && !snapshot.controllingPassenger();
        }
        if (type == Locomotion.FOLLOW_WATER_PAUSE) return follow && state.waterFollowPaused();
        if (type == Locomotion.FOLLOW_WATER_BARRIER_RECOVERY) {
            return follow
                    && !snapshot.friendInWater()
                    && snapshot.friendDryGroundedOnSolidSupport()
                    && snapshot.followMovementRequired()
                    && snapshot.waterBarrierEvidence()
                    && snapshot.recoveryLandingSafeAndVisible()
                    && state.noProgressTicks() >= WATER_BARRIER_STUCK_TICKS
                    && state.waterEvidenceTicks() >= REQUIRED_WATER_EVIDENCE_TICKS
                    && previous.recoveryCooldownTicks() == 0
                    && snapshot.recoveryOutcome() == RecoveryOutcome.NONE;
        }
        if (type == Locomotion.FOLLOW_DISTANCE_STOP) return follow;
        if (type == Locomotion.FOLLOW_PATH) return follow && !state.waterFollowPaused();
        if (type == Locomotion.FOLLOW_DISTANCE_TELEPORT) return follow && !state.waterFollowPaused();
        if (type == Locomotion.IDLE_EXPLORE) return snapshot.command() == Command.FOLLOW && !snapshot.controllingPassenger();
        if (type == Locomotion.HOME_DIMENSION_TRANSFER
                || type == Locomotion.HOME_RETURN
                || type == Locomotion.HOME_WANDER) return snapshot.command() == Command.HOME;
        if (type == Locomotion.SOCIAL_MOVE) return snapshot.command() != Command.REST && !snapshot.controllingPassenger();
        return true;
    }

    private static boolean isTargetAllowed(TargetPolicy policy, Snapshot snapshot) {
        return snapshot.command() != Command.REST || policy != TargetPolicy.SET_HOSTILE;
    }

    private static LookCandidate selectLook(
            Snapshot snapshot, LocomotionCandidate locomotion, List<LookCandidate> candidates) {
        if (snapshot.command() == Command.REST) {
            return candidates.stream().filter(candidate -> candidate.type() == Look.LOCK_SEATED)
                    .findFirst().orElse(new LookCandidate(Look.LOCK_SEATED, null, "rest lock"));
        }
        if (locomotion.type() == Locomotion.IMMOBILE_DOWNED) {
            return noLook();
        }
        return candidates.stream().min(Comparator.comparingInt(candidate -> candidate.type().priority()))
                .orElseGet(FadedMovementCoordinator::noLook);
    }

    private static LocomotionCandidate noLocomotion() {
        return new LocomotionCandidate(Locomotion.NONE, null, 0.0D, "no eligible locomotion");
    }

    private static LookCandidate noLook() { return new LookCandidate(Look.NONE, null, "no eligible look"); }
    private static TargetCandidate preserveTarget() {
        return new TargetCandidate(TargetPolicy.PRESERVE, null, "no target change");
    }
}
