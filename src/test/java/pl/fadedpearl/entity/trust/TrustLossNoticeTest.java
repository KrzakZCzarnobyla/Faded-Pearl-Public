package pl.fadedpearl.entity.trust;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TrustLossNoticeTest {
    @Test
    void reportsOnlyActualClampedLoss() {
        assertEquals(new TrustLossNotice(3, TrustLossNotice.Reason.RAIN),
                TrustLossNotice.from(10, FadedTrustManager.RAIN_EXPOSURE));
        assertEquals(new TrustLossNotice(2, TrustLossNotice.Reason.WATER),
                TrustLossNotice.from(2, FadedTrustManager.ENTERED_WATER));
        assertEquals(0, TrustLossNotice.from(0, FadedTrustManager.PLAYER_ATTACK).points());
        assertEquals(0, TrustLossNotice.from(10, FadedTrustManager.FLOWER_GIFT).points());
    }

    @Test
    void classifiesExistingNegativeModifiers() {
        assertEquals(TrustLossNotice.Reason.FRIEND_HIT,
                TrustLossNotice.from(50, FadedTrustManager.PLAYER_ATTACK).reason());
        assertEquals(TrustLossNotice.Reason.STARE,
                TrustLossNotice.from(50, FadedTrustManager.PERSISTENT_STARE).reason());
    }

    @Test
    void keepsTheOriginalReasonWhileReportingTheScaledLoss() {
        assertEquals(new TrustLossNotice(4, TrustLossNotice.Reason.FRIEND_HIT),
                TrustLossNotice.from(50, FadedTrustManager.PLAYER_ATTACK, -4));
        assertEquals(new TrustLossNotice(0, TrustLossNotice.Reason.WATER),
                TrustLossNotice.from(50, FadedTrustManager.ENTERED_WATER, 0));
    }
}
