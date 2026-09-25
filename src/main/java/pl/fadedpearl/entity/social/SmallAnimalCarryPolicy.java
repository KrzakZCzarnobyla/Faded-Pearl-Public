package pl.fadedpearl.entity.social;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** Bounded, Minecraft-independent gates for the companion's voluntary animal carry. */
public final class SmallAnimalCarryPolicy {
    public static final int MIN_TRUST = 60;
    public static final int SEARCH_INTERVAL_TICKS = 40;
    public static final int SEARCH_RADIUS = 8;
    public static final int MAX_APPROACH_TICKS = 120;
    public static final int CARRY_MIN_TICKS = 160;
    public static final int CARRY_RANDOM_TICKS = 81;
    public static final int FAILED_ATTEMPT_COOLDOWN_TICKS = 200;
    public static final int GLOBAL_COOLDOWN_MIN_TICKS = 2400;
    public static final int GLOBAL_COOLDOWN_RANDOM_TICKS = 2401;
    public static final int SAME_ANIMAL_COOLDOWN_TICKS = 7200;

    public record Context(boolean healed, boolean downed, int trust, boolean follow,
                          boolean friendNearby, boolean threat, boolean busy,
                          boolean dry, boolean onGround, int cooldown) {}

    public record Candidate(boolean alive, boolean landAnimal, boolean baby,
                            float width, float height, boolean healthy,
                            boolean sitting, boolean leashed, boolean vehicle,
                            boolean passenger, boolean ownerAllowed) {}

    public record Interruption(boolean downed, boolean commandChanged, boolean threat,
                               boolean friendNeedsRescue, boolean unsafeEnvironment,
                               boolean animalHurt, boolean animalMissing,
                               boolean friendAbsent) {}

    public static boolean canStart(Context context) {
        return context.healed() && !context.downed() && context.trust() >= MIN_TRUST
                && context.follow() && context.friendNearby() && !context.threat()
                && !context.busy() && context.dry() && context.onGround()
                && context.cooldown() == 0;
    }

    public static boolean canCarry(Candidate animal) {
        if (!animal.alive() || !animal.landAnimal() || !animal.healthy()
                || animal.sitting() || animal.leashed() || animal.vehicle()
                || animal.passenger() || !animal.ownerAllowed()) return false;
        return animal.baby()
                ? animal.width() <= 1.2F && animal.height() <= 1.4F
                : animal.width() <= 0.9F && animal.height() <= 1.2F;
    }

    public static boolean shouldRelease(Interruption interruption) {
        return interruption.downed() || interruption.commandChanged() || interruption.threat()
                || interruption.friendNeedsRescue() || interruption.unsafeEnvironment()
                || interruption.animalHurt() || interruption.animalMissing()
                || interruption.friendAbsent();
    }

    public static boolean canTryEscape(int elapsedTicks) {
        return elapsedTicks >= 80 && elapsedTicks % 20 == 0;
    }

    public static int nextGlobalCooldown(IntUnaryOperator random) {
        return GLOBAL_COOLDOWN_MIN_TICKS + random.applyAsInt(GLOBAL_COOLDOWN_RANDOM_TICKS);
    }

    public static boolean canSelectAnimal(UUID candidate, UUID lastCarried, int sameAnimalCooldown) {
        return sameAnimalCooldown <= 0 || !candidate.equals(lastCarried);
    }

    private SmallAnimalCarryPolicy() {}
}
