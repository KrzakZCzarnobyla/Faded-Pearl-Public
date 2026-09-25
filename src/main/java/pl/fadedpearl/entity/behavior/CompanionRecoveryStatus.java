package pl.fadedpearl.entity.behavior;

/** Display-only estimates. The entity remains authoritative for downed time and healing. */
public final class CompanionRecoveryStatus {
    public static final int DOWNED_TICKS = 600;
    public static final int HEAL_INTERVAL_TICKS = 80;

    public static int secondsUntilStand(int elapsedTicks) {
        return Math.max(0, (DOWNED_TICKS - Math.max(0, elapsedTicks) + 19) / 20);
    }

    public static int approximateSecondsUntilFull(float health, float maximum) {
        if (maximum <= 0 || health >= maximum) return 0;
        return Math.max(1, (int) Math.ceil((maximum - Math.max(0, health))
                * HEAL_INTERVAL_TICKS / 20.0D));
    }

    private CompanionRecoveryStatus() {}
}
