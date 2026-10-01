package pl.fadedpearl.entity.dialogue;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.ARMOR_UPGRADE;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.BUILD;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.CRAFTSMANSHIP;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.ENDERMAN_DIAMOND;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.KNOWLEDGE;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.NIGHT_WATCH;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.PLAYER_DIAMOND;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.TAMED_CAT;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.TAMED_OTHER;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.TAMED_PARROT;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.TAMED_WOLF;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Context.VILLAGE;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ARMOR_UPGRADE_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.BUILD_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.BUILD_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.BUILD_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.CRAFT_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.CRAFT_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.CRAFT_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.ENDERMAN_DIAMOND_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.KNOWLEDGE_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.NONE;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.NIGHT_WATCH_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.PLAYER_DIAMOND_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_CAT_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_OTHER_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_PARROT_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.TAMED_WOLF_REPEAT_LEARNING;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.VILLAGE_FIRST;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.VILLAGE_REPEAT_BONDED;
import static pl.fadedpearl.entity.dialogue.FadedRelationshipDialoguePolicy.Selection.VILLAGE_REPEAT_LEARNING;

final class FadedRelationshipDialoguePolicyTest {
    private static final Map<FadedRelationshipDialoguePolicy.Context, FadedRelationshipDialoguePolicy.Selection>
            FIRST_SELECTIONS = selections(
                    KNOWLEDGE_FIRST,
                    CRAFT_FIRST,
                    VILLAGE_FIRST,
                    PLAYER_DIAMOND_FIRST,
                    ENDERMAN_DIAMOND_FIRST,
                    TAMED_WOLF_FIRST,
                    TAMED_CAT_FIRST,
                    TAMED_PARROT_FIRST,
                    TAMED_OTHER_FIRST,
                    BUILD_FIRST,
                    ARMOR_UPGRADE_FIRST,
                    NIGHT_WATCH_FIRST);
    private static final Map<FadedRelationshipDialoguePolicy.Context, FadedRelationshipDialoguePolicy.Selection>
            LEARNING_SELECTIONS = selections(
                    KNOWLEDGE_REPEAT_LEARNING,
                    CRAFT_REPEAT_LEARNING,
                    VILLAGE_REPEAT_LEARNING,
                    PLAYER_DIAMOND_REPEAT_LEARNING,
                    ENDERMAN_DIAMOND_REPEAT_LEARNING,
                    TAMED_WOLF_REPEAT_LEARNING,
                    TAMED_CAT_REPEAT_LEARNING,
                    TAMED_PARROT_REPEAT_LEARNING,
                    TAMED_OTHER_REPEAT_LEARNING,
                    BUILD_REPEAT_LEARNING,
                    ARMOR_UPGRADE_REPEAT_LEARNING,
                    NIGHT_WATCH_REPEAT_LEARNING);
    private static final Map<FadedRelationshipDialoguePolicy.Context, FadedRelationshipDialoguePolicy.Selection>
            BONDED_SELECTIONS = selections(
                    KNOWLEDGE_REPEAT_BONDED,
                    CRAFT_REPEAT_BONDED,
                    VILLAGE_REPEAT_BONDED,
                    PLAYER_DIAMOND_REPEAT_BONDED,
                    ENDERMAN_DIAMOND_REPEAT_BONDED,
                    TAMED_WOLF_REPEAT_BONDED,
                    TAMED_CAT_REPEAT_BONDED,
                    TAMED_PARROT_REPEAT_BONDED,
                    TAMED_OTHER_REPEAT_BONDED,
                    BUILD_REPEAT_BONDED,
                    ARMOR_UPGRADE_REPEAT_BONDED,
                    NIGHT_WATCH_REPEAT_BONDED);

