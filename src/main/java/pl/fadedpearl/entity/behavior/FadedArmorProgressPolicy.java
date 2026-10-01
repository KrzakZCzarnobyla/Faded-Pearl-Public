package pl.fadedpearl.entity.behavior;

/** Pure helpers for recognizing a real increase over the best armor value seen this session. */
public final class FadedArmorProgressPolicy {
    public static boolean isUpgrade(int observedMaximum, int currentArmor) {
        return observedMaximum > 0 && currentArmor > observedMaximum;
    }

    public static int nextMaximum(int observedMaximum, int currentArmor) {
        return Math.max(Math.max(0, observedMaximum), Math.max(0, currentArmor));
    }

    private FadedArmorProgressPolicy() {}
}
