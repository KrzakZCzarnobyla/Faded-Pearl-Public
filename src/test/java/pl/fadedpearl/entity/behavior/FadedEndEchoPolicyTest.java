package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.fadedpearl.entity.behavior.FadedEndEchoPolicy.DragonState;
import static pl.fadedpearl.entity.behavior.FadedEndEchoPolicy.Reaction;
import static pl.fadedpearl.entity.dialogue.WorldAwarenessMemory.Milestone;

final class FadedEndEchoPolicyTest {
    @Test
    void everyApprovedMemoryIsSelectedOnlyWhileUndiscovered() {
        assertSelection(Reaction.END_PORTAL,
                observation(true, false, DragonState.UNAVAILABLE, false, true), Set.of());
        assertSelection(Reaction.DRAGON_EGG,
                observation(false, false, DragonState.UNAVAILABLE, true, false), Set.of());
        assertSelection(Reaction.END_RETURN,
                observation(false, false, DragonState.UNAVAILABLE, false, true), Set.of(Milestone.END_ARRIVAL));

        for (Reaction reaction : Reaction.values()) {
            Milestone milestone = FadedEndEchoPolicy.milestone(reaction);
            FadedEndEchoPolicy.Observation trigger = switch (reaction) {
                case END_PORTAL -> observation(true, false, DragonState.UNAVAILABLE, false, true);
                case END_ARRIVAL_LIVE -> observation(false, true, DragonState.LIVE_FIRST_FIGHT, false, false);
                case END_ARRIVAL_DEFEATED -> observation(false, true, DragonState.PREVIOUSLY_DEFEATED, false, false);
                case DRAGON_DEFEATED -> observation(false, true, DragonState.PREVIOUSLY_DEFEATED, false, false);
                case DRAGON_EGG -> observation(false, false, DragonState.UNAVAILABLE, true, false);
                case END_RETURN -> observation(false, false, DragonState.UNAVAILABLE, false, true);
            };
            Set<Milestone> seen = reaction == Reaction.DRAGON_DEFEATED
                    ? Set.of(Milestone.END_ARRIVAL, Milestone.END_DRAGON_LIVE_WITNESSED, milestone)
                    : reaction == Reaction.END_RETURN
                    ? Set.of(Milestone.END_ARRIVAL, milestone)
                    : Set.of(milestone);
            assertTrue(FadedEndEchoPolicy.select(trigger, seen).isEmpty(), reaction.name());
        }
    }

    @Test
    void firstArrivalUsesAuthoritativeFightHistory() {
        assertSelection(Reaction.END_ARRIVAL_LIVE,
                observation(false, true, DragonState.LIVE_FIRST_FIGHT, false, false), Set.of());
        assertSelection(Reaction.END_ARRIVAL_DEFEATED,
                observation(false, true, DragonState.PREVIOUSLY_DEFEATED, false, false), Set.of());
        assertTrue(FadedEndEchoPolicy.select(
                observation(false, true, DragonState.UNAVAILABLE, false, false), Set.of()).isEmpty());
    }

    @Test
    void endExitPortalCanNeverBecomeTheOverworldThresholdMemory() {
        assertTrue(FadedEndEchoPolicy.select(
                observation(true, true, DragonState.UNAVAILABLE, false, false), Set.of()).isEmpty());
        assertSelection(Reaction.END_PORTAL,
                observation(true, false, DragonState.UNAVAILABLE, false, true), Set.of());
    }

    @Test
    void portalScanIsSkippedAfterDiscoveryAndOutsideTheOverworld() {
        assertTrue(FadedEndEchoPolicy.shouldScanEndPortal(Set.of(), true, true, 20));
        assertFalse(FadedEndEchoPolicy.shouldScanEndPortal(
                Set.of(Milestone.END_PORTAL), true, true, 20));
        assertFalse(FadedEndEchoPolicy.shouldScanEndPortal(Set.of(), true, false, 20));
        assertFalse(FadedEndEchoPolicy.shouldScanEndPortal(Set.of(), false, true, 20));
        assertFalse(FadedEndEchoPolicy.shouldScanEndPortal(Set.of(), true, true, 19));
    }

    @Test
    void persistedLiveWitnessOwnsTheArrivalVariantAcrossDelayReloadAndDragonDeath() {
        Set<Milestone> witnessedLiveFight = Set.of(Milestone.END_DRAGON_LIVE_WITNESSED);
        FadedEndEchoPolicy.Observation afterDragonDeath = observation(
                false, true, DragonState.PREVIOUSLY_DEFEATED, false, false);

        assertSelection(Reaction.END_ARRIVAL_LIVE, afterDragonDeath, witnessedLiveFight);
        assertTrue(FadedEndEchoPolicy.stillRelevant(
                Reaction.END_ARRIVAL_LIVE, afterDragonDeath, witnessedLiveFight));
    }

