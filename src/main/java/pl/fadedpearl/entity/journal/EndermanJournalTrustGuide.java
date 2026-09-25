package pl.fadedpearl.entity.journal;

import pl.fadedpearl.entity.curiosity.ItemCuriosityPolicy;
import pl.fadedpearl.entity.trust.FadedTrustManager;
import pl.fadedpearl.entity.social.SmallAnimalCarryPolicy;
import pl.fadedpearl.entity.behavior.CompanionLivelinessPolicy;

import java.util.OptionalInt;

/** Only unambiguous trust gates for already discovered journal behaviors. */
public final class EndermanJournalTrustGuide {
    public static int heartNumber(int trust) {
        return FadedTrustManager.stageIndex(trust) + 1;
    }

    public static OptionalInt requiredTrust(EndermanJournalSnapshot.BehaviorEntry behavior) {
        return switch (behavior) {
            case CURIOSITY -> OptionalInt.of(ItemCuriosityPolicy.MIN_TRUST);
            case CARRY -> OptionalInt.of(FadedTrustManager.STAGE_PARTNER);
            case SOCIAL_REPOSITION -> OptionalInt.of(CompanionLivelinessPolicy.SOCIAL_MIN_TRUST);
            case ANIMAL_CARRY -> OptionalInt.of(SmallAnimalCarryPolicy.MIN_TRUST);
            default -> OptionalInt.empty();
        };
    }

    private EndermanJournalTrustGuide() {}
}
