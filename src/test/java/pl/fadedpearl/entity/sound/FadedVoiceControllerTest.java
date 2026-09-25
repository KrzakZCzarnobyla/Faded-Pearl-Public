package pl.fadedpearl.entity.sound;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.sound.FadedVoiceController.AmbientPose;
import static pl.fadedpearl.entity.sound.FadedVoiceController.AmbientSnapshot;
import static pl.fadedpearl.entity.sound.FadedVoiceController.AmbientVariant;

class FadedVoiceControllerTest {
    @Test
    void woundedNoticeUsesOnlyThePlayerPresenceRisingEdge() {
        assertFalse(FadedVoiceController.shouldPlayWoundedNotice(false, false, true));
        assertFalse(FadedVoiceController.shouldPlayWoundedNotice(true, false, false));
        assertTrue(FadedVoiceController.shouldPlayWoundedNotice(true, false, true));
        assertFalse(FadedVoiceController.shouldPlayWoundedNotice(true, true, true));
        assertFalse(FadedVoiceController.shouldPlayWoundedNotice(true, true, false));
        assertTrue(FadedVoiceController.shouldPlayWoundedNotice(true, false, true));
    }

    @Test
    void recordedCryCoversTheFullVoiceLine() {
        assertEquals(87, FadedVoiceController.CRY_PRESENTATION_TICKS);
    }

    @Test
    void everySafetyGateBlocksAmbient() {
        List<AmbientSnapshot> blocked = List.of(
                snapshot(false, true, false, false, false, false, false, false, false, false, false, false),
                snapshot(true, false, false, false, false, false, false, false, false, false, false, false),
                snapshot(true, true, true, false, false, false, false, false, false, false, false, false),
                snapshot(true, true, false, true, false, false, false, false, false, false, false, false),
                snapshot(true, true, false, false, true, false, false, false, false, false, false, false),
                snapshot(true, true, false, false, false, true, false, false, false, false, false, false),
                snapshot(true, true, false, false, false, false, true, false, false, false, false, false),
                snapshot(true, true, false, false, false, false, false, true, false, false, false, false),
                snapshot(true, true, false, false, false, false, false, false, true, false, false, false),
                snapshot(true, true, false, false, false, false, false, false, false, true, false, false),
                snapshot(true, true, false, false, false, false, false, false, false, false, true, false),
                snapshot(true, true, false, false, false, false, false, false, false, false, false, true));

        assertTrue(FadedVoiceController.isAmbientEligible(eligible()));
        for (AmbientSnapshot snapshot : blocked) {
            assertFalse(FadedVoiceController.isAmbientEligible(snapshot), snapshot.toString());
            AtomicInteger calls = new AtomicInteger();
            var decision = FadedVoiceController.tickAmbient(0, snapshot, 100, bound -> {
                calls.incrementAndGet();
                return 0;
            });
            assertFalse(decision.play(), snapshot.toString());
            assertEquals(0, decision.nextCooldown());
            assertEquals(0, calls.get());
        }
    }

    @Test
    void transientCooldownInitializesCountsDownAndHonorsInclusiveBoundaries() {
        var minimum = FadedVoiceController.tickAmbient(FadedVoiceController.UNINITIALIZED_COOLDOWN,
                eligible(), 100, bound -> 0);
        assertFalse(minimum.play());
        assertEquals(FadedVoiceController.AMBIENT_MIN_COOLDOWN_TICKS, minimum.nextCooldown());

        int spread = FadedVoiceController.AMBIENT_MAX_COOLDOWN_TICKS
                - FadedVoiceController.AMBIENT_MIN_COOLDOWN_TICKS + 1;
        var maximum = FadedVoiceController.tickAmbient(FadedVoiceController.UNINITIALIZED_COOLDOWN,
                eligible(), 100, bound -> {
                    assertEquals(spread, bound);
                    return bound - 1;
                });
        assertEquals(FadedVoiceController.AMBIENT_MAX_COOLDOWN_TICKS, maximum.nextCooldown());

        var counted = FadedVoiceController.tickAmbient(1,
                snapshot(true, false, true, true, true, true, true, true, true, true, true, true),
                0, bound -> fail("Countdown must not consume RNG"));
        assertFalse(counted.play());
        assertEquals(0, counted.nextCooldown());
    }

    @Test
    void oneRandomChoiceCreatesExactlyOneSoundPosePairAndOneNewCooldown() {
        List<Integer> bounds = new ArrayList<>();
        int[] values = {1, 321};
        AtomicInteger index = new AtomicInteger();
        var decision = FadedVoiceController.tickAmbient(0, eligible(), 100, bound -> {
            bounds.add(bound);
            return values[index.getAndIncrement()];
        });

        assertTrue(decision.play());
        assertEquals(AmbientVariant.ENDERMAN_TWO, decision.variant());
        assertEquals(AmbientPose.LOOK_AROUND, decision.pose());
        assertEquals(FadedVoiceController.AMBIENT_MIN_COOLDOWN_TICKS + 321, decision.nextCooldown());
        assertEquals(List.of(3, 2401), bounds);
    }

    @Test
    void everyAmbientVariantHasAStablePoseAndFullAudioDuration() {
        assertAll(
                () -> assertEquals(AmbientPose.CURIOUS,
                        FadedVoiceController.poseFor(AmbientVariant.ENDERMAN_ONE, 100)),
                () -> assertEquals(AmbientPose.LOOK_AROUND,
                        FadedVoiceController.poseFor(AmbientVariant.ENDERMAN_TWO, 100)),
                () -> assertEquals(AmbientPose.CURIOUS,
                        FadedVoiceController.poseFor(AmbientVariant.VOICE_164238, 34)),
                () -> assertEquals(AmbientPose.AFFECTION,
                        FadedVoiceController.poseFor(AmbientVariant.VOICE_164238, 35)),
                () -> assertEquals(65, AmbientVariant.ENDERMAN_ONE.presentationTicks()),
                () -> assertEquals(62, AmbientVariant.ENDERMAN_TWO.presentationTicks()),
                () -> assertEquals(77, AmbientVariant.VOICE_164238.presentationTicks()));
    }

    private static AmbientSnapshot eligible() {
        return snapshot(true, true, false, false, false, false, false, false, false, false, false, false);
    }

    private static AmbientSnapshot snapshot(boolean healed, boolean friendNearby, boolean downed,
                                            boolean healing, boolean rest, boolean social, boolean curiosity,
                                            boolean carrying, boolean combat, boolean protection,
                                            boolean unsafe, boolean moving) {
        return new AmbientSnapshot(healed, friendNearby, downed, healing, rest, social, curiosity,
                carrying, combat, protection, unsafe, moving);
    }
}
