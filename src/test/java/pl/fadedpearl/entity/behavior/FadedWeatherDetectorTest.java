package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FadedWeatherDetectorTest {
    private static final FadedWeatherDetector.State EMPTY = new FadedWeatherDetector.State(false, 0, 0);

    @Test
    void rainPenaltyEmitsExactlyAtTwelveHundredOnlyOncePerContinuousExposure() {
        FadedWeatherDetector.Result threshold = tick(rainInput(), new FadedWeatherDetector.State(false, 1199, 27));
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHIVER,
                FadedWeatherDetector.Intent.RAIN_PENALTY), threshold.beforeSleepAndNight());
        assertEquals(new FadedWeatherDetector.State(false, 1200, 0), threshold.state());

        FadedWeatherDetector.Result after = tick(rainInput(), threshold.state());
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHIVER), after.beforeSleepAndNight());
        assertEquals(1201, after.state().rainExposureTicks());
    }

    @Test
    void dryTickResetsExposureAndAllowsACompleteNewPenaltySequence() {
        FadedWeatherDetector.Result dry = tick(input(false, true, false, false, true, 1.0D, false, false),
                new FadedWeatherDetector.State(false, 1201, 0));
        assertEquals(0, dry.state().rainExposureTicks());

        FadedWeatherDetector.Result newThreshold = tick(rainInput(),
                new FadedWeatherDetector.State(false, 1199, 0));
        assertTrue(newThreshold.beforeSleepAndNight().contains(FadedWeatherDetector.Intent.RAIN_PENALTY));
    }

    @Test
    void directExposureAlwaysResetsSharedShelterCounter() {
        assertEquals(0, tick(rainInput(), new FadedWeatherDetector.State(false, 0, 599))
                .state().shelteredTogetherTicks());
    }

    @Test
    void sharedShelterRewardsAtSixHundredAndResetsCounter() {
        FadedWeatherDetector.Result result = tick(shelterInput(36.0D, false),
                new FadedWeatherDetector.State(true, 0, 599));
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHELTER,
                FadedWeatherDetector.Intent.SHELTER_REWARD), result.beforeSleepAndNight());
        assertEquals(0, result.state().shelteredTogetherTicks());
    }

    @Test
    void shelterDistanceBoundaryIsInclusiveAndGreaterValueResets() {
        assertEquals(1, tick(shelterInput(36.0D, false), EMPTY).state().shelteredTogetherTicks());
        assertEquals(0, tick(shelterInput(Math.nextUp(36.0D), false),
                new FadedWeatherDetector.State(true, 0, 25)).state().shelteredTogetherTicks());
    }

    @Test
    void wetFriendNoRainPrecipitationOrVisibleSkyExcludeSharedShelter() {
        FadedWeatherDetector.State active = new FadedWeatherDetector.State(true, 0, 25);
        assertEquals(0, tick(shelterInput(1.0D, true), active).state().shelteredTogetherTicks());
        assertEquals(0, tick(input(true, false, false, false, false, 1.0D, false, false), active)
                .state().shelteredTogetherTicks());
        assertEquals(0, tick(input(true, true, false, false, true, 1.0D, false, false), active)
                .state().shelteredTogetherTicks());
    }

    @Test
    void snowEmitsOnlyOnSamplingTickAndInPostSleepPhase() {
        FadedWeatherDetector.Input betweenSamples = input(true, false, true, true, true, 1.0D, false, false);
        assertTrue(tick(betweenSamples, EMPTY).afterSleepAndNight().isEmpty());

        FadedWeatherDetector.Result sampled = tick(
                input(true, false, true, true, true, 1.0D, false, true), EMPTY);
        assertTrue(sampled.beforeSleepAndNight().isEmpty());
        assertEquals(List.of(FadedWeatherDetector.Intent.SNOW_CATCH), sampled.afterSleepAndNight());
    }

    @Test
    void snowExposureAcceptsMotionBlockingHeightAtOrBelowEntityOnly() {
        assertTrue(FadedWeatherDetector.isAtOrAboveMotionBlockingHeight(80, 80));
        assertTrue(FadedWeatherDetector.isAtOrAboveMotionBlockingHeight(79, 80));
        assertFalse(FadedWeatherDetector.isAtOrAboveMotionBlockingHeight(81, 80));
    }

    @Test
    void warmRainCannotBecomeSnowExposure() {
        FadedWeatherDetector.Result result = tick(
                input(true, true, false, true, true, true, 1.0D, false, true), EMPTY);
        assertTrue(result.afterSleepAndNight().isEmpty());
    }

    @Test
    void roofedSnowCannotEmitSnowCatch() {
        FadedWeatherDetector.Result result = tick(
                input(true, false, true, false, false, false, 1.0D, false, true), EMPTY);
        assertTrue(result.afterSleepAndNight().isEmpty());
    }

    @Test
    void rainAndSnowAreExclusiveThroughPrecipitationInput() {
        FadedWeatherDetector.Result rain = tick(rainInput(), EMPTY);
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHIVER), rain.beforeSleepAndNight());
        assertTrue(rain.afterSleepAndNight().isEmpty());

        FadedWeatherDetector.Result snow = tick(
                input(true, false, true, true, true, 1.0D, false, true), EMPTY);
        assertTrue(snow.beforeSleepAndNight().isEmpty());
        assertEquals(List.of(FadedWeatherDetector.Intent.SNOW_CATCH), snow.afterSleepAndNight());
    }

    @Test
    void rainShelteredMatchesRainPrecipitationAndBlockedSkyExactly() {
        assertTrue(tick(shelterInput(1.0D, false), EMPTY).state().rainSheltered());
        assertFalse(tick(input(false, true, false, false, false, 1.0D, false, false), EMPTY)
                .state().rainSheltered());
        assertFalse(tick(input(true, false, false, false, false, 1.0D, false, false), EMPTY)
                .state().rainSheltered());
        assertFalse(tick(input(true, true, false, false, true, 1.0D, false, false), EMPTY)
                .state().rainSheltered());
    }

    @Test
    void deepCaveIsNotAWeatherShelter() {
        FadedWeatherDetector.Input cave = new FadedWeatherDetector.Input(
                true, true, false, false, false, false, false, 1.0D, false, false);
        FadedWeatherDetector.Result result = tick(cave, new FadedWeatherDetector.State(true, 0, 599));
        assertFalse(result.state().rainSheltered());
        assertTrue(result.beforeSleepAndNight().isEmpty());
        assertEquals(0, result.state().shelteredTogetherTicks());
    }

    @Test
    void shelterMustBeWithinFourBlocksOfSurfaceHeight() {
        assertTrue(FadedWeatherDetector.isNearSurfaceShelter(84, 80));
        assertFalse(FadedWeatherDetector.isNearSurfaceShelter(85, 80));
    }

    @Test
    void intentOrderPreservesRainActionBeforePenaltyAndShelterBeforeReward() {
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHIVER,
                        FadedWeatherDetector.Intent.RAIN_PENALTY),
                tick(rainInput(), new FadedWeatherDetector.State(false, 1199, 0)).beforeSleepAndNight());
        assertEquals(List.of(FadedWeatherDetector.Intent.RAIN_SHELTER,
                        FadedWeatherDetector.Intent.SHELTER_REWARD),
                tick(shelterInput(1.0D, false),
                        new FadedWeatherDetector.State(true, 0, 599)).beforeSleepAndNight());
    }

    @Test
    void detectorApiHasNoMinecraftOrRandomDependency() {
        assertAll(
                () -> assertFalse(hasForbiddenType(FadedWeatherDetector.class)),
                () -> assertFalse(hasForbiddenType(FadedWeatherDetector.Input.class)),
                () -> assertFalse(hasForbiddenType(FadedWeatherDetector.State.class)),
                () -> assertFalse(hasForbiddenType(FadedWeatherDetector.Result.class)));
    }

    private static boolean hasForbiddenType(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream()
                .map(field -> field.getType())
                .anyMatch(fieldType -> fieldType.getName().startsWith("net.minecraft")
                        || fieldType == Random.class);
    }

    private static FadedWeatherDetector.Result tick(FadedWeatherDetector.Input input,
                                                     FadedWeatherDetector.State state) {
        return FadedWeatherDetector.tick(input, state);
    }

    private static FadedWeatherDetector.Input rainInput() {
        return input(true, true, false, true, false, true, 1.0D, false, false);
    }

    private static FadedWeatherDetector.Input shelterInput(double distanceSq, boolean friendWet) {
        return input(true, true, false, false, false, false, distanceSq, friendWet, false);
    }

    private static FadedWeatherDetector.Input input(boolean raining, boolean rain, boolean snow,
                                                     boolean rainingAtSelf, boolean canSeeSky,
                                                     double distanceSq, boolean rainingAtFriend,
                                                     boolean snowSampleTick) {
        return input(raining, rain, snow, rainingAtSelf, rainingAtSelf, canSeeSky,
                distanceSq, rainingAtFriend, snowSampleTick);
    }

    private static FadedWeatherDetector.Input input(boolean raining, boolean rain, boolean snow,
                                                     boolean rainingAtSelf, boolean snowingAtSelf,
                                                     boolean canSeeSky, double distanceSq,
                                                     boolean rainingAtFriend, boolean snowSampleTick) {
        return new FadedWeatherDetector.Input(raining, rain, snow, rainingAtSelf, snowingAtSelf, canSeeSky,
                !canSeeSky, distanceSq, rainingAtFriend, snowSampleTick);
    }
}
