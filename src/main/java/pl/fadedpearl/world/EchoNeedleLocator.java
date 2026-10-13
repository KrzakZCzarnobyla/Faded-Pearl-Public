package pl.fadedpearl.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Pure selection and quantization policy for the cave-encounter HUD locator. */
public final class EchoNeedleLocator {
    public static final double MAX_RANGE = 96.0D;
    public static final int SECTOR_COUNT = 16;
    public static final double SIMILAR_HEIGHT_RANGE = 4.0D;

    public enum Height {
        BELOW,
        SIMILAR,
        ABOVE;

        public static Height fromCode(int code) {
            return code >= 0 && code < values().length ? values()[code] : SIMILAR;
        }
    }

    public record Candidate(UUID companion, String dimension, BlockPos pos,
                            boolean alive, boolean healed, boolean unclaimed) {}

    public record Reading(int sector, Height height) {
        public Reading {
            sector = Math.floorMod(sector, SECTOR_COUNT);
            height = height == null ? Height.SIMILAR : height;
        }
    }

    public static boolean canGuide(boolean spectator, boolean hasCompanion, boolean canSeeSky) {
        return !spectator && !hasCompanion && !canSeeSky;
    }

    public static Optional<Reading> locate(Collection<Candidate> candidates, String dimension,
                                           double playerX, double playerY, double playerZ,
                                           float playerYaw) {
        double maxDistanceSq = MAX_RANGE * MAX_RANGE;
        return candidates.stream()
                .filter(candidate -> candidate.alive && !candidate.healed && candidate.unclaimed)
                .filter(candidate -> candidate.dimension.equals(dimension))
                .filter(candidate -> distanceSquared(candidate.pos, playerX, playerY, playerZ) <= maxDistanceSq)
                .min(Comparator.comparingDouble((Candidate candidate) ->
                                distanceSquared(candidate.pos, playerX, playerY, playerZ))
                        .thenComparing(Candidate::companion))
                .map(candidate -> new Reading(
                        sector(playerX, playerZ, playerYaw, candidate.pos),
                        height(playerY, candidate.pos.getY() + 0.5D)));
    }

    static int sector(double playerX, double playerZ, float playerYaw, BlockPos target) {
        double dx = target.getX() + 0.5D - playerX;
        double dz = target.getZ() + 0.5D - playerZ;
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relativeYaw = Mth.wrapDegrees(targetYaw - playerYaw);
        return sectorFromRelativeYaw(relativeYaw);
    }

    static int sectorFromRelativeYaw(double relativeYaw) {
        return Math.floorMod((int) Math.floor((Mth.wrapDegrees(relativeYaw) + 11.25D) / 22.5D),
                SECTOR_COUNT);
    }

    static Height height(double playerY, double targetY) {
        double delta = targetY - playerY;
        if (delta > SIMILAR_HEIGHT_RANGE) return Height.ABOVE;
        if (delta < -SIMILAR_HEIGHT_RANGE) return Height.BELOW;
        return Height.SIMILAR;
    }

    private static double distanceSquared(BlockPos target, double playerX, double playerY, double playerZ) {
        double dx = target.getX() + 0.5D - playerX;
        double dy = target.getY() + 0.5D - playerY;
        double dz = target.getZ() + 0.5D - playerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private EchoNeedleLocator() {}
}
