package pl.fadedpearl.entity.dialogue;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/** Pure selection policy for relationship-memory dialogue after a completed shared experience. */
public final class FadedRelationshipDialoguePolicy {
    public static final int BONDED_MIN_TRUST = 60;
    public static final int REPEAT_ROLL_BOUND = 3;

    public enum Context {
        KNOWLEDGE,
        CRAFTSMANSHIP,
        VILLAGE,
        PLAYER_DIAMOND,
        ENDERMAN_DIAMOND,
        TAMED_WOLF,
        TAMED_CAT,
        TAMED_PARROT,
        TAMED_OTHER,
        BUILD,
        ARMOR_UPGRADE,
        NIGHT_WATCH
    }

    public enum Selection {
        NONE,
        KNOWLEDGE_FIRST,
        KNOWLEDGE_REPEAT_LEARNING,
        KNOWLEDGE_REPEAT_BONDED,
        CRAFT_FIRST,
        CRAFT_REPEAT_LEARNING,
        CRAFT_REPEAT_BONDED,
        VILLAGE_FIRST,
        VILLAGE_REPEAT_LEARNING,
        VILLAGE_REPEAT_BONDED,
        PLAYER_DIAMOND_FIRST,
        PLAYER_DIAMOND_REPEAT_LEARNING,
        PLAYER_DIAMOND_REPEAT_BONDED,
        ENDERMAN_DIAMOND_FIRST,
        ENDERMAN_DIAMOND_REPEAT_LEARNING,
        ENDERMAN_DIAMOND_REPEAT_BONDED,
        TAMED_WOLF_FIRST,
        TAMED_WOLF_REPEAT_LEARNING,
        TAMED_WOLF_REPEAT_BONDED,
        TAMED_CAT_FIRST,
        TAMED_CAT_REPEAT_LEARNING,
        TAMED_CAT_REPEAT_BONDED,
        TAMED_PARROT_FIRST,
        TAMED_PARROT_REPEAT_LEARNING,
        TAMED_PARROT_REPEAT_BONDED,
        TAMED_OTHER_FIRST,
        TAMED_OTHER_REPEAT_LEARNING,
        TAMED_OTHER_REPEAT_BONDED,
        BUILD_FIRST,
        BUILD_REPEAT_LEARNING,
        BUILD_REPEAT_BONDED,
        ARMOR_UPGRADE_FIRST,
        ARMOR_UPGRADE_REPEAT_LEARNING,
        ARMOR_UPGRADE_REPEAT_BONDED,
        NIGHT_WATCH_FIRST,
        NIGHT_WATCH_REPEAT_LEARNING,
        NIGHT_WATCH_REPEAT_BONDED
    }

    /**
     * Selects a dialogue family for a shared experience which has already occurred.
     * The random source is read exactly once, with bound {@value #REPEAT_ROLL_BOUND}, only for
     * repeated experiences owned by a present player. A roll other than zero suppresses dialogue.
     */
    public static Selection select(
            Context context,
            boolean ownerPresent,
            boolean firstDiscovery,
            int trust,
            IntUnaryOperator boundedRandom
    ) {
        if (!ownerPresent) return Selection.NONE;

        Objects.requireNonNull(context, "context");
        if (firstDiscovery) return firstSelection(context);

        int roll = Objects.requireNonNull(boundedRandom, "boundedRandom").applyAsInt(REPEAT_ROLL_BOUND);
        if (roll < 0 || roll >= REPEAT_ROLL_BOUND) {
            throw new IllegalArgumentException("Relationship dialogue roll must be in 0..2: " + roll);
        }
        if (roll != 0) return Selection.NONE;

        boolean bonded = trust >= BONDED_MIN_TRUST;
        return switch (context) {
            case KNOWLEDGE -> bonded
                    ? Selection.KNOWLEDGE_REPEAT_BONDED
                    : Selection.KNOWLEDGE_REPEAT_LEARNING;
            case CRAFTSMANSHIP -> bonded
                    ? Selection.CRAFT_REPEAT_BONDED
                    : Selection.CRAFT_REPEAT_LEARNING;
            case VILLAGE -> bonded
                    ? Selection.VILLAGE_REPEAT_BONDED
                    : Selection.VILLAGE_REPEAT_LEARNING;
            case PLAYER_DIAMOND -> bonded
                    ? Selection.PLAYER_DIAMOND_REPEAT_BONDED
                    : Selection.PLAYER_DIAMOND_REPEAT_LEARNING;
            case ENDERMAN_DIAMOND -> bonded
                    ? Selection.ENDERMAN_DIAMOND_REPEAT_BONDED
                    : Selection.ENDERMAN_DIAMOND_REPEAT_LEARNING;
            case TAMED_WOLF -> bonded
                    ? Selection.TAMED_WOLF_REPEAT_BONDED
                    : Selection.TAMED_WOLF_REPEAT_LEARNING;
            case TAMED_CAT -> bonded
                    ? Selection.TAMED_CAT_REPEAT_BONDED
                    : Selection.TAMED_CAT_REPEAT_LEARNING;
            case TAMED_PARROT -> bonded
                    ? Selection.TAMED_PARROT_REPEAT_BONDED
                    : Selection.TAMED_PARROT_REPEAT_LEARNING;
            case TAMED_OTHER -> bonded
                    ? Selection.TAMED_OTHER_REPEAT_BONDED
                    : Selection.TAMED_OTHER_REPEAT_LEARNING;
            case BUILD -> bonded
                    ? Selection.BUILD_REPEAT_BONDED
                    : Selection.BUILD_REPEAT_LEARNING;
            case ARMOR_UPGRADE -> bonded
                    ? Selection.ARMOR_UPGRADE_REPEAT_BONDED
                    : Selection.ARMOR_UPGRADE_REPEAT_LEARNING;
            case NIGHT_WATCH -> bonded
                    ? Selection.NIGHT_WATCH_REPEAT_BONDED
                    : Selection.NIGHT_WATCH_REPEAT_LEARNING;
        };
    }

    private static Selection firstSelection(Context context) {
        return switch (context) {
            case KNOWLEDGE -> Selection.KNOWLEDGE_FIRST;
            case CRAFTSMANSHIP -> Selection.CRAFT_FIRST;
            case VILLAGE -> Selection.VILLAGE_FIRST;
            case PLAYER_DIAMOND -> Selection.PLAYER_DIAMOND_FIRST;
            case ENDERMAN_DIAMOND -> Selection.ENDERMAN_DIAMOND_FIRST;
            case TAMED_WOLF -> Selection.TAMED_WOLF_FIRST;
            case TAMED_CAT -> Selection.TAMED_CAT_FIRST;
            case TAMED_PARROT -> Selection.TAMED_PARROT_FIRST;
            case TAMED_OTHER -> Selection.TAMED_OTHER_FIRST;
            case BUILD -> Selection.BUILD_FIRST;
            case ARMOR_UPGRADE -> Selection.ARMOR_UPGRADE_FIRST;
            case NIGHT_WATCH -> Selection.NIGHT_WATCH_FIRST;
        };
    }

    private FadedRelationshipDialoguePolicy() {}
}
