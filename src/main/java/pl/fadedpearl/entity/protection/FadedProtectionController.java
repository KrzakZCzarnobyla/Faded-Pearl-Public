package pl.fadedpearl.entity.protection;

import pl.fadedpearl.entity.trust.FadedTrustManager;

/** Pure protection priority resolver. World reads and all rescue effects remain in the entity facade. */
public final class FadedProtectionController {
    public static final int FIRE_RESCUE_MIN_HORIZONTAL_DISTANCE = 4;
    public static final int FIRE_RESCUE_MAX_HORIZONTAL_RADIUS = 8;

    public enum RescueIntent {
        NONE,
        FALL,
        LAVA_OR_FIRE,
        LOW_HEALTH
    }

    public record Snapshot(
            boolean friendPresent,
            boolean downed,
            boolean attackerPresent,
            boolean carryingPassenger,
            float friendFallDistance,
            int trust,
            double friendDistanceSquared,
            boolean friendInLava,
            int friendRemainingFireTicks,
            int protectorCooldown,
            float friendHealth,
            float friendMaxHealth
    ) {}

    /** Guard is a non-terminal prelude and can be returned together with a rescue intent. */
    public record Decision(boolean guardAttacker, RescueIntent rescue) {}

    public static Decision resolve(Snapshot state) {
        if (!state.friendPresent() || state.downed()) return none();

        boolean guardAttacker = state.attackerPresent() && !state.carryingPassenger();
        if (shouldRescueFall(state)) return new Decision(guardAttacker, RescueIntent.FALL);
        if (shouldRescueLavaOrFire(state)) return new Decision(guardAttacker, RescueIntent.LAVA_OR_FIRE);
        if (shouldRescueLowHealth(state)) return new Decision(guardAttacker, RescueIntent.LOW_HEALTH);
        return new Decision(guardAttacker, RescueIntent.NONE);
    }

    public static boolean shouldRescueFall(Snapshot state) {
        return state.friendFallDistance() > 6.0F
                && !state.carryingPassenger()
                && FadedTrustManager.canRescueFall(state.trust())
                && state.friendDistanceSquared() < 900.0D;
    }

    public static boolean shouldRescueLavaOrFire(Snapshot state) {
        return (state.friendInLava() || state.friendRemainingFireTicks() > 80)
                && state.protectorCooldown() <= 0;
    }

    public static boolean shouldRescueLowHealth(Snapshot state) {
        return state.friendHealth() <= Math.max(4.0F, state.friendMaxHealth() * .2F)
                && state.protectorCooldown() <= 0
                && FadedTrustManager.canRescueLowHealth(state.trust());
    }

    public static boolean isValidFireRescueLandingOffset(int deltaX, int deltaZ) {
        long horizontalDistanceSquared = (long) deltaX * deltaX + (long) deltaZ * deltaZ;
        long minimumDistanceSquared = (long) FIRE_RESCUE_MIN_HORIZONTAL_DISTANCE
                * FIRE_RESCUE_MIN_HORIZONTAL_DISTANCE;
        return horizontalDistanceSquared >= minimumDistanceSquared
                && Math.abs(deltaX) <= FIRE_RESCUE_MAX_HORIZONTAL_RADIUS
                && Math.abs(deltaZ) <= FIRE_RESCUE_MAX_HORIZONTAL_RADIUS;
    }

    private static Decision none() {
        return new Decision(false, RescueIntent.NONE);
    }

    private FadedProtectionController() {}
}
