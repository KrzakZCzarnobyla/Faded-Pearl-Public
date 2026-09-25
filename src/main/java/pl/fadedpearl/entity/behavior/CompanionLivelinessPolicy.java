package pl.fadedpearl.entity.behavior;

/** Pure gates and timing for optional, non-persistent companion liveliness. */
public final class CompanionLivelinessPolicy {
    public static final int SOCIAL_MIN_TRUST = 60;
    public static final double SOCIAL_MIN_DISTANCE = 3.0D;
    public static final double SOCIAL_MAX_DISTANCE = 7.0D;
    public static final int SOCIAL_START_ROLL = 240;
    public static final int SOCIAL_MIN_COOLDOWN = 1200;
    public static final int SOCIAL_COOLDOWN_VARIANCE = 1201;
    public static final int SOCIAL_MOVE_TIMEOUT = 200;

    public record SocialMoveSnapshot(
            int trust,
            boolean followCommand,
            boolean friendClose,
            boolean friendStationary,
            boolean healed,
            boolean downed,
            boolean healing,
            boolean carrying,
            boolean rescue,
            boolean threat,
            boolean unsafeEnvironment,
            boolean curiosity,
            boolean interaction,
            boolean otherSocialAction) {}

    public static boolean canSocialMove(SocialMoveSnapshot state) {
        return state.trust() >= SOCIAL_MIN_TRUST && state.followCommand()
                && state.friendClose() && state.friendStationary() && state.healed()
                && !state.downed() && !state.healing() && !state.carrying()
                && !state.rescue() && !state.threat() && !state.unsafeEnvironment()
                && !state.curiosity() && !state.interaction() && !state.otherSocialAction();
    }

    public static boolean isInSocialRing(double horizontalDistanceSqr) {
        return Double.isFinite(horizontalDistanceSqr)
                && horizontalDistanceSqr >= SOCIAL_MIN_DISTANCE * SOCIAL_MIN_DISTANCE
                && horizontalDistanceSqr <= SOCIAL_MAX_DISTANCE * SOCIAL_MAX_DISTANCE;
    }

    public static int socialCooldown(int boundedRandom) {
        if (boundedRandom < 0 || boundedRandom >= SOCIAL_COOLDOWN_VARIANCE)
            throw new IllegalArgumentException("Social cooldown random value outside bound");
        return SOCIAL_MIN_COOLDOWN + boundedRandom;
    }

    private CompanionLivelinessPolicy() {}
}
