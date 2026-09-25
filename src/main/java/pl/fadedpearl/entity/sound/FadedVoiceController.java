package pl.fadedpearl.entity.sound;

import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.Objects;
import java.util.function.IntUnaryOperator;

/** Pure timing, eligibility and presentation pairing for situational voice lines. */
public final class FadedVoiceController {
    public static final int UNINITIALIZED_COOLDOWN = -1;
    public static final int AMBIENT_MIN_COOLDOWN_TICKS = 2400;
    public static final int AMBIENT_MAX_COOLDOWN_TICKS = 4800;
    public static final int WOUNDED_NOTICE_TICKS = 60;
    public static final int CRY_PRESENTATION_TICKS = 87;

    public enum AmbientVariant {
        ENDERMAN_ONE(65),
        ENDERMAN_TWO(62),
        VOICE_164238(77);

        private final int presentationTicks;

        AmbientVariant(int presentationTicks) {
            this.presentationTicks = presentationTicks;
        }

        public int presentationTicks() {
            return presentationTicks;
        }
    }

    public enum AmbientPose {
        CURIOUS,
        LOOK_AROUND,
        AFFECTION
    }

    public record AmbientSnapshot(
            boolean healed,
            boolean friendNearby,
            boolean downed,
            boolean healing,
            boolean restCommand,
            boolean socialActionActive,
            boolean curiosityActive,
            boolean carrying,
            boolean combatActive,
            boolean protectionOrRescueActive,
            boolean unsafeEnvironment,
            boolean moving
    ) {}

    public record AmbientDecision(int nextCooldown, AmbientVariant variant, AmbientPose pose) {
        public AmbientDecision {
            if ((variant == null) != (pose == null)) {
                throw new IllegalArgumentException("Ambient sound and pose must be selected together");
            }
        }

        public boolean play() {
            return variant != null;
        }
    }

    public static boolean shouldPlayWoundedNotice(boolean wounded, boolean wasPlayerNearby,
                                                   boolean playerNearby) {
        return wounded && !wasPlayerNearby && playerNearby;
    }

    public static boolean isAmbientEligible(AmbientSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshot.healed() && snapshot.friendNearby() && !snapshot.downed() && !snapshot.healing()
                && !snapshot.restCommand() && !snapshot.socialActionActive() && !snapshot.curiosityActive()
                && !snapshot.carrying() && !snapshot.combatActive() && !snapshot.protectionOrRescueActive()
                && !snapshot.unsafeEnvironment() && !snapshot.moving();
    }

    public static AmbientDecision tickAmbient(int cooldown, AmbientSnapshot snapshot, int trust,
                                               IntUnaryOperator boundedRandom) {
        Objects.requireNonNull(boundedRandom, "boundedRandom");
        if (cooldown < 0) {
            return waiting(nextCooldown(boundedRandom));
        }
        if (cooldown > 0) {
            return waiting(cooldown - 1);
        }
        if (!isAmbientEligible(snapshot)) {
            return waiting(0);
        }

        AmbientVariant variant = AmbientVariant.values()[nextRandom(boundedRandom, AmbientVariant.values().length)];
        return new AmbientDecision(nextCooldown(boundedRandom), variant, poseFor(variant, trust));
    }

    public static AmbientPose poseFor(AmbientVariant variant, int trust) {
        return switch (Objects.requireNonNull(variant, "variant")) {
            case ENDERMAN_ONE -> AmbientPose.CURIOUS;
            case ENDERMAN_TWO -> AmbientPose.LOOK_AROUND;
            case VOICE_164238 -> FadedTrustManager.allowsAffection(trust)
                    ? AmbientPose.AFFECTION : AmbientPose.CURIOUS;
        };
    }

    private static AmbientDecision waiting(int cooldown) {
        return new AmbientDecision(cooldown, null, null);
    }

    private static int nextCooldown(IntUnaryOperator boundedRandom) {
        int spread = AMBIENT_MAX_COOLDOWN_TICKS - AMBIENT_MIN_COOLDOWN_TICKS + 1;
        return AMBIENT_MIN_COOLDOWN_TICKS + nextRandom(boundedRandom, spread);
    }

    private static int nextRandom(IntUnaryOperator boundedRandom, int bound) {
        int value = boundedRandom.applyAsInt(bound);
        if (value < 0 || value >= bound) {
            throw new IllegalArgumentException("Random value " + value + " is outside 0.." + (bound - 1));
        }
        return value;
    }

    private FadedVoiceController() {}
}
