package pl.fadedpearl.entity.behavior;

import java.util.Objects;

/** Pure trust gates for categories classified by the Minecraft-facing world adapter. */
public final class FadedWorldInterestCatalog {
    public static final int CRAFTSMANSHIP_MIN_TRUST = 35;
    public static final int MAX_ROUTE_CANDIDATES = 8;

    public enum Category { KNOWLEDGE, CRAFTSMANSHIP }

    public static int requiredTrust(Category category) {
        return switch (Objects.requireNonNull(category, "category")) {
            case KNOWLEDGE -> FadedWorldInterestPolicy.MIN_TRUST;
            case CRAFTSMANSHIP -> CRAFTSMANSHIP_MIN_TRUST;
        };
    }

    public static boolean isUnlocked(Category category, int trust) {
        return trust >= requiredTrust(category);
    }

    public static int primaryRouteBudget(boolean fallbackAvailable) {
        return fallbackAvailable ? MAX_ROUTE_CANDIDATES - 1 : MAX_ROUTE_CANDIDATES;
    }

    private FadedWorldInterestCatalog() {}
}
