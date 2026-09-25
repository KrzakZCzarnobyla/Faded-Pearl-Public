package pl.fadedpearl.entity.animation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.fadedpearl.entity.animation.FadedAnimationState.AnimationIntent;
import static pl.fadedpearl.entity.animation.FadedAnimationState.CarryAction;
import static pl.fadedpearl.entity.animation.FadedAnimationState.CompanionCommand;
import static pl.fadedpearl.entity.animation.FadedAnimationState.Snapshot;
import static pl.fadedpearl.entity.animation.FadedAnimationState.SocialAction;

class FadedAnimationStateTest {
    @Test
    void woundedStateIgnoresEveryHealedPriority() {
        assertEquals(AnimationIntent.SIT, resolve(false, false, false));
        assertEquals(AnimationIntent.NOTICE, resolve(false, false, true));
        assertEquals(AnimationIntent.CRY, resolve(false, true, false));
        assertEquals(AnimationIntent.QUIET_CRY, resolve(false, true, true));

        Snapshot allLowerPriorities = new Snapshot(false, true, false, true, CarryAction.PICKING_UP,
                true, true, CompanionCommand.REST, SocialAction.JUMP_REACT, true, true);
        assertEquals(AnimationIntent.CRY, FadedAnimationState.resolve(allLowerPriorities));
    }

    @Test
    void healingWinsEveryLowerPriority() {
        Snapshot snapshot = new Snapshot(true, false, false, true, CarryAction.PICKING_UP,
                true, true, CompanionCommand.REST, SocialAction.JUMP_REACT, true, true);
        assertEquals(AnimationIntent.HEALING, FadedAnimationState.resolve(snapshot));
    }

    @Test
    void carryPriorityAndMotionMatchBaseline() {
        assertEquals(AnimationIntent.PICK_UP_PLAYER, resolve(CarryAction.PICKING_UP, true, true));
        assertEquals(AnimationIntent.PUT_DOWN_PLAYER, resolve(CarryAction.PUTTING_DOWN, true, true));
        assertEquals(AnimationIntent.CARRY_IDLE, resolve(CarryAction.CARRYING, false, false));
        assertEquals(AnimationIntent.CARRY_WALK, resolve(CarryAction.CARRYING, false, true));
        assertEquals(AnimationIntent.CARRY_IDLE, resolve(CarryAction.NONE, true, false));
        assertEquals(AnimationIntent.CARRY_WALK, resolve(CarryAction.NONE, true, true));
    }

    @Test
    void downedAndRestCommandWinOverSocialActions() {
        Snapshot downed = baseline(SocialAction.JUMP_REACT, true, true, CompanionCommand.REST, true);
        assertEquals(AnimationIntent.DOWNED_IDLE, FadedAnimationState.resolve(downed));

        Snapshot rest = baseline(SocialAction.JUMP_REACT, true, true, CompanionCommand.REST, false);
        assertEquals(AnimationIntent.REST, FadedAnimationState.resolve(rest));
    }

    @Test
    void everySocialActionHasItsBaselineStationaryMapping() {
        Map<SocialAction, AnimationIntent> expected = Map.ofEntries(
                Map.entry(SocialAction.NONE, AnimationIntent.IDLE),
                Map.entry(SocialAction.CURIOUS, AnimationIntent.CURIOUS),
                Map.entry(SocialAction.LOOK_AROUND, AnimationIntent.LOOK_AROUND),
                Map.entry(SocialAction.FLOWER_INTEREST, AnimationIntent.FLOWER_INTEREST),
                Map.entry(SocialAction.HELD_ITEM, AnimationIntent.HELD_ITEM),
                Map.entry(SocialAction.PLAYER_GESTURE, AnimationIntent.PLAYER_GESTURE),
                Map.entry(SocialAction.RAIN_NERVOUS, AnimationIntent.RAIN_NERVOUS),
                Map.entry(SocialAction.WORRIED, AnimationIntent.WORRIED),
                Map.entry(SocialAction.GROUND_FLOWER, AnimationIntent.GROUND_FLOWER),
                Map.entry(SocialAction.REST_NEAR, AnimationIntent.REST),
                Map.entry(SocialAction.TOUCH_WOUND, AnimationIntent.TOUCH_WOUND),
                Map.entry(SocialAction.PEEK_CORNER, AnimationIntent.PEEK),
                Map.entry(SocialAction.PLAYFUL_TELEPORT, AnimationIntent.IDLE),
                Map.entry(SocialAction.NIGHT_CLOSE, AnimationIntent.IDLE),
                Map.entry(SocialAction.SOUND_ALERT, AnimationIntent.HOSTILE_WARNING),
                Map.entry(SocialAction.BRING_FLOWER, AnimationIntent.FLOWER_INTEREST),
                Map.entry(SocialAction.AFFECTION, AnimationIntent.AFFECTION),
                Map.entry(SocialAction.HUG, AnimationIntent.HUG),
                Map.entry(SocialAction.GUARD, AnimationIntent.WORRIED),
                Map.entry(SocialAction.JUMP_REACT, AnimationIntent.JUMP_REACT),
                Map.entry(SocialAction.CROUCH, AnimationIntent.CROUCH),
                Map.entry(SocialAction.STARE_FREEZE, AnimationIntent.STARE_FREEZE),
                Map.entry(SocialAction.STARE_TILT, AnimationIntent.STARE_TILT),
                Map.entry(SocialAction.TOUCH_RECOIL, AnimationIntent.TOUCH_RECOIL),
                Map.entry(SocialAction.TOUCH_HESITATE, AnimationIntent.TOUCH_HESITATE),
                Map.entry(SocialAction.RAIN_SHELTER, AnimationIntent.RAIN_SHELTER),
                Map.entry(SocialAction.RAIN_SHIVER, AnimationIntent.RAIN_SHIVER),
                Map.entry(SocialAction.DOWNED_RECOVER, AnimationIntent.DOWNED_RECOVER),
                Map.entry(SocialAction.CHEST_EXPOSE, AnimationIntent.CHEST_EXPOSE),
                Map.entry(SocialAction.EMBRACE_READY, AnimationIntent.EMBRACE_READY),
                Map.entry(SocialAction.NIGHT_GAZE, AnimationIntent.NIGHT_GAZE),
                Map.entry(SocialAction.SNOW_CATCH, AnimationIntent.SNOW_CATCH),
                Map.entry(SocialAction.WATCH_SLEEPING, AnimationIntent.WATCH_SLEEPING),
                Map.entry(SocialAction.ITEM_POINT, AnimationIntent.ITEM_POINT),
                Map.entry(SocialAction.ITEM_INSPECT, AnimationIntent.ITEM_INSPECT));

        assertEquals(SocialAction.values().length, expected.size());
        for (SocialAction action : SocialAction.values()) {
            assertEquals(expected.get(action), FadedAnimationState.resolve(baseline(action, false, false,
                    CompanionCommand.FOLLOW, false)), action.name());
        }
    }

