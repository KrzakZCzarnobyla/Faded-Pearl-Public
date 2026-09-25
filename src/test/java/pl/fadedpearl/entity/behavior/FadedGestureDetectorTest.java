package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FadedGestureDetectorTest {
    private static final FadedGestureDetector.State EMPTY =
            new FadedGestureDetector.State(0, 0, 0, 0, false);

    @Test
    void countsOnlyRisingAirborneEdgesAndEmitsOnceOnFourthJump() {
        FadedGestureDetector.State state = EMPTY;
        for (int jump = 1; jump <= 4; jump++) {
            FadedGestureDetector.Result airborne = tick(state, 25.0D, true, false, false, false);
            assertEquals(jump == 4 ? List.of(FadedGestureDetector.Intent.JUMP) : List.of(), airborne.intents());
            assertEquals(jump == 4 ? 0 : jump, airborne.state().jumpCount());
            FadedGestureDetector.Result stillAirborne = tick(airborne.state(), 25.0D, true, false, false, false);
            assertTrue(stillAirborne.intents().isEmpty());
            assertEquals(airborne.state().jumpCount(), stillAirborne.state().jumpCount());
            state = tick(stillAirborne.state(), 25.0D, false, false, false, false).state();
        }
    }

    @Test
    void jumpDistanceBoundaryIsInclusive() {
        FadedGestureDetector.State threeJumps = new FadedGestureDetector.State(0, 0, 3, 20, false);
        assertEquals(List.of(FadedGestureDetector.Intent.JUMP),
                tick(threeJumps, 25.0D, true, false, false, false).intents());
        assertTrue(tick(threeJumps, Math.nextUp(25.0D), true, false, false, false).intents().isEmpty());
    }

    @Test
    void jumpWindowExpiresBeforeProcessingCurrentEdge() {
        FadedGestureDetector.State expiring = new FadedGestureDetector.State(0, 0, 3, 1, false);
        FadedGestureDetector.Result result = tick(expiring, 1.0D, true, false, false, false);
        assertTrue(result.intents().isEmpty());
        assertEquals(1, result.state().jumpCount());
        assertEquals(60, result.state().jumpWindowTimer());
    }

    @Test
    void crouchEmitsExactlyAtEightHundredAndResets() {
        FadedGestureDetector.State at798 = new FadedGestureDetector.State(798, 0, 0, 0, false);
        FadedGestureDetector.Result tick799 = tick(at798, 9.0D, false, true, false, false);
        assertTrue(tick799.intents().isEmpty());
        assertEquals(799, tick799.state().crouchTimer());
        FadedGestureDetector.Result tick800 = tick(tick799.state(), 9.0D, false, true, false, false);
        assertEquals(List.of(FadedGestureDetector.Intent.CROUCH), tick800.intents());
        assertEquals(0, tick800.state().crouchTimer());
    }

    @Test
    void crouchDeadZoneMaintainsTimerAndResetThresholdIsExclusive() {
        FadedGestureDetector.State active = new FadedGestureDetector.State(27, 0, 0, 0, false);
        assertEquals(27, tick(active, 10.0D, false, true, false, false).state().crouchTimer());
        assertEquals(27, tick(active, 16.0D, false, true, false, false).state().crouchTimer());
        assertEquals(0, tick(active, Math.nextUp(16.0D), false, true, false, false).state().crouchTimer());
        assertEquals(0, tick(active, 1.0D, false, false, false, false).state().crouchTimer());
    }

    @Test
    void stareEmitsAtTwoHundredResetsAndClassifiesTrustResponse() {
        FadedGestureDetector.State at198 = new FadedGestureDetector.State(0, 198, 0, 0, false);
        FadedGestureDetector.Result tick199 = tick(at198, 25.0D, false, false, true, 11);
        assertTrue(tick199.intents().isEmpty());
        FadedGestureDetector.Result low = tick(tick199.state(), 25.0D, false, false, true, 11);
        assertEquals(List.of(FadedGestureDetector.Intent.STARE_LOW), low.intents());
        assertEquals(0, low.state().stareTimer());

        FadedGestureDetector.State at199 = new FadedGestureDetector.State(0, 199, 0, 0, false);
        assertEquals(List.of(FadedGestureDetector.Intent.STARE_HIGH),
                tick(at199, 25.0D, false, false, true, false).intents());
        assertEquals(0, tick(at199, 25.0D, false, false, false, 12).state().stareTimer());
        assertTrue(tick(at199, Math.nextUp(25.0D), false, false, true, 11).intents().isEmpty());
    }

    @Test
    void missingFriendPreservesJumpWindowBaselineAndResetsOnlyDocumentedState() {
        FadedGestureDetector.State previous = new FadedGestureDetector.State(8, 9, 3, 2, true);
        FadedGestureDetector.Result result = FadedGestureDetector.tick(
                new FadedGestureDetector.Input(false, 0.0D, false, false, false, 0), previous);
        assertEquals(new FadedGestureDetector.State(0, 0, 3, 1, false), result.state());
        assertTrue(result.intents().isEmpty());
    }

    @Test
    void preservesIntentOrderWhenMultipleThresholdsCoincide() {
        FadedGestureDetector.State previous = new FadedGestureDetector.State(799, 199, 3, 20, false);
        FadedGestureDetector.Result result = tick(previous, 1.0D, true, true, true, 11);
        assertEquals(List.of(FadedGestureDetector.Intent.JUMP, FadedGestureDetector.Intent.CROUCH,
                FadedGestureDetector.Intent.STARE_LOW), result.intents());
    }

    @Test
    void detectorApiHasNoMinecraftOrRandomDependency() {
        assertAll(
                () -> assertFalse(hasForbiddenType(FadedGestureDetector.class)),
                () -> assertFalse(hasForbiddenType(FadedGestureDetector.Input.class)),
                () -> assertFalse(hasForbiddenType(FadedGestureDetector.State.class)),
                () -> assertFalse(hasForbiddenType(FadedGestureDetector.Result.class)));
    }

    private static boolean hasForbiddenType(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream()
                .map(field -> field.getType())
                .anyMatch(fieldType -> fieldType.getName().startsWith("net.minecraft")
                        || fieldType == Random.class);
    }

    private static FadedGestureDetector.Result tick(FadedGestureDetector.State state, double distanceSq,
                                                     boolean airborne, boolean crouching,
                                                     boolean staring, boolean lowTrustStare) {
        return FadedGestureDetector.tick(new FadedGestureDetector.Input(true, distanceSq, airborne,
                crouching, staring, lowTrustStare ? 11 : 12), state);
    }

    private static FadedGestureDetector.Result tick(FadedGestureDetector.State state, double distanceSq,
                                                     boolean airborne, boolean crouching,
                                                     boolean staring, int trust) {
        return FadedGestureDetector.tick(new FadedGestureDetector.Input(true, distanceSq, airborne,
                crouching, staring, trust), state);
    }
}
