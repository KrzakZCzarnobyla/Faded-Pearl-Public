package pl.fadedpearl.world;

/** Prevents stationary anchor use from becoming an infinite trust source. */
public final class AnchorRecallRewardPolicy {
    public static final double MINIMUM_DISTANCE_SQUARED = 64.0D;

    public static boolean shouldReward(double distanceSquared, int configuredReward) {
        return configuredReward > 0 && distanceSquared >= MINIMUM_DISTANCE_SQUARED;
    }

    private AnchorRecallRewardPolicy() {}
}
