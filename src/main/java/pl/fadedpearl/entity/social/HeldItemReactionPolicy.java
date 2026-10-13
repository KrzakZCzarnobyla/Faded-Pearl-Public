package pl.fadedpearl.entity.social;

/** Pure timing rules for the ambient reaction to an item held by the owner. */
public final class HeldItemReactionPolicy {
    public static final int COOLDOWN_TICKS = 900;

    public static int tickCooldown(int cooldownTicks) {
        return Math.max(0, cooldownTicks - 1);
    }

    public static boolean canReact(boolean hasHeldItem, boolean itemChanged, int stableTicks,
                                   int cooldownTicks) {
        return hasHeldItem && cooldownTicks <= 0
                && (itemChanged || stableTicks >= COOLDOWN_TICKS);
    }

    public static int startCooldown() {
        return COOLDOWN_TICKS;
    }

    private HeldItemReactionPolicy() {}
}
