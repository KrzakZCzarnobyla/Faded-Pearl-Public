package pl.fadedpearl.entity.journal;

import org.junit.jupiter.api.Test;
import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

final class EndermanJournalTrustGuideTest {
    @Test
    void heartNumbersMatchActualEightTrustStages() {
        int[] starts = {0, 6, 12, 25, 35, 60, 75, 85};
        for (int index = 0; index < starts.length; index++)
            assertEquals(index + 1, EndermanJournalTrustGuide.heartNumber(starts[index]));
        assertEquals(3, EndermanJournalTrustGuide.heartNumber(20));
        assertEquals(8, EndermanJournalTrustGuide.heartNumber(FadedTrustManager.MAX_TRUST));
    }

    @Test
    void onlyUnambiguousObservedAbilitiesHaveAThreshold() {
        assertEquals(OptionalInt.of(20), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.CURIOSITY));
        assertEquals(OptionalInt.of(75), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.CARRY));
        assertEquals(OptionalInt.of(60), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.ANIMAL_CARRY));
        assertEquals(OptionalInt.of(60), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.SOCIAL_REPOSITION));
        assertEquals(OptionalInt.empty(), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.RESCUE));
        assertEquals(OptionalInt.empty(), EndermanJournalTrustGuide.requiredTrust(
                EndermanJournalSnapshot.BehaviorEntry.NAMED_PETS));
    }
}
