package pl.fadedpearl.entity.journal;

import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.journal.EndermanJournalSnapshot.BehaviorEntry;
import static pl.fadedpearl.entity.journal.EndermanJournalSnapshot.DayEntry;

final class EndermanJournalPolicyTest {
    @Test
    void mapsApprovedPhaseACatalogOnlyFromConfirmedFacts() {
        EndermanJournalSnapshot snapshot = EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                Set.of(JournalMemory.Discovery.DAY_MEETING, JournalMemory.Discovery.DAY_HEALING,
                        JournalMemory.Discovery.BEHAVIOR_COMMANDS, JournalMemory.Discovery.BEHAVIOR_RESCUE),
                Set.of(WorldAwarenessMemory.Milestone.VILLAGE, WorldAwarenessMemory.Milestone.PLAYER_DIAMOND,
                        WorldAwarenessMemory.Milestone.ENDERMAN_DIAMOND, WorldAwarenessMemory.Milestone.ENDER_PEARL,
                        WorldAwarenessMemory.Milestone.ARMOR_UPGRADE, WorldAwarenessMemory.Milestone.TAMED_ANY,
                        WorldAwarenessMemory.Milestone.BUILD_COMPLETED),
                true, Optional.of("Lumen"), true, true, 0x12ABCDEF, 60));

        assertEquals(Set.of(DayEntry.values()), snapshot.days());
        assertTrue(snapshot.behaviors().containsAll(Set.of(BehaviorEntry.CURIOSITY,
                BehaviorEntry.OWN_NAME, BehaviorEntry.NAMED_PETS, BehaviorEntry.HOME,
                BehaviorEntry.COMMANDS, BehaviorEntry.RESCUE)));
        assertFalse(snapshot.behaviors().contains(BehaviorEntry.CARRY));
        assertEquals("Lumen", snapshot.knownName());
        assertEquals(0xABCDEF, snapshot.healingColor());
        assertEquals(60, snapshot.trust());
        assertFalse(snapshot.fadeOriginKnown());
    }

    @Test
    void namePetWeatherAndShelterDiscoveriesRemainIndependent() {
        EndermanJournalSnapshot ownNameOnly = EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                Set.of(JournalMemory.Discovery.BEHAVIOR_WEATHER_REACTION), Set.of(), false,
                Optional.of("Lumen"), false, false, 0, 12));
        assertTrue(ownNameOnly.behaviors().contains(BehaviorEntry.OWN_NAME));
        assertTrue(ownNameOnly.behaviors().contains(BehaviorEntry.WEATHER_REACTION));
        assertFalse(ownNameOnly.behaviors().contains(BehaviorEntry.NAMED_PETS));
        assertFalse(ownNameOnly.behaviors().contains(BehaviorEntry.SHARED_SHELTER));

        EndermanJournalSnapshot petAndShelter = EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                Set.of(JournalMemory.Discovery.BEHAVIOR_SHARED_SHELTER), Set.of(), false,
                Optional.empty(), true, false, 0, 35));
        assertFalse(petAndShelter.behaviors().contains(BehaviorEntry.OWN_NAME));
        assertFalse(petAndShelter.behaviors().contains(BehaviorEntry.WEATHER_REACTION));
        assertTrue(petAndShelter.behaviors().contains(BehaviorEntry.NAMED_PETS));
        assertTrue(petAndShelter.behaviors().contains(BehaviorEntry.SHARED_SHELTER));
    }

    @Test
    void emptyLegacyFactsRevealNothingExceptSafeBasicColor() {
        EndermanJournalSnapshot snapshot = EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                Set.of(), Set.of(), false, Optional.empty(), false, false, -1, -10));
        assertAll(() -> assertTrue(snapshot.days().isEmpty()),
                () -> assertTrue(snapshot.behaviors().isEmpty()),
                () -> assertTrue(snapshot.knownName().isEmpty()),
                () -> assertEquals(0xFFFFFF, snapshot.healingColor()),
                () -> assertEquals(0, snapshot.trust()),
                () -> assertFalse(snapshot.fadeOriginKnown()));
    }

    @Test
    void fadeOriginRevealsOnlyAfterPersistentDiscovery() {
        EndermanJournalPolicy.Facts facts = new EndermanJournalPolicy.Facts(
                Set.of(JournalMemory.Discovery.BASIC_FADE_ORIGIN), Set.of(), false,
                Optional.empty(), false, false, 0, 85);
        EndermanJournalSnapshot snapshot = EndermanJournalPolicy.snapshot(facts);
        assertTrue(snapshot.fadeOriginKnown());
    }

    @Test
    void animalCarryDoesNotUnlockPlayerCarryOrPetNames() {
        EndermanJournalSnapshot snapshot = EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                Set.of(JournalMemory.Discovery.BEHAVIOR_ANIMAL_CARRY), Set.of(), false,
                Optional.empty(), false, false, 0, 75));
        assertEquals(Set.of(BehaviorEntry.ANIMAL_CARRY), snapshot.behaviors());
    }

    @Test
    void escapePearlRequiresBothOwnerHitAndPriorEnderPearlPossession() {
        assertFalse(escapePearl(Set.of()));
        assertFalse(escapePearl(Set.of(JournalMemory.Discovery.ESCAPE_PEARL_PLAYER_HIT)));
        assertFalse(escapePearl(Set.of(JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD)));
        assertTrue(escapePearl(Set.of(JournalMemory.Discovery.ESCAPE_PEARL_PLAYER_HIT,
                JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD)));
    }

    private static boolean escapePearl(Set<JournalMemory.Discovery> discoveries) {
        return EndermanJournalPolicy.snapshot(new EndermanJournalPolicy.Facts(
                discoveries, Set.of(), false, Optional.empty(), false, false, 0, 0))
                .behaviors().contains(BehaviorEntry.ESCAPE_PEARL);
    }
}
