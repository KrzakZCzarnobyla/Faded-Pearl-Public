package pl.fadedpearl.entity.behavior;

/** Narrow combat-only reposition gate; landing safety is checked against the live world. */
public final class CombatBlinkPolicy {
    public static final int SUCCESS_COOLDOWN_TICKS = 100;
    public static final int FAILED_COOLDOWN_TICKS = 20;

    public static boolean canAttempt(int cooldown, boolean carrying, boolean sameLevel,
                                     double distanceSquared) {
        return cooldown <= 0 && !carrying && sameLevel
                && distanceSquared >= 16.0D && distanceSquared <= 100.0D;
    }

    private CombatBlinkPolicy() {}
}