    @Test
    void missingOwnerAlwaysReturnsNoneWithoutConsumingRng() {
        AtomicInteger calls = new AtomicInteger();

        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            assertAll(context.name(),
                    () -> assertEquals(NONE, select(context, false, true, 100, calls)),
                    () -> assertEquals(NONE, select(context, false, false, 100, calls)));
        }
        assertEquals(0, calls.get());
    }

    @Test
    void firstDiscoveryUsesItsContextFamilyWithoutConsumingRng() {
        AtomicInteger calls = new AtomicInteger();

        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            assertEquals(FIRST_SELECTIONS.get(context), select(context, true, true, 100, calls), context.name());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void repeatRollZeroUsesLearningAtFiftyNineAndBondedAtSixty() {
        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            assertAll(context.name(),
                    () -> assertEquals(LEARNING_SELECTIONS.get(context),
                            FadedRelationshipDialoguePolicy.select(context, true, false, 59, bound -> 0)),
                    () -> assertEquals(BONDED_SELECTIONS.get(context),
                            FadedRelationshipDialoguePolicy.select(context, true, false, 60, bound -> 0)));
        }
    }

    @Test
    void everyRepeatPerformsExactlyOneBoundedRollFromZeroToTwo() {
        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            for (int roll = 0; roll < FadedRelationshipDialoguePolicy.REPEAT_ROLL_BOUND; roll++) {
                AtomicInteger calls = new AtomicInteger();
                AtomicReference<Integer> observedBound = new AtomicReference<>();
                int returnedRoll = roll;
                FadedRelationshipDialoguePolicy.Selection expected = returnedRoll == 0
                        ? LEARNING_SELECTIONS.get(context)
                        : NONE;

                FadedRelationshipDialoguePolicy.Selection selection = FadedRelationshipDialoguePolicy.select(
                        context, true, false, 59, bound -> {
                            calls.incrementAndGet();
                            observedBound.set(bound);
                            return returnedRoll;
                        });

                assertAll(context.name() + " roll " + roll,
                        () -> assertEquals(1, calls.get()),
                        () -> assertEquals(3, observedBound.get()),
                        () -> assertEquals(expected, selection));
            }
        }
    }

    @Test
    void everyContextSelectsAnIndependentDialogueFamily() {
        assertAll(
                () -> assertEquals(FadedRelationshipDialoguePolicy.Context.values().length,
                        FIRST_SELECTIONS.size()),
                () -> assertEquals(FadedRelationshipDialoguePolicy.Context.values().length,
                        LEARNING_SELECTIONS.size()),
                () -> assertEquals(FadedRelationshipDialoguePolicy.Context.values().length,
                        BONDED_SELECTIONS.size()),
                () -> assertEquals(FIRST_SELECTIONS.size(), new HashSet<>(FIRST_SELECTIONS.values()).size()),
                () -> assertEquals(LEARNING_SELECTIONS.size(), new HashSet<>(LEARNING_SELECTIONS.values()).size()),
                () -> assertEquals(BONDED_SELECTIONS.size(), new HashSet<>(BONDED_SELECTIONS.values()).size()));

        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            assertAll(context.name(),
                    () -> assertEquals(FIRST_SELECTIONS.get(context),
                            FadedRelationshipDialoguePolicy.select(context, true, true, 100, bound -> {
                                throw new AssertionError("first event must not read RNG");
                            })),
                    () -> assertEquals(LEARNING_SELECTIONS.get(context),
                            FadedRelationshipDialoguePolicy.select(context, true, false, 59, bound -> 0)),
                    () -> assertEquals(BONDED_SELECTIONS.get(context),
                            FadedRelationshipDialoguePolicy.select(context, true, false, 60, bound -> 0)));
        }
    }

    @Test
    void rejectedRepeatRollsNeverSelectAnyContextFamily() {
        for (FadedRelationshipDialoguePolicy.Context context
                : FadedRelationshipDialoguePolicy.Context.values()) {
            assertAll(context.name(),
                    () -> assertEquals(NONE,
                            FadedRelationshipDialoguePolicy.select(context, true, false, 100, bound -> 1)),
                    () -> assertEquals(NONE,
                            FadedRelationshipDialoguePolicy.select(context, true, false, 100, bound -> 2)));
        }
    }

    @Test
    void invalidRepeatRollIsRejectedAfterOneRead() {
        for (int roll : new int[]{-1, 3}) {
            AtomicInteger calls = new AtomicInteger();

            assertAll("roll " + roll,
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> FadedRelationshipDialoguePolicy.select(
                                    KNOWLEDGE, true, false, 60, bound -> {
                                        calls.incrementAndGet();
                                        return roll;
                                    })),
                    () -> assertEquals(1, calls.get()));
        }
    }

    private static FadedRelationshipDialoguePolicy.Selection select(
            FadedRelationshipDialoguePolicy.Context context,
            boolean ownerPresent,
            boolean firstDiscovery,
            int trust,
            AtomicInteger calls
    ) {
        return FadedRelationshipDialoguePolicy.select(context, ownerPresent, firstDiscovery, trust, bound -> {
            calls.incrementAndGet();
            return 0;
        });
    }

    private static Map<FadedRelationshipDialoguePolicy.Context, FadedRelationshipDialoguePolicy.Selection> selections(
            FadedRelationshipDialoguePolicy.Selection knowledge,
            FadedRelationshipDialoguePolicy.Selection craftsmanship,
            FadedRelationshipDialoguePolicy.Selection village,
            FadedRelationshipDialoguePolicy.Selection playerDiamond,
            FadedRelationshipDialoguePolicy.Selection endermanDiamond,
            FadedRelationshipDialoguePolicy.Selection tamedWolf,
            FadedRelationshipDialoguePolicy.Selection tamedCat,
            FadedRelationshipDialoguePolicy.Selection tamedParrot,
            FadedRelationshipDialoguePolicy.Selection tamedOther,
            FadedRelationshipDialoguePolicy.Selection build,
            FadedRelationshipDialoguePolicy.Selection armorUpgrade,
            FadedRelationshipDialoguePolicy.Selection nightWatch
    ) {
        EnumMap<FadedRelationshipDialoguePolicy.Context, FadedRelationshipDialoguePolicy.Selection> result =
                new EnumMap<>(FadedRelationshipDialoguePolicy.Context.class);
        result.put(KNOWLEDGE, knowledge);
        result.put(CRAFTSMANSHIP, craftsmanship);
        result.put(VILLAGE, village);
        result.put(PLAYER_DIAMOND, playerDiamond);
        result.put(ENDERMAN_DIAMOND, endermanDiamond);
        result.put(TAMED_WOLF, tamedWolf);
        result.put(TAMED_CAT, tamedCat);
        result.put(TAMED_PARROT, tamedParrot);
        result.put(TAMED_OTHER, tamedOther);
        result.put(BUILD, build);
        result.put(ARMOR_UPGRADE, armorUpgrade);
        result.put(NIGHT_WATCH, nightWatch);
        return result;
    }
}
