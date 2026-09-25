package pl.fadedpearl.entity.interaction;

import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Pure classification for player interactions with the Faded Enderman. */
public final class FadedInteractionHandler {
    private FadedInteractionHandler() {}

    public enum HeldItem {
        PULSATING_PEARL,
        EMPTY_PEARL,
        ESCAPE_PEARL,
        SMALL_FLOWER,
        EMPTY_HAND,
        OTHER
    }

    public enum InteractionIntent {
        PASS,
        CLIENT_SUCCESS,
        HEAL_WITH_PULSATING_PEARL,
        EMPTY_PEARL_SILENCE,
        FIRST_MEETING_PEARL,
        RECOVER_DOWNED,
        FLOWER_INTERACTION,
        EQUIP_ESCAPE_PEARL,
        OPEN_COMMAND_MENU,
        EMPTY_HAND_INTERACTION,
        STRANGER_NEUTRAL_REACTION
    }

    public enum TouchIntent {
        TOUCH_RECOIL,
        TOUCH_HESITATE,
        LOOK_AROUND,
        AFFECTION,
        CURIOUS,
        CHEST_EXPOSE,
        EMBRACE_READY
    }

    public record InteractionInput(boolean mainHand, boolean clientSide, boolean serverPlayer,
                                   boolean healed, boolean pearlGiven, boolean friend,
                                   boolean downed, boolean crouching, HeldItem heldItem) {
        public InteractionInput {
            Objects.requireNonNull(heldItem, "heldItem");
        }
    }

    public static InteractionIntent classify(InteractionInput input) {
        Objects.requireNonNull(input, "input");
        if (!input.mainHand()) return InteractionIntent.PASS;
        if (input.clientSide()) return InteractionIntent.CLIENT_SUCCESS;
        if (!input.serverPlayer()) return InteractionIntent.PASS;

        if (!input.healed()) {
            if (input.heldItem() == HeldItem.PULSATING_PEARL)
                return InteractionIntent.HEAL_WITH_PULSATING_PEARL;
            if (input.heldItem() == HeldItem.EMPTY_PEARL)
                return InteractionIntent.EMPTY_PEARL_SILENCE;
            return input.pearlGiven() ? InteractionIntent.PASS : InteractionIntent.FIRST_MEETING_PEARL;
        }

        if (!input.friend())
            return !input.downed() && input.heldItem() == HeldItem.EMPTY_HAND
                    ? InteractionIntent.STRANGER_NEUTRAL_REACTION
                    : InteractionIntent.PASS;
        if (input.downed()) return InteractionIntent.RECOVER_DOWNED;
        if (input.heldItem() == HeldItem.ESCAPE_PEARL) return InteractionIntent.EQUIP_ESCAPE_PEARL;
        if (input.heldItem() == HeldItem.SMALL_FLOWER) return InteractionIntent.FLOWER_INTERACTION;
        if (input.heldItem() == HeldItem.EMPTY_HAND)
            return input.crouching() ? InteractionIntent.OPEN_COMMAND_MENU
                    : InteractionIntent.EMPTY_HAND_INTERACTION;
        return InteractionIntent.PASS;
    }

    public static TouchIntent classifyTouch(int trust, BooleanSupplier affectionChoice) {
        Objects.requireNonNull(affectionChoice, "affectionChoice");
        if (FadedTrustManager.usesTouchRecoil(trust)) return TouchIntent.TOUCH_RECOIL;
        if (FadedTrustManager.usesTouchHesitation(trust)) return TouchIntent.TOUCH_HESITATE;
        if (FadedTrustManager.usesTouchLearning(trust)) return TouchIntent.LOOK_AROUND;
        if (FadedTrustManager.usesTouchDevotion(trust)) return TouchIntent.EMBRACE_READY;
        if (FadedTrustManager.usesTouchChestExpose(trust)) return TouchIntent.CHEST_EXPOSE;
        return affectionChoice.getAsBoolean() ? TouchIntent.AFFECTION : TouchIntent.CURIOUS;
    }
}
