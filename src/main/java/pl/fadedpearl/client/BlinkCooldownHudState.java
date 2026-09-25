package pl.fadedpearl.client;

public final class BlinkCooldownHudState {
    public static final int TOTAL_TICKS = 80;
    public static final int FILL_START = 0xFF5A287D;
    public static final int FILL_READY = 0xFF21D4C3;
    public static final int BORDER_START = 0xFF351445;
    public static final int BORDER_READY = 0xFF0B8F88;

    public static int clampedCooldown(int cooldownTicks) {
        return Math.max(0, Math.min(TOTAL_TICKS, cooldownTicks));
    }

    public static int filledPixels(int cooldownTicks, int width) {
        if (width <= 0) return 0;
        int elapsedTicks = TOTAL_TICKS - clampedCooldown(cooldownTicks);
        return elapsedTicks * width / TOTAL_TICKS;
    }

    public static boolean isReady(int cooldownTicks) {
        return clampedCooldown(cooldownTicks) == 0;
    }

    public static int fillColor(int cooldownTicks) {
        return interpolateArgb(cooldownTicks, FILL_START, FILL_READY);
    }

    public static int borderColor(int cooldownTicks) {
        return interpolateArgb(cooldownTicks, BORDER_START, BORDER_READY);
    }

    static int interpolateArgb(int cooldownTicks, int start, int end) {
        int elapsedTicks = TOTAL_TICKS - clampedCooldown(cooldownTicks);
        int alpha = interpolateChannel(start >>> 24, end >>> 24, elapsedTicks);
        int red = interpolateChannel(start >>> 16 & 0xFF, end >>> 16 & 0xFF, elapsedTicks);
        int green = interpolateChannel(start >>> 8 & 0xFF, end >>> 8 & 0xFF, elapsedTicks);
        int blue = interpolateChannel(start & 0xFF, end & 0xFF, elapsedTicks);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int interpolateChannel(int start, int end, int elapsedTicks) {
        return (start * (TOTAL_TICKS - elapsedTicks) + end * elapsedTicks + TOTAL_TICKS / 2) / TOTAL_TICKS;
    }

    private BlinkCooldownHudState() {
    }
}
