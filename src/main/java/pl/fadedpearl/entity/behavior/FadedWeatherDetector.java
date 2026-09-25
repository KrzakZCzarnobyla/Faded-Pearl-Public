package pl.fadedpearl.entity.behavior;

import java.util.ArrayList;
import java.util.List;

public final class FadedWeatherDetector {
    private FadedWeatherDetector() {}

    public enum Intent {
        RAIN_SHIVER,
        RAIN_PENALTY,
        RAIN_SHELTER,
        SHELTER_REWARD,
        SNOW_CATCH
    }

    public record Input(boolean raining, boolean precipitationRain, boolean precipitationSnow,
                        boolean rainingAtSelf, boolean snowingAtSelf, boolean canSeeSkyAtSelf,
                        boolean nearSurfaceShelter, double friendDistanceSq,
                        boolean rainingAtFriend, boolean snowSampleTick) {}

    public record State(boolean rainSheltered, int rainExposureTicks,
                        int shelteredTogetherTicks) {}

    public record Result(State state, List<Intent> beforeSleepAndNight,
                         List<Intent> afterSleepAndNight) {
        public Result {
            beforeSleepAndNight = List.copyOf(beforeSleepAndNight);
            afterSleepAndNight = List.copyOf(afterSleepAndNight);
        }
    }

    public static Result tick(Input input, State previous) {
        boolean exposedToRain = input.raining() && input.precipitationRain() && input.rainingAtSelf();
        boolean rainSheltered = input.raining() && input.precipitationRain()
                && !input.canSeeSkyAtSelf() && input.nearSurfaceShelter();
        int rainExposureTicks = previous.rainExposureTicks();
        int shelteredTogetherTicks = previous.shelteredTogetherTicks();
        List<Intent> beforeSleepAndNight = new ArrayList<>(2);
        List<Intent> afterSleepAndNight = new ArrayList<>(1);

        if (exposedToRain) {
            shelteredTogetherTicks = 0;
            beforeSleepAndNight.add(Intent.RAIN_SHIVER);
            if (++rainExposureTicks == 1200)
                beforeSleepAndNight.add(Intent.RAIN_PENALTY);
        } else {
            rainExposureTicks = 0;
            if (rainSheltered && input.friendDistanceSq() <= 36.0D && !input.rainingAtFriend()) {
                beforeSleepAndNight.add(Intent.RAIN_SHELTER);
                if (++shelteredTogetherTicks >= 600) {
                    beforeSleepAndNight.add(Intent.SHELTER_REWARD);
                    shelteredTogetherTicks = 0;
                }
            } else {
                shelteredTogetherTicks = 0;
            }
        }

        boolean snowing = input.raining() && input.precipitationSnow() && input.snowingAtSelf();
        if (input.snowSampleTick() && snowing)
            afterSleepAndNight.add(Intent.SNOW_CATCH);

        return new Result(new State(rainSheltered, rainExposureTicks, shelteredTogetherTicks),
                beforeSleepAndNight, afterSleepAndNight);
    }

    public static boolean isAtOrAboveMotionBlockingHeight(int motionBlockingHeight, int blockY) {
        return motionBlockingHeight <= blockY;
    }

    public static boolean isNearSurfaceShelter(int motionBlockingHeight, int blockY) {
        return motionBlockingHeight - blockY <= 4;
    }
}
