package pl.fadedpearl.entity.animation;

/** Pure priority resolver for the Faded Enderman's animation controller. */
public final class FadedAnimationState {
    private FadedAnimationState() {}

    public enum AnimationIntent {
        SIT, CRY, QUIET_CRY, NOTICE, IDLE, WALK, RUN, REST, CURIOUS, LOOK_AROUND,
        FLOWER_INTEREST, HELD_ITEM, PLAYER_GESTURE, HEALING, TOUCH_WOUND, HUG, PEEK,
        AFFECTION, RAIN_NERVOUS, WORRIED, HOSTILE_WARNING, GROUND_FLOWER, PICK_UP_PLAYER, CARRY_IDLE,
        CARRY_WALK, PUT_DOWN_PLAYER, JUMP_REACT, CROUCH, STARE_FREEZE, STARE_TILT,
        TOUCH_RECOIL, TOUCH_HESITATE, RAIN_SHELTER, RAIN_SHIVER, DOWNED_IDLE,
        DOWNED_RECOVER, CHEST_EXPOSE, EMBRACE_READY, NIGHT_GAZE, SNOW_CATCH,
        WATCH_SLEEPING, ITEM_POINT, ITEM_INSPECT
    }

    public enum CompanionCommand { FOLLOW, STAY, REST, HOME }
    public enum CarryAction { NONE, PICKING_UP, CARRYING, PUTTING_DOWN }

    public enum SocialAction {
        NONE, CURIOUS, LOOK_AROUND, FLOWER_INTEREST, HELD_ITEM, PLAYER_GESTURE,
        RAIN_NERVOUS, WORRIED, GROUND_FLOWER, REST_NEAR, TOUCH_WOUND, PEEK_CORNER,
        PLAYFUL_TELEPORT, NIGHT_CLOSE, SOUND_ALERT, BRING_FLOWER, AFFECTION, HUG, GUARD,
        JUMP_REACT, CROUCH, STARE_FREEZE, STARE_TILT, TOUCH_RECOIL, TOUCH_HESITATE,
        RAIN_SHELTER, RAIN_SHIVER, DOWNED_RECOVER, CHEST_EXPOSE, EMBRACE_READY,
        NIGHT_GAZE, SNOW_CATCH, WATCH_SLEEPING, ITEM_POINT, ITEM_INSPECT
    }

    public record Snapshot(boolean healed, boolean crying, boolean playerNearby, boolean healing,
                           CarryAction carryAction, boolean carryingPassenger, boolean downed,
                           CompanionCommand command, SocialAction socialAction,
                           boolean running, boolean moving) {
        public Snapshot {
            if (carryAction == null || command == null || socialAction == null) {
                throw new IllegalArgumentException("Animation snapshot enum values cannot be null");
            }
        }
    }

    public static AnimationIntent resolve(Snapshot snapshot) {
        if (!snapshot.healed()) {
            if (snapshot.crying()) {
                return snapshot.playerNearby() ? AnimationIntent.QUIET_CRY : AnimationIntent.CRY;
            }
            return snapshot.playerNearby() ? AnimationIntent.NOTICE : AnimationIntent.SIT;
        }
        if (snapshot.healing()) return AnimationIntent.HEALING;
        if (snapshot.carryAction() == CarryAction.PICKING_UP) return AnimationIntent.PICK_UP_PLAYER;
        if (snapshot.carryAction() == CarryAction.PUTTING_DOWN) return AnimationIntent.PUT_DOWN_PLAYER;
        if (snapshot.carryingPassenger() || snapshot.carryAction() == CarryAction.CARRYING) {
            return snapshot.moving() ? AnimationIntent.CARRY_WALK : AnimationIntent.CARRY_IDLE;
        }
        if (snapshot.downed()) return AnimationIntent.DOWNED_IDLE;
        if (snapshot.command() == CompanionCommand.REST) return AnimationIntent.REST;

        AnimationIntent special = specialAction(snapshot.socialAction());
        if (special != null) return special;
        if (snapshot.running()) return AnimationIntent.RUN;
        if (snapshot.socialAction() == SocialAction.SOUND_ALERT) return AnimationIntent.HOSTILE_WARNING;
        if (snapshot.moving()) return AnimationIntent.WALK;

        AnimationIntent stationary = stationaryAction(snapshot.socialAction());
        return stationary != null ? stationary : AnimationIntent.IDLE;
    }

    private static AnimationIntent specialAction(SocialAction action) {
        return switch (action) {
            case JUMP_REACT -> AnimationIntent.JUMP_REACT;
            case CROUCH -> AnimationIntent.CROUCH;
            case STARE_FREEZE -> AnimationIntent.STARE_FREEZE;
            case STARE_TILT -> AnimationIntent.STARE_TILT;
            case TOUCH_RECOIL -> AnimationIntent.TOUCH_RECOIL;
            case TOUCH_HESITATE -> AnimationIntent.TOUCH_HESITATE;
            case RAIN_SHELTER -> AnimationIntent.RAIN_SHELTER;
            case RAIN_SHIVER -> AnimationIntent.RAIN_SHIVER;
            case DOWNED_RECOVER -> AnimationIntent.DOWNED_RECOVER;
            case CHEST_EXPOSE -> AnimationIntent.CHEST_EXPOSE;
            case EMBRACE_READY -> AnimationIntent.EMBRACE_READY;
            case NIGHT_GAZE -> AnimationIntent.NIGHT_GAZE;
            case SNOW_CATCH -> AnimationIntent.SNOW_CATCH;
            case WATCH_SLEEPING -> AnimationIntent.WATCH_SLEEPING;
            case ITEM_POINT -> AnimationIntent.ITEM_POINT;
            case ITEM_INSPECT -> AnimationIntent.ITEM_INSPECT;
            default -> null;
        };
    }

    private static AnimationIntent stationaryAction(SocialAction action) {
        return switch (action) {
            case FLOWER_INTEREST, BRING_FLOWER -> AnimationIntent.FLOWER_INTEREST;
            case HELD_ITEM -> AnimationIntent.HELD_ITEM;
            case PLAYER_GESTURE -> AnimationIntent.PLAYER_GESTURE;
            case RAIN_NERVOUS -> AnimationIntent.RAIN_NERVOUS;
            case WORRIED, GUARD -> AnimationIntent.WORRIED;
            case GROUND_FLOWER -> AnimationIntent.GROUND_FLOWER;
            case REST_NEAR -> AnimationIntent.REST;
            case TOUCH_WOUND -> AnimationIntent.TOUCH_WOUND;
            case PEEK_CORNER -> AnimationIntent.PEEK;
            case HUG -> AnimationIntent.HUG;
            case AFFECTION -> AnimationIntent.AFFECTION;
            case CURIOUS -> AnimationIntent.CURIOUS;
            case LOOK_AROUND -> AnimationIntent.LOOK_AROUND;
            default -> null;
        };
    }
}
