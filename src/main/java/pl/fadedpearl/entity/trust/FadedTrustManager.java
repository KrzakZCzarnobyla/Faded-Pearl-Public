package pl.fadedpearl.entity.trust;

/** Pure trust thresholds and modifiers. World time and persistence remain owned by the entity. */
public final class FadedTrustManager {
    public static final int MIN_TRUST = 0;
    public static final int MAX_TRUST = 100;

    public static final int STAGE_CAUTION = 6;
    public static final int STAGE_CURIOSITY = 12;
    public static final int STAGE_ACCEPTANCE = 25;
    public static final int STAGE_BOND = 35;
    public static final int STAGE_TRUST = 60;
    public static final int STAGE_PARTNER = 75;
    public static final int STAGE_DEVOTION = 85;

    public static final int TOUCH_LEARNING_LIMIT = 40;
    public static final int WORLD_LEARNING_LIMIT = 30;

    public static final int FLOWER_ACCEPTED = 1;
    public static final int CARRY_STARTED = 2;
    public static final int PLAYER_ATTACK = -8;
    public static final int PERSISTENT_STARE = -1;
    public static final int RAIN_EXPOSURE = -3;
    public static final int SHARED_SHELTER = 2;
    public static final int ENTERED_WATER = -10;
    public static final int NEW_LIGHT = 1;
    public static final int RETURN_AFTER_ABSENCE = 3;
    public static final int PROXIMITY = 1;
    public static final int FLOWER_GIFT = 1;
    public static final int TOUCH = 1;
    public static final int FALL_RESCUE = 2;
    public static final int LAVA_RESCUE = 3;
    public static final int LOW_HEALTH_RESCUE = 2;
    public static final int ANCHOR_RECALL = 1;

    private FadedTrustManager() {}

    public static int applyModifier(int trust, int modifier) {
        return Math.max(MIN_TRUST, Math.min(MAX_TRUST, trust + modifier));
    }

    public static boolean isPositiveModifier(int modifier) { return modifier > 0; }

    public static int scaleModifier(int modifier, int gainPercent, int lossPercent) {
        if (modifier == 0) return 0;
        int percent = modifier > 0 ? gainPercent : lossPercent;
        if (percent <= 0) return 0;
        int magnitude = Math.max(1, Math.round(Math.abs(modifier) * percent / 100.0F));
        return modifier > 0 ? magnitude : -magnitude;
    }

    public static int stageIndex(int trust) {
        if (trust < STAGE_CAUTION) return 0;
        if (trust < STAGE_CURIOSITY) return 1;
        if (trust < STAGE_ACCEPTANCE) return 2;
        if (trust < STAGE_BOND) return 3;
        if (trust < STAGE_TRUST) return 4;
        if (trust < STAGE_PARTNER) return 5;
        if (trust < STAGE_DEVOTION) return 6;
        return 7;
    }

    public static boolean canCarry(int trust) { return trust >= STAGE_PARTNER; }
    public static boolean usesLowGestureResponse(int trust) { return trust < STAGE_BOND; }
    public static boolean isWorldCautious(int trust) { return trust < STAGE_CURIOSITY; }
    public static boolean isWorldLearning(int trust) {
        return trust >= STAGE_CURIOSITY && trust < WORLD_LEARNING_LIMIT;
    }
    public static boolean allowsTouchWound(int trust) { return trust >= STAGE_TRUST; }
    public static boolean allowsAffection(int trust) { return trust >= STAGE_BOND; }
    public static boolean usesTouchRecoil(int trust) { return trust < STAGE_CAUTION; }
    public static boolean usesTouchHesitation(int trust) {
        return trust >= STAGE_CAUTION && trust < STAGE_CURIOSITY;
    }
    public static boolean usesTouchLearning(int trust) {
        return trust >= STAGE_CURIOSITY && trust < TOUCH_LEARNING_LIMIT;
    }
    public static boolean usesTouchDevotion(int trust) { return trust >= STAGE_DEVOTION; }
    public static boolean usesTouchChestExpose(int trust) {
        return trust >= STAGE_TRUST && trust < STAGE_DEVOTION;
    }
    public static boolean canRescueFall(int trust) { return trust >= STAGE_BOND; }
    public static boolean canRescueLowHealth(int trust) { return trust >= STAGE_TRUST; }
}
