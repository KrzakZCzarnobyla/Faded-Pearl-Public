package pl.fadedpearl.client;

import pl.fadedpearl.entity.trust.FadedTrustManager;

/** Pure render values for the eight existing trust stages. */
public final class FadedTrustVisuals {
    private static final float[] FLOWER_LIGHT = {
            0.62F, 0.68F, 0.74F, 0.80F, 0.86F, 0.91F, 0.95F, 0.98F
    };

    public static float flowerLight(int trust) {
        return FLOWER_LIGHT[FadedTrustManager.stageIndex(trust)];
    }

    public static float colorChannel(int channel, int trust) {
        if (channel < 0 || channel > 255) throw new IllegalArgumentException("Colour channel must be 0..255");
        float base = channel / 255.0F;
        float light = flowerLight(trust);
        return Math.min(1.0F, base * light + (1.0F - light) * 0.08F);
    }

    private FadedTrustVisuals() {}
}
