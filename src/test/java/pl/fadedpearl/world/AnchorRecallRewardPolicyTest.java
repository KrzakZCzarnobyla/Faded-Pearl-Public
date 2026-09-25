package pl.fadedpearl.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnchorRecallRewardPolicyTest {
    @Test
    void onlyMeaningfulConfiguredRecallAwardsTrust() {
        assertFalse(AnchorRecallRewardPolicy.shouldReward(63.99D, 1));
        assertTrue(AnchorRecallRewardPolicy.shouldReward(64.0D, 1));
        assertFalse(AnchorRecallRewardPolicy.shouldReward(400.0D, 0));
    }
}
