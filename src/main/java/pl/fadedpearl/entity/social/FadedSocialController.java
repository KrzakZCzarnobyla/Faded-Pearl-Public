package pl.fadedpearl.entity.social;

import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Pure priority and idle resolver. World reads and all effects remain in the entity facade. */
public final class FadedSocialController {
    public enum Intent {
        RESET,
        WATER_PAUSE,
        SOUND_ALERT,
        DELIVER_FLOWER,
        CONTINUE_WORRIED,
        RAIN_NERVOUS,
        HOSTILE_WARNING,
        GROUND_FLOWER,
        WATCH_SLEEPING,
        NIGHT_CLOSE,
        FLOWER_HELD,
        PLAYER_GESTURE,
        HELD_ITEM,
        CONTINUE_ACTION,
        TRY_WORLD_CURIOSITY,
        IDLE_BLOCKED,
        START_IDLE_ACTION
    }

    public enum IdleAction {
        CURIOUS,
        LOOK_AROUND,
        TOUCH_WOUND,
        PEEK_CORNER,
        REST_NEAR,
        PLAYFUL_TELEPORT,
        AFFECTION
    }

    public record Snapshot(
            boolean friendMissing,
            boolean friendTooFar,
            boolean downed,
            boolean restCommand,
            boolean homeCommand,
            boolean waterPause,
            boolean friendHurt,
            boolean soundAlert,
            boolean carriedFlower,
            boolean worriedActive,
            boolean raining,
            boolean hostileWarning,
            boolean droppedFlower,
            boolean sleeping,
            boolean nightClose,
            boolean flowerHeld,
            boolean playerGesture,
            boolean heldItem,
            boolean activeAction,
            boolean worldCuriosityReady,
            boolean idleBlocked
    ) {}

    public record Decision(boolean friendHurt, Intent intent) {}

    public record IdleDecision(Intent intent, IdleAction action, boolean consumeTimingRng) {
        static IdleDecision blocked() {
            return new IdleDecision(Intent.IDLE_BLOCKED, null, false);
        }

        static IdleDecision started(IdleAction action) {
            return new IdleDecision(Intent.START_IDLE_ACTION, action, true);
        }
    }

    public static Decision resolve(Snapshot state) {
        if (state.friendMissing() || state.friendTooFar() || state.downed()
                || state.restCommand() || state.homeCommand()) return new Decision(false, Intent.RESET);
        if (state.waterPause()) return new Decision(false, Intent.WATER_PAUSE);

        boolean hurt = state.friendHurt();
        if (state.soundAlert()) return new Decision(hurt, Intent.SOUND_ALERT);
        if (state.carriedFlower()) return new Decision(hurt, Intent.DELIVER_FLOWER);
        if (hurt || state.worriedActive()) return new Decision(hurt, Intent.CONTINUE_WORRIED);
        if (state.hostileWarning()) return new Decision(false, Intent.HOSTILE_WARNING);
        if (state.raining()) return new Decision(false, Intent.RAIN_NERVOUS);
        if (state.droppedFlower()) return new Decision(false, Intent.GROUND_FLOWER);
        if (state.sleeping()) return new Decision(false, Intent.WATCH_SLEEPING);
        if (state.nightClose()) return new Decision(false, Intent.NIGHT_CLOSE);
        if (state.flowerHeld()) return new Decision(false, Intent.FLOWER_HELD);
        if (state.playerGesture()) return new Decision(false, Intent.PLAYER_GESTURE);
        if (state.heldItem()) return new Decision(false, Intent.HELD_ITEM);
        if (state.activeAction()) return new Decision(false, Intent.CONTINUE_ACTION);
        if (state.worldCuriosityReady()) return new Decision(false, Intent.TRY_WORLD_CURIOSITY);
        return new Decision(false, state.idleBlocked() ? Intent.IDLE_BLOCKED : Intent.START_IDLE_ACTION);
    }

    public static IdleDecision resolveIdle(boolean blocked, int trust, boolean followCommand,
                                           IntSupplier choiceSupplier) {
        if (blocked) return IdleDecision.blocked();
        int choice = Objects.requireNonNull(choiceSupplier, "choiceSupplier").getAsInt();
        return IdleDecision.started(mapIdle(choice, trust, followCommand));
    }

    public static IdleAction mapIdle(int choice, int trust, boolean followCommand) {
        IdleAction action = switch (choice) {
            case 0 -> IdleAction.CURIOUS;
            case 1 -> IdleAction.LOOK_AROUND;
            case 2 -> IdleAction.TOUCH_WOUND;
            case 3 -> IdleAction.PEEK_CORNER;
            case 4 -> IdleAction.REST_NEAR;
            case 5 -> IdleAction.PLAYFUL_TELEPORT;
            case 6 -> IdleAction.AFFECTION;
            default -> throw new IllegalArgumentException("Idle choice must be in 0..6: " + choice);
        };
        if (action == IdleAction.TOUCH_WOUND && !FadedTrustManager.allowsTouchWound(trust))
            return IdleAction.CURIOUS;
        if (action == IdleAction.AFFECTION && !FadedTrustManager.allowsAffection(trust))
            return IdleAction.LOOK_AROUND;
        if (action == IdleAction.PLAYFUL_TELEPORT && !followCommand)
            return IdleAction.LOOK_AROUND;
        return action;
    }

    private FadedSocialController() {}
}
