package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementStageCAdapterTest {
    private static final Path ENTITY = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void activeGoalsProduceTypedCandidatesAndPreserveNonCombatConstants() throws IOException {
        String source = Files.readString(ENTITY);
        for (String intent : new String[]{
                "Locomotion.ESCAPE_FIRE", "Locomotion.COMBAT_CHASE",
                "Locomotion.FOLLOW_PATH", "Locomotion.FOLLOW_DISTANCE_TELEPORT",
                "Locomotion.HOME_DIMENSION_TRANSFER", "Locomotion.HOME_RETURN",
                "Locomotion.HOME_WANDER"}) {
            assertTrue(source.contains(intent), intent);
        }
        for (String baseline : new String[]{
                "distance > 900.0D",
                "1.25D : 0.85D", "5.5D", "1.35D", "1225.0D", "2500.0D",
                "8, 12", "random.nextInt(80)", "10, 5", ".72D"}) {
            assertTrue(source.contains(baseline), baseline);
        }
        assertTrue(source.contains("private static final double SPEED = 1.3D"));
    }

    @Test
    void meleeKeepsVanillaAttackCadenceWhileChaseAndLookAreArbitrated() throws IOException {
        String source = Files.readString(ENTITY);
        String melee = between(source, "private final class ArbitratedMeleeAttackGoal",
                "private final class FollowFriendGoal");
        assertTrue(melee.contains("ticksUntilNextAttack = adjustedTickDelay(20)"));
        assertTrue(melee.contains("swing(InteractionHand.MAIN_HAND)"));
        assertTrue(melee.contains("doHurtTarget(target)"));
        assertEquals(3, occurrences(melee, "isCombatAllowed(isAcceptedCombatTarget(target))"));
        assertTrue(source.contains("return shouldProtectFriendFrom(target)"));
        assertTrue(source.contains("|| target == selfDefenseTarget && selfDefenseTicks > 0 && target.isAlive()"));
        assertEquals(0, occurrences(melee, "protectionTargetAccepted"));
        assertTrue(melee.contains("queueLocomotion(FadedMovementCoordinator.Locomotion.COMBAT_CHASE"));
        assertTrue(melee.contains("queueLookAt(target, 30.0F, 30.0F"));
        assertEquals(0, occurrences(melee, "getNavigation().moveTo"));
        assertEquals(0, occurrences(melee, "getLookControl().setLookAt"));
    }

    @Test
    void followUnsafeDistanceUsesDedicatedAlwaysAllowedStop() throws IOException {
        String source = Files.readString(ENTITY);
        String follow = between(source, "private final class FollowFriendGoal",
                "private final class EscapeFireGoal");
        assertTrue(follow.contains("distance > 900.0D"));
        assertTrue(follow.contains("Locomotion.FOLLOW_DISTANCE_STOP"));
        assertEquals(0, occurrences(follow, "FOLLOW_WATER_PAUSE,\n                        \"follow teleport landing unavailable\""));
    }

    @Test
    void longHomeReturnIsRecomputedEveryActiveTick() throws IOException {
        String source = Files.readString(ENTITY);
        String home = between(source, "private final class ReturnHomeGoal",
                "private BlockPos findSafeHomeLanding");
        assertTrue(home.contains("start()"));
        assertTrue(home.contains("queueHomeReturn(homePos)"));
        assertTrue(home.contains("distanceToSqr(Vec3.atCenterOf(homePos)) > 2500.0D) queueHomeReturn(homePos)"));
        assertTrue(home.contains("if (landing != null) queueLocomotion"));
        assertTrue(home.contains("else queueHomePath(destination)"));
        assertTrue(home.contains("\"home path\", () -> applyMoveTo(destination, 1.0D, .9D)"));
        assertEquals(0, occurrences(home.substring(home.indexOf("private void queueHomeReturn")), "() -> {}"));
    }

    @Test
    void sblIsOnlySensorMemoryAndIdleCandidateSource() throws IOException {
        String source = Files.readString(ENTITY);
        String core = between(source, "getCoreTasks()", "getIdleTasks()");
        assertTrue(core.contains("BrainActivityGroup.coreTasks()"));
        assertEquals(0, occurrences(core, "LookAtTarget"));
        assertEquals(0, occurrences(core, "MoveToWalkTarget"));
        assertTrue(source.contains("getMemory(MemoryModuleType.LOOK_TARGET)"));
        assertTrue(source.contains("getMemory(MemoryModuleType.WALK_TARGET)"));
        assertTrue(source.contains("Locomotion.IDLE_EXPLORE"));
        assertTrue(source.contains("Look.LOOK_WALK_TARGET"));
    }

    @Test
    void floatIsTheOnlyAllowedVanillaSafetyInternalAndFriendWanderStaysInactive() throws IOException {
        String source = Files.readString(ENTITY);
        String registrations = between(source, "protected void registerGoals()", "protected Brain.Provider");
        assertTrue(registrations.contains("new FloatGoal(this)"));
        assertEquals(0, occurrences(registrations, "new MeleeAttackGoal"));
        assertEquals(0, occurrences(registrations, "new WanderNearFriendGoal"));
        assertEquals(1, occurrences(source, "private final class WanderNearFriendGoal"));
    }

    @Test
    void interactionLatchStillGatesEveryOrdinaryGoalAndBrainIdle() throws IOException {
        String source = Files.readString(ENTITY);
        for (String className : new String[]{"FollowFriendGoal", "ReturnHomeDimensionGoal",
                "ReturnHomeGoal", "WanderNearAnchorGoal"}) {
            int start = source.indexOf("class " + className);
            int end = source.indexOf("private final class", start + 20);
            if (end < 0) end = source.length();
            assertTrue(source.substring(start, end).contains("interactionMovementStopLatched"), className);
        }
        assertTrue(source.contains("!interactionMovementStopLatched\n                && getCommand() == CompanionCommand.FOLLOW"));
    }

    @Test
    void restEmitsImmobilizationAndProtectorDoesNotEmitGuardOrTarget() throws IOException {
        String source = Files.readString(ENTITY);
        assertTrue(source.contains("getCommand() == CompanionCommand.REST) {\n"
                + "            queueStop(FadedMovementCoordinator.Locomotion.IMMOBILE_REST"));
        assertTrue(source.contains("queueClearTarget(\"rest clears protection target\")"));
        String protector = between(source, "private void tickProtector()", "private void tickNearbyMobReactions()");
        assertTrue(protector.contains("decision.guardAttacker() && getCommand() != CompanionCommand.REST"));
    }

    @Test
    void stageDRecoveryIsEnabledOnlyThroughTheApprovedCandidate() throws IOException {
        String source = Files.readString(ENTITY);
        assertEquals(1, occurrences(source, "Locomotion.FOLLOW_WATER_BARRIER_RECOVERY"));
        assertEquals(1, occurrences(source, "WATER_BARRIER_STUCK_TICKS"));
        assertEquals(0, occurrences(source, "RETRY_COOLDOWN_TICKS"));
    }

    private static String between(String source, String start, String end) {
        return source.substring(source.indexOf(start), source.indexOf(end, source.indexOf(start)));
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
