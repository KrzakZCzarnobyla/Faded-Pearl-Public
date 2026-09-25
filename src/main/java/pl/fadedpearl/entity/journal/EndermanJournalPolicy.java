package pl.fadedpearl.entity.journal;

import pl.fadedpearl.entity.dialogue.WorldAwarenessMemory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static pl.fadedpearl.entity.journal.EndermanJournalSnapshot.BehaviorEntry;
import static pl.fadedpearl.entity.journal.EndermanJournalSnapshot.DayEntry;

/** Pure catalog policy: it reveals only server-confirmed facts supplied by the companion state. */
public final class EndermanJournalPolicy {
    public record Facts(
            Set<JournalMemory.Discovery> journal,
            Set<WorldAwarenessMemory.Milestone> awareness,
            boolean curiosityObserved,
            Optional<String> learnedName,
            boolean learnedPetName,
            boolean hasHome,
            int healingColor,
            int trust) {
        public Facts {
            journal = journal == null ? Set.of() : Set.copyOf(journal);
            awareness = awareness == null ? Set.of() : Set.copyOf(awareness);
            learnedName = learnedName == null ? Optional.empty() : learnedName;
        }
    }

    public static EndermanJournalSnapshot snapshot(Facts facts) {
        EnumSet<DayEntry> days = EnumSet.noneOf(DayEntry.class);
        EnumSet<BehaviorEntry> behaviors = EnumSet.noneOf(BehaviorEntry.class);
        if (facts.journal().contains(JournalMemory.Discovery.DAY_MEETING)) days.add(DayEntry.MEETING);
        if (facts.journal().contains(JournalMemory.Discovery.DAY_HEALING)) days.add(DayEntry.HEALING);
        mapAwareness(facts.awareness(), days);
        if (facts.curiosityObserved()) behaviors.add(BehaviorEntry.CURIOSITY);
        if (facts.learnedName().isPresent()) behaviors.add(BehaviorEntry.OWN_NAME);
        if (facts.learnedPetName()) behaviors.add(BehaviorEntry.NAMED_PETS);
        if (facts.hasHome()) behaviors.add(BehaviorEntry.HOME);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_COMMANDS)) behaviors.add(BehaviorEntry.COMMANDS);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_CARRY)) behaviors.add(BehaviorEntry.CARRY);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_RESCUE)) behaviors.add(BehaviorEntry.RESCUE);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_WEATHER_REACTION))
            behaviors.add(BehaviorEntry.WEATHER_REACTION);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_SHARED_SHELTER))
            behaviors.add(BehaviorEntry.SHARED_SHELTER);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_SOCIAL_REPOSITION))
            behaviors.add(BehaviorEntry.SOCIAL_REPOSITION);
        if (facts.journal().contains(JournalMemory.Discovery.BEHAVIOR_ANIMAL_CARRY))
            behaviors.add(BehaviorEntry.ANIMAL_CARRY);
        if (facts.journal().contains(JournalMemory.Discovery.ESCAPE_PEARL_PLAYER_HIT)
                && facts.journal().contains(JournalMemory.Discovery.ESCAPE_PEARL_ENDER_PEARL_HELD))
            behaviors.add(BehaviorEntry.ESCAPE_PEARL);
        return new EndermanJournalSnapshot(days, behaviors, facts.learnedName().orElse(""),
                facts.healingColor(), facts.journal().contains(JournalMemory.Discovery.BASIC_FADE_ORIGIN),
                facts.trust());
    }

    private static void mapAwareness(Set<WorldAwarenessMemory.Milestone> awareness, EnumSet<DayEntry> days) {
        if (awareness.contains(WorldAwarenessMemory.Milestone.VILLAGE)) days.add(DayEntry.VILLAGE);
        if (awareness.contains(WorldAwarenessMemory.Milestone.PLAYER_DIAMOND)) days.add(DayEntry.PLAYER_DIAMOND);
        if (awareness.contains(WorldAwarenessMemory.Milestone.ENDERMAN_DIAMOND)) days.add(DayEntry.ENDERMAN_DIAMOND);
        if (awareness.contains(WorldAwarenessMemory.Milestone.ENDER_PEARL)) days.add(DayEntry.ENDER_PEARL);
        if (awareness.contains(WorldAwarenessMemory.Milestone.ARMOR_UPGRADE)) days.add(DayEntry.ARMOR_UPGRADE);
        if (awareness.contains(WorldAwarenessMemory.Milestone.TAMED_ANY)) days.add(DayEntry.FIRST_TAME);
        if (awareness.contains(WorldAwarenessMemory.Milestone.BUILD_COMPLETED)) days.add(DayEntry.FIRST_BUILD);
    }

    private EndermanJournalPolicy() {}
}
