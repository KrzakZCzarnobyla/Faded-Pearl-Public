package pl.fadedpearl.entity.behavior;

import java.util.ArrayList;
import java.util.List;
import pl.fadedpearl.entity.trust.FadedTrustManager;

public final class FadedGestureDetector {
    private FadedGestureDetector() {}

    public enum Intent {
        JUMP,
        CROUCH,
        STARE_LOW,
        STARE_HIGH
    }

    public record Input(boolean friendPresent, double distanceSq, boolean airborne,
                        boolean crouching, boolean lookHitsTarget, int trust) {}

    public record State(int crouchTimer, int stareTimer, int jumpCount,
                        int jumpWindowTimer, boolean friendWasAirborne) {}

    public record Result(State state, List<Intent> intents) {
        public Result {
            intents = List.copyOf(intents);
        }
    }

    public static Result tick(Input input, State previous) {
        int crouchTimer = previous.crouchTimer();
        int stareTimer = previous.stareTimer();
        int jumpCount = previous.jumpCount();
        int jumpWindowTimer = previous.jumpWindowTimer();
        boolean friendWasAirborne = previous.friendWasAirborne();
        List<Intent> intents = new ArrayList<>(2);

        if (jumpWindowTimer > 0 && --jumpWindowTimer == 0)
            jumpCount = 0;

        if (!input.friendPresent()) {
            return new Result(new State(0, 0, jumpCount, jumpWindowTimer, false), intents);
        }

        if (input.distanceSq() <= 25.0D && input.airborne() && !friendWasAirborne) {
            jumpCount++;
            jumpWindowTimer = 60;
            if (jumpCount >= 4) {
                intents.add(Intent.JUMP);
                jumpCount = 0;
            }
        }
        friendWasAirborne = input.airborne();

        if (input.crouching() && input.distanceSq() <= 9.0D) {
            if (++crouchTimer == 800) {
                intents.add(Intent.CROUCH);
                crouchTimer = 0;
            }
        } else if (!input.crouching() || input.distanceSq() > 16.0D) {
            crouchTimer = 0;
        }

        boolean staring = input.distanceSq() <= 25.0D && input.lookHitsTarget();
        if (staring && ++stareTimer >= 200) {
            intents.add(FadedTrustManager.isWorldCautious(input.trust()) ? Intent.STARE_LOW : Intent.STARE_HIGH);
            stareTimer = 0;
        } else if (!staring) {
            stareTimer = 0;
        }

        return new Result(new State(crouchTimer, stareTimer, jumpCount, jumpWindowTimer,
                friendWasAirborne), intents);
    }
}
