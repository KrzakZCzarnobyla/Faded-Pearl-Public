package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementStageDAdapterTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void stageDUsesApprovedThresholdCooldownAndBoundedScanFrequency() throws IOException {
        String source = Files.readString(ENTITY);
        assertTrue(source.contains("WATER_BARRIER_SCAN_INTERVAL_TICKS = 10"));
        assertTrue(source.contains("WATER_BARRIER_MAX_CORRIDOR_SAMPLES = 32"));
        assertTrue(source.contains("++waterBarrierScanTicks >= WATER_BARRIER_SCAN_INTERVAL_TICKS"));
        assertTrue(source.contains("movementState.recoveryCooldownTicks() == 0"));
        assertTrue(source.contains("FadedMovementCoordinator.WATER_BARRIER_STUCK_TICKS - 1"));
        assertTrue(source.contains("FadedMovementCoordinator.REQUIRED_WATER_EVIDENCE_TICKS"));
        assertEquals(1, occurrences(source, "scanWaterBarrier(path, friend)"));
    }

    @Test
    void evidenceRequiresWaterAtPartialPathOrBoundedCorridor() throws IOException {
        String source = Files.readString(ENTITY);
        String scan = between(source, "private boolean scanWaterBarrier", "private boolean hasWaterNear");
        assertTrue(scan.contains("path != null && path.getEndNode() != null"));
        assertTrue(scan.contains("path.getNodePos(index)"));
        assertTrue(scan.contains("hasWaterNear(corridorStart)"));
        assertTrue(scan.contains("Math.min(WATER_BARRIER_MAX_CORRIDOR_SAMPLES"));
        assertTrue(scan.contains("isWaterColumn(pos)"));
        String water = between(source, "private boolean isWaterColumn", "private BlockPos findVisibleFollowRecoveryLanding");
        assertTrue(water.contains("FluidTags.WATER"));
    }

    @Test
    void ordinaryWallFenceAndTransientPathFailureCannotRecoverWithoutWaterAndVisibility() throws IOException {
        String source = Files.readString(ENTITY);
        String observation = between(source, "private FollowMovementObservation observeFollowMovement",
                "private void resetFollowBarrierObservation");
        assertTrue(observation.contains("!measurableProgress && !pathReachable"));
        assertTrue(observation.contains("cachedWaterBarrierEvidence"));
        assertTrue(observation.contains("findVisibleFollowRecoveryLanding(friend)"));
        String landing = between(source, "private BlockPos findVisibleFollowRecoveryLanding",
                "private boolean isVisibleRecoveryLanding");
        assertTrue(landing.contains("isSafeHomeLanding(serverLevel, feet)"));
        assertTrue(landing.contains("isVisibleRecoveryLanding(feet)"));
        assertTrue(source.contains("hit.getType() == HitResult.Type.MISS"));
    }

    @Test
    void bothConfirmedDryVariantsUseSameRecoveryWithoutRequiringWetEnderman() throws IOException {
        String source = Files.readString(ENTITY);
        String observation = between(source, "private FollowMovementObservation observeFollowMovement",
                "private void resetFollowBarrierObservation");
        assertTrue(observation.contains("distanceSqr <= 900.0D"));
        assertTrue(observation.contains("Locomotion.FOLLOW_WATER_BARRIER_RECOVERY"));
        String eligibility = between(observation, "boolean recoveryReady", "if (recoveryReady)");
        assertEquals(0, occurrences(eligibility, "isInWaterOrBubble()"));
        assertTrue(source.contains("path.getEndNode().asBlockPos()"));
        assertTrue(source.contains("BlockPos corridorStart = blockPosition()"));
    }

    @Test
    void recoveryHasSingleAdapterApplyAndRejectedLandingHasNoEffects() throws IOException {
        String source = Files.readString(ENTITY);
        String observation = between(source, "private FollowMovementObservation observeFollowMovement",
                "private void resetFollowBarrierObservation");
        assertTrue(observation.contains("applyNavigationStop();"));
        assertTrue(observation.contains("trySafeTeleport"));
        assertTrue(observation.contains("RecoveryOutcome.SUCCESS"));
        assertTrue(observation.contains("RecoveryOutcome.ROLLBACK"));
        assertTrue(observation.contains("RecoveryOutcome.NO_TARGET"));
        assertEquals(0, occurrences(observation, "sendParticles"));
        assertEquals(0, occurrences(observation, "playSound"));
    }

    @Test
    void progressFriendMovementReachablePathAndWaterPauseResetOrBlockEpisode() throws IOException {
        String source = Files.readString(ENTITY);
        String observation = between(source, "private FollowMovementObservation observeFollowMovement",
                "private void resetFollowBarrierObservation");
        assertTrue(observation.contains("currentPosition.distanceToSqr(lastFollowObservationPosition)"));
        assertTrue(observation.contains("friendPosition.distanceToSqr(lastFollowFriendPosition)"));
        assertTrue(observation.contains("lastFollowDistanceSqr - distanceSqr"));
        assertTrue(observation.contains("path != null && path.canReach() && !pathUsesWater"));
        assertTrue(observation.contains("friend.isInWaterOrBubble()"));
        assertTrue(observation.contains("resetFollowBarrierObservation();"));
    }

    @Test
    void transientStageDStateIsNotWrittenToNbt() throws IOException {
        String source = Files.readString(ENTITY);
        String save = between(source, "addAdditionalSaveData", "readAdditionalSaveData");
        for (String transientName : new String[]{"noProgressTicks", "waterEvidenceTicks",
                "recoveryCooldownTicks", "lastFollowObservationPosition", "lastObservedFriendId",
                "pendingBarrierRecoveryOutcome"}) {
            assertEquals(0, occurrences(save, transientName), transientName);
        }
    }

    @Test
    void canReachPathThroughWaterAndSelfRescueLoopUseDryRouteVerification() throws IOException {
        String source = Files.readString(ENTITY);
        String observation = between(source, "private FollowMovementObservation observeFollowMovement",
                "private void resetFollowBarrierObservation");
        assertTrue(observation.contains("boolean pathUsesWater = pathUsesWater(path)"));
        assertTrue(observation.contains("movementState.selfRescuedInFollowEpisode()"));
        assertTrue(observation.contains("createDryFollowPath(friend)"));
        assertTrue(observation.contains("!selfRescueLatched"));
        String dryPath = between(source, "private Path createDryFollowPath", "private Path consumeDryFollowPath");
        assertTrue(dryPath.contains("setPathfindingMalus(BlockPathTypes.WATER, -1.0F)"));
        assertTrue(dryPath.contains("finally"));
        assertTrue(dryPath.contains("setPathfindingMalus(BlockPathTypes.WATER, previousWaterMalus)"));
        assertTrue(dryPath.contains("dryPath.canReach() && !pathUsesWater(dryPath)"));
        assertTrue(source.contains("applyMoveTo(dryPath, speed)"));
    }

    @Test
    void friendUuidOrMovementStartsFreshEpisodeAndInvalidatesEveryCache() throws IOException {
        String source = Files.readString(ENTITY);
        assertTrue(source.contains("lastObservedFriendId != null && !lastObservedFriendId.equals(friend.getUUID())"));
        assertTrue(source.contains("if (activeFollow && !commandChanged) lastObservedFriendId = friend.getUUID()"));
        assertTrue(source.contains("if (targetChanged || friendMoved) clearFollowBarrierEpisodeCache()"));
        String clear = between(source, "private void clearFollowBarrierEpisodeCache", "private boolean pathUsesWater");
        assertTrue(clear.contains("waterBarrierScanTicks = 0"));
        assertTrue(clear.contains("cachedWaterBarrierEvidence = false"));
        assertTrue(clear.contains("cachedDryFollowPath = null"));
        assertTrue(clear.contains("cachedDryFollowTarget = null"));
    }

    @Test
    void followSelfRescueCannotCrossOccludingWallBeforeBarrierRecoveryRaycast() throws IOException {
        String source = Files.readString(ENTITY);
        String visibleSelfRescue = between(source, "private BlockPos findNearestVisibleFollowSelfRescueLanding",
                "public static AttributeSupplier.Builder createAttributes");
        assertTrue(visibleSelfRescue.contains("isSafeHomeLanding(serverLevel, feet)"));
        assertTrue(visibleSelfRescue.contains("isVisibleRecoveryLanding(feet)"));

        String escape = between(source, "private void escapeWaterToDryLand", "private boolean teleportNearGroundedFriend");
        assertTrue(escape.contains("getCommand() == CompanionCommand.FOLLOW && getFriendPlayer() != null"));
        assertTrue(escape.contains("findNearestVisibleFollowSelfRescueLanding(blockPosition(), 16, 12)"));
        assertTrue(escape.contains("findNearestSafePassengerLanding"));
        assertEquals(0, occurrences(escape, "teleportNearGroundedFriend"));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        return source.substring(from, to);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
