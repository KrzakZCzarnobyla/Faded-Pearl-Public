package pl.fadedpearl.entity.behavior;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Pure gates and timing for optional, non-persistent interest in nearby world objects. */
public final class FadedWorldInterestPolicy {
    public static final int MIN_TRUST = 25;
    public static final int SCAN_CADENCE_TICKS = 100;
    public static final int APPROACH_TIMEOUT_TICKS = 200;
    public static final int INSPECT_DURATION_TICKS = 100;
    public static final int COOLDOWN_MIN_TICKS = 2400;
    public static final int COOLDOWN_MAX_TICKS = 4800;
    public static final int SHORT_FAILURE_COOLDOWN_TICKS = 400;

    public enum Decision {
        NONE,
        INSPECT
    }

    /**
     * A world-independent view of every gate which can prevent a world-interest start.
     * Candidate validation, including visibility and a reachable dry landing, belongs to the
     * caller and is represented only by {@code candidateAvailable}.
     */
    public record Snapshot(
            int trust,
            boolean followCommand,
            boolean healed,
            boolean calm,
            boolean onDrySafeGround,
            boolean livingOwnerWithinRange,
            boolean cooldownReady,
            boolean downed,
            boolean healing,
            boolean rescueActive,
            boolean recoveryActive,
            boolean combatActive,
            boolean threatPresent,
            boolean inWater,
            boolean onFire,
            boolean harmfulRain,
            boolean unsafeEnvironment,
            boolean carryingPlayer,
            boolean carryingAnimal,
            boolean ownerInteractionActive,
            boolean itemCuriosityActive,
            boolean fadeMeetingActive,
            boolean otherRoutineActive,
            boolean socialActionActive,
            boolean higherPriorityMovementIntentActive,
            boolean candidateAvailable
    ) {}

    public static Decision decide(Snapshot state) {
        Objects.requireNonNull(state, "state");
        if (state.trust() < MIN_TRUST
                || !state.followCommand()
                || !state.healed()
                || !state.calm()
                || !state.onDrySafeGround()
                || !state.livingOwnerWithinRange()
                || !state.cooldownReady()
                || state.downed()
                || state.healing()
                || state.rescueActive()
                || state.recoveryActive()
                || state.combatActive()
                || state.threatPresent()
                || state.inWater()
                || state.onFire()
                || state.harmfulRain()
                || state.unsafeEnvironment()
                || state.carryingPlayer()
                || state.carryingAnimal()
                || state.ownerInteractionActive()
                || state.itemCuriosityActive()
                || state.fadeMeetingActive()
                || state.otherRoutineActive()
                || state.socialActionActive()
                || state.higherPriorityMovementIntentActive()
                || !state.candidateAvailable()) {
            return Decision.NONE;
        }
        return Decision.INSPECT;
    }

    /**
     * Rolls the inclusive completion cooldown only for an accepted start. A rejected decision
     * returns zero without reading the supplier, so blocked attempts cannot advance RNG state.
     */
    public static int rollCompletionCooldown(Decision decision, IntSupplier boundedRandomSupplier) {
        Objects.requireNonNull(decision, "decision");
        if (decision != Decision.INSPECT) return 0;

        int boundedRandom = Objects.requireNonNull(boundedRandomSupplier, "boundedRandomSupplier").getAsInt();
        int bound = COOLDOWN_MAX_TICKS - COOLDOWN_MIN_TICKS + 1;
        if (boundedRandom < 0 || boundedRandom >= bound) {
            throw new IllegalArgumentException("World-interest cooldown random value outside bound: " + boundedRandom);
        }
        return COOLDOWN_MIN_TICKS + boundedRandom;
    }

    private FadedWorldInterestPolicy() {}
}
