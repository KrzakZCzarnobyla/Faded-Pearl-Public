package pl.fadedpearl.entity.behavior;

/** Bounded server-side cadences used to dephase ambient scans across many companions. */
public final class CompanionPerformancePolicy {
    public static final int PROTECTOR_HOSTILE_SCAN_TICKS = 5;
    public static final int AMBIENT_MOB_SCAN_TICKS = 20;
    public static final int CURIOSITY_IDLE_SCAN_TICKS = 20;
    public static final int LIGHT_SCAN_TICKS = 100;
    public static final int NAMED_PET_SCAN_TICKS = 100;
    public static final int FADE_MEETING_MISS_COOLDOWN_TICKS = 100;

    public static boolean isCadenceTick(int tickCount, int entityId, int interval) {
        if (interval <= 0) throw new IllegalArgumentException("interval must be positive");
        return Math.floorMod(tickCount + entityId, interval) == 0;
    }

    private CompanionPerformancePolicy() {}
}
