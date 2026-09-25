package pl.fadedpearl.entity.trust;

/** A visible loss is the clamped change, never the requested penalty at the lower bound. */
public record TrustLossNotice(int points, Reason reason) {
    public enum Reason { FRIEND_HIT, STARE, RAIN, WATER, OTHER }

    public static TrustLossNotice from(int before, int modifier) {
        return from(before, modifier, modifier);
    }

    public static TrustLossNotice from(int before, int reasonModifier, int appliedModifier) {
        int lost = Math.max(0, before - FadedTrustManager.applyModifier(before, appliedModifier));
        return new TrustLossNotice(lost, switch (reasonModifier) {
            case FadedTrustManager.PLAYER_ATTACK -> Reason.FRIEND_HIT;
            case FadedTrustManager.PERSISTENT_STARE -> Reason.STARE;
            case FadedTrustManager.RAIN_EXPOSURE -> Reason.RAIN;
            case FadedTrustManager.ENTERED_WATER -> Reason.WATER;
            default -> Reason.OTHER;
        });
    }
}