    @Test
    void runSoundAlertAndMovementPreserveTheirOrder() {
        assertEquals(AnimationIntent.RUN, FadedAnimationState.resolve(
                baseline(SocialAction.SOUND_ALERT, true, true, CompanionCommand.FOLLOW, false)));
        assertEquals(AnimationIntent.HOSTILE_WARNING, FadedAnimationState.resolve(
                baseline(SocialAction.SOUND_ALERT, false, true, CompanionCommand.FOLLOW, false)));
        assertEquals(AnimationIntent.WALK, FadedAnimationState.resolve(
                baseline(SocialAction.GUARD, false, true, CompanionCommand.FOLLOW, false)));
        assertEquals(AnimationIntent.WORRIED, FadedAnimationState.resolve(
                baseline(SocialAction.GUARD, false, false, CompanionCommand.FOLLOW, false)));
        assertEquals(AnimationIntent.REST, FadedAnimationState.resolve(
                baseline(SocialAction.REST_NEAR, false, false, CompanionCommand.FOLLOW, false)));
    }

    @Test
    void specialActionsStillWinOverRun() {
        for (SocialAction action : new SocialAction[]{SocialAction.JUMP_REACT, SocialAction.CROUCH,
                SocialAction.STARE_FREEZE, SocialAction.STARE_TILT, SocialAction.TOUCH_RECOIL,
                SocialAction.TOUCH_HESITATE, SocialAction.RAIN_SHELTER, SocialAction.RAIN_SHIVER,
                SocialAction.DOWNED_RECOVER, SocialAction.CHEST_EXPOSE, SocialAction.EMBRACE_READY,
                SocialAction.NIGHT_GAZE, SocialAction.SNOW_CATCH, SocialAction.WATCH_SLEEPING}) {
            assertFalse(FadedAnimationState.resolve(
                    baseline(action, true, true, CompanionCommand.FOLLOW, false)) == AnimationIntent.RUN,
                    action.name());
        }
    }

    @Test
    void rejectsIncompleteSnapshots() {
        assertThrows(IllegalArgumentException.class, () -> new Snapshot(true, false, false, false,
                null, false, false, CompanionCommand.FOLLOW, SocialAction.NONE, false, false));
    }

    @Test
    void resolverBytecodeHasNoMinecraftGeckoLibOrRandomDependency() throws IOException {
        assertPureBytecode(FadedAnimationState.class);
        for (Class<?> nested : FadedAnimationState.class.getDeclaredClasses()) {
            assertPureBytecode(nested);
        }
    }

    private static void assertPureBytecode(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing class resource " + resource);
            String constantPool = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(constantPool.contains("net/minecraft"), type.getName());
            assertFalse(constantPool.contains("software/bernie"), type.getName());
            assertFalse(constantPool.contains("java/util/Random"), type.getName());
        }
    }

    private static AnimationIntent resolve(boolean healed, boolean crying, boolean nearby) {
        return FadedAnimationState.resolve(new Snapshot(healed, crying, nearby, false, CarryAction.NONE,
                false, false, CompanionCommand.FOLLOW, SocialAction.NONE, false, false));
    }

    private static AnimationIntent resolve(CarryAction carryAction, boolean passenger, boolean moving) {
        return FadedAnimationState.resolve(new Snapshot(true, false, false, false, carryAction,
                passenger, true, CompanionCommand.REST, SocialAction.JUMP_REACT, true, moving));
    }

    private static Snapshot baseline(SocialAction action, boolean running, boolean moving,
                                     CompanionCommand command, boolean downed) {
        return new Snapshot(true, false, false, false, CarryAction.NONE, false, downed,
                command, action, running, moving);
    }
}
