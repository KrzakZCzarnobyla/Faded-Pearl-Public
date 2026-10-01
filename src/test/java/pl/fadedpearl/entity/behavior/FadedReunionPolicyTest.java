package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pl.fadedpearl.entity.behavior.FadedReunionPolicy.CompletionDecision;
import static pl.fadedpearl.entity.behavior.FadedReunionPolicy.ContactDecision;

final class FadedReunionPolicyTest {
    @Test
    void onlyRealContactRefreshesTheClock() {
        assertEquals(ContactDecision.NONE,
                FadedReunionPolicy.observeContact(10L, 30_000L, false));
        assertEquals(ContactDecision.INITIALIZE,
                FadedReunionPolicy.observeContact(0L, 30_000L, true));
        assertEquals(ContactDecision.REFRESH,
                FadedReunionPolicy.observeContact(10_000L, 34_000L, true));
    }

    @Test
    void absenceMustBeStrictlyLongerThanOneMinecraftDay() {
        assertEquals(ContactDecision.REFRESH,
                FadedReunionPolicy.observeContact(10L, 24_010L, true));
        assertEquals(ContactDecision.ARM_REUNION,
                FadedReunionPolicy.observeContact(10L, 24_011L, true));
    }

    @Test
    void pendingReunionWaitsWithoutOwningUrgentBehavior() {
        assertEquals(CompletionDecision.NONE,
                FadedReunionPolicy.advance(false, 0, true, true, 100));
        assertEquals(CompletionDecision.WAITING,
                FadedReunionPolicy.advance(true, 0, false, true, 100));
        assertEquals(CompletionDecision.WAITING,
                FadedReunionPolicy.advance(true, 0, true, false, 100));
    }

    @Test
    void calmCompletionUsesTheTrustTierBoundary() {
        assertEquals(CompletionDecision.COMPLETE_LEARNING,
                FadedReunionPolicy.advance(true, 0, true, true, 59));
        assertEquals(CompletionDecision.COMPLETE_BONDED,
                FadedReunionPolicy.advance(true, 0, true, true, 60));
    }

    @Test
    void staleOpportunityIsCancelledBeforeItCanComplete() {
        assertEquals(CompletionDecision.WAITING,
                FadedReunionPolicy.advance(true, FadedReunionPolicy.MAX_PENDING_TICKS - 1,
                        true, false, 60));
        assertEquals(CompletionDecision.CANCELLED,
                FadedReunionPolicy.advance(true, FadedReunionPolicy.MAX_PENDING_TICKS,
                        true, true, 60));
    }
}
