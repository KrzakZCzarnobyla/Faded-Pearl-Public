package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FadedTrustVisualsTest {
    private static final int[] STAGES = {0, 6, 12, 25, 35, 60, 75, 85};

    @Test
    void allEightStagesIncreaseFlowerLight() {
        for (int index = 1; index < STAGES.length; index++) {
            assertTrue(FadedTrustVisuals.flowerLight(STAGES[index])
                    > FadedTrustVisuals.flowerLight(STAGES[index - 1]));
        }
    }

    @Test
    void flowerTintRetainsItsDominantChannel() {
        float red = FadedTrustVisuals.colorChannel(220, 0);
        float green = FadedTrustVisuals.colorChannel(40, 0);
        assertTrue(red > green);
        assertTrue(FadedTrustVisuals.colorChannel(220, 85) > red);
        assertTrue(FadedTrustVisuals.colorChannel(220, 100) < 1.0F);
        assertTrue(FadedTrustVisuals.colorChannel(40, 100)
                < FadedTrustVisuals.colorChannel(220, 100));
        assertThrows(IllegalArgumentException.class, () -> FadedTrustVisuals.colorChannel(256, 0));
    }
}
