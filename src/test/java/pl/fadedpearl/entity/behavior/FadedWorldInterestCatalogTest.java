package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FadedWorldInterestCatalogTest {
    @Test
    void knowledgeKeepsItsAcceptanceThreshold() {
        assertFalse(FadedWorldInterestCatalog.isUnlocked(
                FadedWorldInterestCatalog.Category.KNOWLEDGE, 24));
        assertTrue(FadedWorldInterestCatalog.isUnlocked(
                FadedWorldInterestCatalog.Category.KNOWLEDGE, 25));
    }

    @Test
    void craftsmanshipStartsAtBondThreshold() {
        assertFalse(FadedWorldInterestCatalog.isUnlocked(
                FadedWorldInterestCatalog.Category.CRAFTSMANSHIP, 34));
        assertTrue(FadedWorldInterestCatalog.isUnlocked(
                FadedWorldInterestCatalog.Category.CRAFTSMANSHIP, 35));
    }

    @Test
    void reachableLastTargetKeepsOneSlotInsideTheEightRouteBudget() {
        assertAll(
                () -> assertEquals(8, FadedWorldInterestCatalog.MAX_ROUTE_CANDIDATES),
                () -> assertEquals(8, FadedWorldInterestCatalog.primaryRouteBudget(false)),
                () -> assertEquals(7, FadedWorldInterestCatalog.primaryRouteBudget(true)));
    }
}