    @Test
    void dragonDefeatNeedsPersistedEvidenceOfTheLiveFight() {
        FadedEndEchoPolicy.Observation defeated = observation(
                false, true, DragonState.PREVIOUSLY_DEFEATED, false, false);
        assertTrue(FadedEndEchoPolicy.select(defeated, Set.of(Milestone.END_ARRIVAL)).isEmpty());
        assertSelection(Reaction.DRAGON_DEFEATED, defeated,
                Set.of(Milestone.END_ARRIVAL, Milestone.END_DRAGON_LIVE_WITNESSED));
        assertTrue(FadedEndEchoPolicy.select(
                observation(false, true, DragonState.LIVE_FIRST_FIGHT, false, false),
                Set.of(Milestone.END_ARRIVAL, Milestone.END_DRAGON_LIVE_WITNESSED)).isEmpty());
    }

    @Test
    void shortDeferralWaitsForSafetyAndThenExpires() {
        assertEquals(FadedEndEchoPolicy.PendingDecision.WAIT,
                FadedEndEchoPolicy.advancePending(0, false));
        assertEquals(FadedEndEchoPolicy.PendingDecision.COMPLETE,
                FadedEndEchoPolicy.advancePending(FadedEndEchoPolicy.MAX_PENDING_TICKS - 1, true));
        assertEquals(FadedEndEchoPolicy.PendingDecision.CANCEL,
                FadedEndEchoPolicy.advancePending(FadedEndEchoPolicy.MAX_PENDING_TICKS, true));
    }

    @Test
    void completionRevalidatesEveryEventsCurrentContext() {
        Set<Milestone> liveFight = Set.of(Milestone.END_DRAGON_LIVE_WITNESSED);
        Set<Milestone> visitedEnd = Set.of(Milestone.END_ARRIVAL);

        assertTrue(FadedEndEchoPolicy.contextPresent(Reaction.END_PORTAL,
                observation(false, false, DragonState.UNAVAILABLE, false, true), Set.of()));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.END_PORTAL,
                observation(false, false, DragonState.UNAVAILABLE, false, true), Set.of()));
        assertTrue(FadedEndEchoPolicy.stillRelevant(Reaction.END_PORTAL,
                observation(true, false, DragonState.UNAVAILABLE, false, true), Set.of()));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.END_PORTAL,
                observation(true, true, DragonState.UNAVAILABLE, false, false), Set.of()));

        FadedEndEchoPolicy.Observation killedDuringArrival = observation(
                false, true, DragonState.PREVIOUSLY_DEFEATED, false, false);
        assertTrue(FadedEndEchoPolicy.stillRelevant(
                Reaction.END_ARRIVAL_LIVE, killedDuringArrival, liveFight));
        assertTrue(FadedEndEchoPolicy.stillRelevant(
                Reaction.END_ARRIVAL_DEFEATED, killedDuringArrival, Set.of()));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.END_ARRIVAL_LIVE,
                observation(false, false, DragonState.PREVIOUSLY_DEFEATED, false, true), liveFight));

        assertTrue(FadedEndEchoPolicy.stillRelevant(
                Reaction.DRAGON_DEFEATED, killedDuringArrival, liveFight));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.DRAGON_DEFEATED,
                observation(false, true, DragonState.LIVE_FIRST_FIGHT, false, false), liveFight));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.DRAGON_DEFEATED,
                observation(false, false, DragonState.PREVIOUSLY_DEFEATED, false, true), liveFight));

        assertTrue(FadedEndEchoPolicy.stillRelevant(Reaction.DRAGON_EGG,
                observation(false, false, DragonState.UNAVAILABLE, true, true), Set.of()));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.DRAGON_EGG,
                observation(false, false, DragonState.UNAVAILABLE, false, true), Set.of()));

        assertTrue(FadedEndEchoPolicy.stillRelevant(Reaction.END_RETURN,
                observation(false, false, DragonState.UNAVAILABLE, false, true), visitedEnd));
        assertFalse(FadedEndEchoPolicy.stillRelevant(Reaction.END_RETURN,
                observation(false, true, DragonState.LIVE_FIRST_FIGHT, false, false), visitedEnd));
    }

    private static FadedEndEchoPolicy.Observation observation(
            boolean portal,
            boolean inEnd,
            DragonState dragonState,
            boolean egg,
            boolean overworld
    ) {
        return new FadedEndEchoPolicy.Observation(portal, inEnd, dragonState, egg, overworld);
    }

    private static void assertSelection(
            Reaction expected,
            FadedEndEchoPolicy.Observation observation,
            Set<Milestone> seen
    ) {
        assertEquals(expected, FadedEndEchoPolicy.select(observation, seen).orElseThrow());
    }
}
