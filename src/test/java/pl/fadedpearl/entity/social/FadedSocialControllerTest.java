package pl.fadedpearl.entity.social;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.social.FadedSocialController.IdleAction.*;
import static pl.fadedpearl.entity.social.FadedSocialController.Intent.*;

class FadedSocialControllerTest {
    @Test
    void resolvesEveryTerminalPriorityInBaselineOrder() {
        boolean[] all = new boolean[21];
        java.util.Arrays.fill(all, 5, all.length, true);
        assertEquals(WATER_PAUSE, resolve(all).intent());
        all[5] = false; assertEquals(SOUND_ALERT, resolve(all).intent());
        all[7] = false; assertEquals(DELIVER_FLOWER, resolve(all).intent());
        all[8] = false; assertEquals(CONTINUE_WORRIED, resolve(all).intent());
        all[6] = false; all[9] = false; assertEquals(HOSTILE_WARNING, resolve(all).intent());
        all[11] = false; assertEquals(RAIN_NERVOUS, resolve(all).intent());
        all[10] = false; assertEquals(GROUND_FLOWER, resolve(all).intent());
        all[12] = false; assertEquals(WATCH_SLEEPING, resolve(all).intent());
        all[13] = false; assertEquals(NIGHT_CLOSE, resolve(all).intent());
        all[14] = false; assertEquals(FLOWER_HELD, resolve(all).intent());
        all[15] = false; assertEquals(PLAYER_GESTURE, resolve(all).intent());
        all[16] = false; assertEquals(HELD_ITEM, resolve(all).intent());
        all[17] = false; assertEquals(CONTINUE_ACTION, resolve(all).intent());
        all[18] = false; assertEquals(TRY_WORLD_CURIOSITY, resolve(all).intent());
        all[19] = false; assertEquals(IDLE_BLOCKED, resolve(all).intent());
        all[20] = false; assertEquals(START_IDLE_ACTION, resolve(all).intent());
    }

    @Test
    void nearbyHostileOutranksRainReaction() {
        boolean[] state = new boolean[21];
        state[10] = true;
        state[11] = true;
        assertEquals(HOSTILE_WARNING, resolve(state).intent());
    }

    @Test
    void everyResetCauseWinsOverAllSocialStimuli() {
        for (int resetIndex = 0; resetIndex < 5; resetIndex++) {
            boolean[] state = new boolean[21];
            java.util.Arrays.fill(state, 5, state.length, true);
            state[resetIndex] = true;
            assertEquals(RESET, resolve(state).intent());
        }
    }

    @Test
    void friendHurtIsPreludeButSoundAndFlowerRemainTerminallyHigherThanWorried() {
        boolean[] state = new boolean[21];
        state[6] = true; state[7] = true;
        FadedSocialController.Decision sound = resolve(state);
        assertTrue(sound.friendHurt()); assertEquals(SOUND_ALERT, sound.intent());
        state[7] = false; state[8] = true;
        FadedSocialController.Decision flower = resolve(state);
        assertTrue(flower.friendHurt()); assertEquals(DELIVER_FLOWER, flower.intent());
        state[8] = false;
        assertEquals(CONTINUE_WORRIED, resolve(state).intent());
    }

    @Test
    void mapsAllIdleChoicesAndFallbacksAtExactTrustThresholds() {
        assertEquals(CURIOUS, FadedSocialController.mapIdle(0, 100, true));
        assertEquals(LOOK_AROUND, FadedSocialController.mapIdle(1, 100, true));
        assertEquals(CURIOUS, FadedSocialController.mapIdle(2, 59, true));
        assertEquals(TOUCH_WOUND, FadedSocialController.mapIdle(2, 60, true));
        assertEquals(PEEK_CORNER, FadedSocialController.mapIdle(3, 100, true));
        assertEquals(REST_NEAR, FadedSocialController.mapIdle(4, 100, true));
        assertEquals(PLAYFUL_TELEPORT, FadedSocialController.mapIdle(5, 100, true));
        assertEquals(LOOK_AROUND, FadedSocialController.mapIdle(5, 100, false));
        assertEquals(LOOK_AROUND, FadedSocialController.mapIdle(6, 34, true));
        assertEquals(AFFECTION, FadedSocialController.mapIdle(6, 35, true));
        assertThrows(IllegalArgumentException.class, () -> FadedSocialController.mapIdle(7, 100, true));
    }

    @Test
    void consumesChoiceOnlyWhenIdleCanStartAndExposesTimingRngContract() {
        AtomicInteger calls = new AtomicInteger();
        FadedSocialController.IdleDecision blocked = FadedSocialController.resolveIdle(true, 100, true, () -> {
            calls.incrementAndGet(); return 0;
        });
        assertEquals(IDLE_BLOCKED, blocked.intent());
        assertFalse(blocked.consumeTimingRng());
        assertEquals(0, calls.get());

        FadedSocialController.IdleDecision started = FadedSocialController.resolveIdle(false, 100, true, () -> {
            calls.incrementAndGet(); return 3;
        });
        assertEquals(START_IDLE_ACTION, started.intent());
        assertEquals(PEEK_CORNER, started.action());
        assertTrue(started.consumeTimingRng());
        assertEquals(1, calls.get());
    }

    private static FadedSocialController.Decision resolve(boolean[] v) {
        return FadedSocialController.resolve(new FadedSocialController.Snapshot(
                v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10],
                v[11], v[12], v[13], v[14], v[15], v[16], v[17], v[18], v[19], v[20]));
    }
}
