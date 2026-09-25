package pl.fadedpearl.entity.interaction;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.interaction.FadedInteractionHandler.HeldItem.*;
import static pl.fadedpearl.entity.interaction.FadedInteractionHandler.InteractionIntent.*;
import static pl.fadedpearl.entity.interaction.FadedInteractionHandler.TouchIntent.*;

class FadedInteractionHandlerTest {
    @Test
    void handClientAndServerPlayerGuardsHaveRequiredPriority() {
        assertEquals(PASS, classify(input(false, true, false, false, false, false, false, true, PULSATING_PEARL)));
        assertEquals(CLIENT_SUCCESS, classify(input(true, true, false, false, false, false, false, false, PULSATING_PEARL)));
        assertEquals(PASS, classify(input(true, false, false, false, false, false, false, false, PULSATING_PEARL)));
    }

    @Test
    void woundedPearlsOverrideFirstMeetingAndFirstMeetingOccursOnlyOnce() {
        assertEquals(HEAL_WITH_PULSATING_PEARL, classify(wounded(false, PULSATING_PEARL)));
        assertEquals(HEAL_WITH_PULSATING_PEARL, classify(wounded(true, PULSATING_PEARL)));
        assertEquals(EMPTY_PEARL_SILENCE, classify(wounded(false, EMPTY_PEARL)));
        assertEquals(EMPTY_PEARL_SILENCE, classify(wounded(true, EMPTY_PEARL)));
        assertEquals(FIRST_MEETING_PEARL, classify(wounded(false, OTHER)));
        assertEquals(FIRST_MEETING_PEARL, classify(wounded(false, EMPTY_HAND)));
        assertEquals(PASS, classify(wounded(true, OTHER)));
    }

    @Test
    void healedStrangerCanOnlyProvokeNeutralEmptyHandReaction() {
        assertEquals(STRANGER_NEUTRAL_REACTION, classify(healed(false, false, false, EMPTY_HAND)));
        assertEquals(STRANGER_NEUTRAL_REACTION, classify(healed(false, false, true, EMPTY_HAND)));
        for (FadedInteractionHandler.HeldItem held : List.of(PULSATING_PEARL, EMPTY_PEARL, ESCAPE_PEARL, SMALL_FLOWER, OTHER))
            assertEquals(PASS, classify(healed(false, false, false, held)));
        for (FadedInteractionHandler.HeldItem held : FadedInteractionHandler.HeldItem.values())
            assertEquals(PASS, classify(healed(false, true, false, held)));
    }

    @Test
    void downedRecoveryOverridesHeldItemAndOtherFriendBranchesAreExclusive() {
        for (FadedInteractionHandler.HeldItem held : FadedInteractionHandler.HeldItem.values())
            assertEquals(RECOVER_DOWNED, classify(healed(true, true, false, held)));

        assertEquals(FLOWER_INTERACTION, classify(healed(true, false, false, SMALL_FLOWER)));
        assertEquals(EQUIP_ESCAPE_PEARL, classify(healed(true, false, false, ESCAPE_PEARL)));
        assertEquals(OPEN_COMMAND_MENU, classify(healed(true, false, true, EMPTY_HAND)));
        assertEquals(EMPTY_HAND_INTERACTION, classify(healed(true, false, false, EMPTY_HAND)));
        assertEquals(PASS, classify(healed(true, false, true, OTHER)));
        assertEquals(PASS, classify(healed(true, false, false, PULSATING_PEARL)));
    }

    @Test
    void touchThresholdBoundariesMatchBaseline() {
        assertEquals(TOUCH_RECOIL, touch(5, true));
        assertEquals(TOUCH_HESITATE, touch(6, true));
        assertEquals(TOUCH_HESITATE, touch(11, true));
        assertEquals(LOOK_AROUND, touch(12, true));
        assertEquals(LOOK_AROUND, touch(39, true));
        assertEquals(AFFECTION, touch(40, true));
        assertEquals(AFFECTION, touch(59, true));
        assertEquals(CHEST_EXPOSE, touch(60, true));
        assertEquals(CHEST_EXPOSE, touch(84, true));
        assertEquals(EMBRACE_READY, touch(85, true));
    }

    @Test
    void randomChoiceMapsBothMidTrustVariants() {
        assertEquals(AFFECTION, touch(40, true));
        assertEquals(CURIOUS, touch(59, false));
    }

    @Test
    void booleanSupplierIsConsumedExactlyOnceOnlyForFortyThroughFiftyNine() {
        for (int trust : List.of(-1, 0, 5, 6, 11, 12, 39, 60, 84, 85, 100)) {
            AtomicInteger calls = new AtomicInteger();
            FadedInteractionHandler.classifyTouch(trust, () -> {
                calls.incrementAndGet();
                return true;
            });
            assertEquals(0, calls.get(), "trust=" + trust);
        }

        for (int trust : List.of(40, 41, 59)) {
            AtomicInteger calls = new AtomicInteger();
            FadedInteractionHandler.classifyTouch(trust, () -> {
                calls.incrementAndGet();
                return true;
            });
            assertEquals(1, calls.get(), "trust=" + trust);
        }
    }

    @Test
    void publicApiHasNoMinecraftEntityWorldOrRandomDependency() {
        for (Class<?> type : List.of(FadedInteractionHandler.class,
                FadedInteractionHandler.HeldItem.class,
                FadedInteractionHandler.InteractionIntent.class,
                FadedInteractionHandler.TouchIntent.class,
                FadedInteractionHandler.InteractionInput.class)) {
            assertFalse(hasForbiddenType(type), type.getName());
        }
        for (Method method : FadedInteractionHandler.class.getDeclaredMethods()) {
            assertFalse(forbidden(method.getReturnType()), method.toString());
            for (Class<?> parameter : method.getParameterTypes())
                assertFalse(forbidden(parameter), method.toString());
        }
    }

    private static boolean hasForbiddenType(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream().anyMatch(field -> forbidden(field.getType()));
    }

    private static boolean forbidden(Class<?> type) {
        return type.getName().startsWith("net.minecraft") || type == Random.class;
    }

    private static FadedInteractionHandler.InteractionIntent classify(
            FadedInteractionHandler.InteractionInput input) {
        return FadedInteractionHandler.classify(input);
    }

    private static FadedInteractionHandler.TouchIntent touch(int trust, boolean choice) {
        return FadedInteractionHandler.classifyTouch(trust, () -> choice);
    }

    private static FadedInteractionHandler.InteractionInput wounded(boolean pearlGiven,
                                                                     FadedInteractionHandler.HeldItem held) {
        return input(true, false, true, false, pearlGiven, false, false, false, held);
    }

    private static FadedInteractionHandler.InteractionInput healed(boolean friend, boolean downed,
                                                                    boolean crouching,
                                                                    FadedInteractionHandler.HeldItem held) {
        return input(true, false, true, true, false, friend, downed, crouching, held);
    }

    private static FadedInteractionHandler.InteractionInput input(boolean mainHand, boolean clientSide,
                                                                   boolean serverPlayer, boolean healed,
                                                                   boolean pearlGiven, boolean friend,
                                                                   boolean downed, boolean crouching,
                                                                   FadedInteractionHandler.HeldItem held) {
        return new FadedInteractionHandler.InteractionInput(mainHand, clientSide, serverPlayer, healed,
                pearlGiven, friend, downed, crouching, held);
    }
}
