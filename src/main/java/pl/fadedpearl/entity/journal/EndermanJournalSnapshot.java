package pl.fadedpearl.entity.journal;

import pl.fadedpearl.entity.dialogue.NameLearningMemory;
import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.EnumSet;
import java.util.Set;

public record EndermanJournalSnapshot(
        Set<DayEntry> days,
        Set<BehaviorEntry> behaviors,
        String knownName,
        int healingColor,
        boolean fadeOriginKnown,
        int trust) {

    public enum DayEntry {
        MEETING, HEALING, VILLAGE, PLAYER_DIAMOND, ENDERMAN_DIAMOND,
        ENDER_PEARL, ARMOR_UPGRADE, FIRST_TAME, FIRST_BUILD
    }

    public enum BehaviorEntry {
        CURIOSITY, OWN_NAME, NAMED_PETS, HOME, COMMANDS, CARRY, RESCUE,
        WEATHER_REACTION, SHARED_SHELTER, SOCIAL_REPOSITION, ANIMAL_CARRY, ESCAPE_PEARL
    }

    public EndermanJournalSnapshot {
        days = days == null || days.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(days));
        behaviors = behaviors == null || behaviors.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(behaviors));
        knownName = NameLearningMemory.sanitizeName(knownName);
        healingColor &= 0xFFFFFF;
        trust = Math.max(FadedTrustManager.MIN_TRUST, Math.min(FadedTrustManager.MAX_TRUST, trust));
    }
}
