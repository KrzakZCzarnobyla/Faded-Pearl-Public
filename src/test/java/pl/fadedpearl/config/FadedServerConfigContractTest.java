package pl.fadedpearl.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FadedServerConfigContractTest {
    @Test
    void defaultsPreserveThePreConfigGameplayValues() {
        assertEquals(200, FadedServerConfig.DEFAULT_ENCOUNTER_CHECK_INTERVAL_TICKS);
        assertEquals(45, FadedServerConfig.DEFAULT_ENCOUNTER_MAX_Y);
        assertEquals(100, FadedServerConfig.DEFAULT_ENCOUNTER_MIN_HORIZONTAL_SPACING);
        assertEquals(100, FadedServerConfig.DEFAULT_TRUST_GAIN_PERCENT);
        assertEquals(100, FadedServerConfig.DEFAULT_TRUST_LOSS_PERCENT);
        assertEquals(30, FadedServerConfig.DEFAULT_RECOVERY_GRACE_SECONDS);
        assertEquals(1, FadedServerConfig.DEFAULT_ANCHOR_RECALL_TRUST_REWARD);
    }
}
