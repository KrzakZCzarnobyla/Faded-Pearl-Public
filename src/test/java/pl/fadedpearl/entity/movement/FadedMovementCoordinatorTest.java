package pl.fadedpearl.entity.movement;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static pl.fadedpearl.entity.movement.FadedMovementCoordinator.*;

class FadedMovementCoordinatorTest {
    private final FadedMovementCoordinator coordinator = new FadedMovementCoordinator();

    @Test
    void explicitPriorityTableOrdersEveryConflictTier() {
        Locomotion[] ordered = {
                Locomotion.ESCAPE_WATER, Locomotion.ESCAPE_FIRE,
                Locomotion.RESCUE_FALL, Locomotion.IMMOBILE_WOUNDED,
                Locomotion.PASSENGER_CONTROLLED, Locomotion.GUARD_INTERPOSE,
                Locomotion.EXTERNAL_INTERACTION_STOP, Locomotion.HOME_RETURN,
                Locomotion.FOLLOW_PATH, Locomotion.SOCIAL_MOVE,
                Locomotion.IDLE_EXPLORE, Locomotion.STOP, Locomotion.NONE
        };
        for (int left = 0; left < ordered.length; left++) {
            for (int right = left + 1; right < ordered.length; right++) {
                assertTrue(ordered[left].priority() < ordered[right].priority(),
                        ordered[left] + " must beat " + ordered[right]);
            }
        }
        assertEquals(Locomotion.RESCUE_FALL.priority(), Locomotion.RESCUE_FIRE.priority());
        assertEquals(Locomotion.GUARD_INTERPOSE.priority(), Locomotion.COMBAT_CHASE.priority());
        assertEquals(Locomotion.EXTERNAL_INTERACTION_STOP.priority(), Locomotion.CURIOSITY_APPROACH.priority());
    }

    @Test
    void resolverReturnsAtMostOneLocomotionLookAndTargetPolicy() {
        Decision decision = coordinator.resolve(followSnapshot(false, false, false), State.initial(),
                new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH), candidate(Locomotion.SOCIAL_MOVE)),
                        List.of(look(Look.LOOK_WALK_TARGET), look(Look.LOOK_FRIEND)),
                        List.of(target(TargetPolicy.SET_HOSTILE), target(TargetPolicy.CLEAR))));
        assertEquals(Locomotion.FOLLOW_PATH, decision.locomotion().type());
        assertEquals(Look.LOOK_FRIEND, decision.look().type());
        assertEquals(TargetPolicy.SET_HOSTILE, decision.targetPolicy().policy());
    }

    @Test
    void selfRescueBeatsFriendRescueImmobilizationPassengerGuardAndFollow() {
        List<LocomotionCandidate> conflicts = List.of(
                candidate(Locomotion.FOLLOW_PATH), candidate(Locomotion.GUARD_INTERPOSE),
                candidate(Locomotion.PASSENGER_CONTROLLED), candidate(Locomotion.IMMOBILE_WOUNDED),
                candidate(Locomotion.RESCUE_FIRE), candidate(Locomotion.ESCAPE_WATER));
        Decision decision = coordinator.resolve(followSnapshot(true, true, false), State.initial(),
                new Candidates(conflicts, List.of(), List.of()));
        assertEquals(Locomotion.ESCAPE_WATER, decision.locomotion().type());
    }

    @Test
    void restAndStayBlockFollowAndIdleWhileHomeAcceptsOnlyHomeMovement() {
        Candidates candidates = new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH),
                candidate(Locomotion.IDLE_EXPLORE), candidate(Locomotion.HOME_RETURN)), List.of(), List.of());
        assertEquals(Locomotion.NONE, coordinator.resolve(snapshot(Command.STAY), State.initial(), candidates).locomotion().type());
        assertEquals(Locomotion.NONE, coordinator.resolve(snapshot(Command.REST), State.initial(), candidates).locomotion().type());
        assertEquals(Locomotion.HOME_RETURN,
                coordinator.resolve(snapshot(Command.HOME), State.initial(), candidates).locomotion().type());
    }

    @Test
    void restImmobilizationRejectsGuardCombatAndProtectionTarget() {
        LocomotionCandidate rest = candidate(Locomotion.IMMOBILE_REST);
        TargetCandidate clear = target(TargetPolicy.CLEAR);
        Decision decision = coordinator.resolve(snapshot(Command.REST), State.initial(),
                new Candidates(List.of(candidate(Locomotion.GUARD_INTERPOSE),
                        candidate(Locomotion.COMBAT_CHASE), rest),
                        List.of(look(Look.LOOK_HOSTILE)),
                        List.of(target(TargetPolicy.SET_HOSTILE), clear)));
        assertSame(rest, decision.locomotion());
        assertEquals(Look.LOCK_SEATED, decision.look().type());
        assertSame(clear, decision.targetPolicy());
    }

    @Test
    void restFixDoesNotLowerDownedHealingCarryOrWaterSafetyPriority() {
        Candidates candidates = new Candidates(List.of(candidate(Locomotion.PASSENGER_CONTROLLED),
                candidate(Locomotion.IMMOBILE_HEALING), candidate(Locomotion.IMMOBILE_DOWNED),
                candidate(Locomotion.IMMOBILE_REST), candidate(Locomotion.ESCAPE_WATER)), List.of(), List.of());
        Snapshot wetRest = new Snapshot(Command.REST, false, true, false, true, true, true,
                false, false, false, false, false, false, false, RecoveryOutcome.NONE);
        assertEquals(Locomotion.ESCAPE_WATER,
                coordinator.resolve(wetRest, State.initial(), candidates).locomotion().type());

        Snapshot dryRest = new Snapshot(Command.REST, false, true, false, true, false, true,
                false, false, false, false, false, false, false, RecoveryOutcome.NONE);
        assertEquals(Locomotion.IMMOBILE_HEALING,
                coordinator.resolve(dryRest, State.initial(), candidates).locomotion().type());
    }

    @Test
    void restForcesSeatedLookAndWoundedPreservesBaselineLook() {
        Candidates rest = new Candidates(List.of(candidate(Locomotion.IMMOBILE_REST)),
                List.of(look(Look.LOOK_HOSTILE)), List.of());
        assertEquals(Look.LOCK_SEATED,
                coordinator.resolve(snapshot(Command.REST), State.initial(), rest).look().type());

        Candidates wounded = new Candidates(List.of(candidate(Locomotion.IMMOBILE_WOUNDED)),
                List.of(look(Look.LOOK_HOSTILE)), List.of());
        assertEquals(Look.LOOK_HOSTILE,
                coordinator.resolve(followSnapshot(false, false, false), State.initial(), wounded).look().type());
    }

    @Test
    void passengerWinsOrdinaryNavigationButWaterEscapeStillWinsPassenger() {
        Candidates ordinary = new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH),
                candidate(Locomotion.PASSENGER_CONTROLLED)), List.of(), List.of());
        assertEquals(Locomotion.PASSENGER_CONTROLLED,
                coordinator.resolve(followSnapshot(false, true, false), State.initial(), ordinary).locomotion().type());

        Candidates water = new Candidates(List.of(candidate(Locomotion.PASSENGER_CONTROLLED),
                candidate(Locomotion.ESCAPE_WATER)), List.of(), List.of());
        assertEquals(Locomotion.ESCAPE_WATER,
                coordinator.resolve(followSnapshot(true, true, false), State.initial(), water).locomotion().type());
    }

    @Test
    void waterEscapeBeatsSimultaneousFireEscapeAndPassenger() {
        Candidates candidates = new Candidates(List.of(candidate(Locomotion.ESCAPE_FIRE),
                candidate(Locomotion.PASSENGER_CONTROLLED), candidate(Locomotion.ESCAPE_WATER)),
                List.of(), List.of());
        assertEquals(Locomotion.ESCAPE_WATER,
                coordinator.resolve(followSnapshot(true, true, false), State.initial(), candidates).locomotion().type());
    }

    @Test
    void followDistanceStopIsAllowedEvenWhileWaterPauseIsNotLatched() {
        Candidates candidates = only(Locomotion.FOLLOW_DISTANCE_STOP);
        assertEquals(Locomotion.FOLLOW_DISTANCE_STOP,
                coordinator.resolve(followSnapshot(false, false, false), State.initial(), candidates).locomotion().type());
    }

    @Test
    void laterProtectionSetBeatsEarlierMeleeClear() {
        TargetCandidate clear = target(TargetPolicy.CLEAR);
        TargetCandidate set = target(TargetPolicy.SET_HOSTILE);
        Decision decision = coordinator.resolve(followSnapshot(false, false, false), State.initial(),
                new Candidates(List.of(), List.of(), List.of(clear, set)));
        assertSame(set, decision.targetPolicy());
    }

    @Test
    void combatGateBlocksRestDownedVehicleAndEmergenciesButAllowsAcceptedGuardTarget() {
        CombatSnapshot allowed = new CombatSnapshot(true, false, false, false,
                false, false, false, false, true);
        assertTrue(FadedMovementCoordinator.isCombatAllowed(allowed));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, true,
                false, false, false, false, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, true, false, false,
                false, false, false, false, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, false,
                true, false, false, false, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, false,
                false, true, false, false, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, false,
                false, false, true, false, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, false,
                false, false, false, true, true)));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(new CombatSnapshot(true, false, false, false,
                false, false, false, false, false)));
    }

    @Test
    void combatGateImmediatelyRejectsChangedOrInvalidatedProtectionTarget() {
        CombatSnapshot acquired = new CombatSnapshot(true, false, false, false,
                false, false, false, false, true);
        CombatSnapshot invalidated = new CombatSnapshot(true, false, false, false,
                false, false, false, false, false);
        assertTrue(FadedMovementCoordinator.isCombatAllowed(acquired));
        assertFalse(FadedMovementCoordinator.isCombatAllowed(invalidated));
    }

    @Test
    void homeLongReturnCanWinOnTickAfterHigherSafetyIntentEnds() {
        LocomotionCandidate homeLongReturn = candidate(Locomotion.HOME_RETURN);
        Decision first = coordinator.resolve(snapshot(Command.HOME), State.initial(),
                new Candidates(List.of(homeLongReturn, candidate(Locomotion.ESCAPE_FIRE)), List.of(), List.of()));
        assertEquals(Locomotion.ESCAPE_FIRE, first.locomotion().type());
        Decision second = coordinator.resolve(snapshot(Command.HOME), first.nextState(),
                new Candidates(List.of(homeLongReturn), List.of(), List.of()));
        assertSame(homeLongReturn, second.locomotion());
    }

    @Test
    void externalInteractionStopBeatsHomeFollowSocialAndIdleButNotSafetyOrCarry() {
        LocomotionCandidate externalStop = candidate(Locomotion.EXTERNAL_INTERACTION_STOP);
        Candidates ordinary = new Candidates(List.of(candidate(Locomotion.IDLE_EXPLORE),
                candidate(Locomotion.SOCIAL_MOVE), candidate(Locomotion.FOLLOW_PATH), externalStop),
                List.of(), List.of());
        Decision stopped = coordinator.resolve(followSnapshot(false, false, false), State.initial(), ordinary);
        assertSame(externalStop, stopped.locomotion());

        for (Locomotion higher : List.of(Locomotion.ESCAPE_WATER, Locomotion.RESCUE_FALL,
                Locomotion.IMMOBILE_WOUNDED, Locomotion.PASSENGER_CONTROLLED,
                Locomotion.GUARD_INTERPOSE)) {
            Snapshot snapshot = higher == Locomotion.ESCAPE_WATER
                    ? followSnapshot(true, true, false)
                    : followSnapshot(false, true, false);
            Decision decision = coordinator.resolve(snapshot, State.initial(),
                    new Candidates(List.of(externalStop, candidate(higher)), List.of(), List.of()));
            assertEquals(higher, decision.locomotion().type(), higher.name());
        }

        Decision homeStopped = coordinator.resolve(snapshot(Command.HOME), State.initial(),
                new Candidates(List.of(candidate(Locomotion.HOME_RETURN), externalStop), List.of(), List.of()));
        assertSame(externalStop, homeStopped.locomotion());
    }

    @Test
    void commandTransitionWithoutInteractionLatchLetsNewHomeOrFollowTakeControl() {
        Decision toHome = coordinator.resolve(snapshot(Command.HOME), State.initial(),
                new Candidates(List.of(candidate(Locomotion.HOME_RETURN), candidate(Locomotion.STOP)),
                        List.of(), List.of()));
        assertEquals(Locomotion.HOME_RETURN, toHome.locomotion().type());

        Decision fromHomeToFollow = coordinator.resolve(followSnapshot(false, false, false), State.initial(),
                new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH), candidate(Locomotion.STOP)),
                        List.of(), List.of()));
        assertEquals(Locomotion.FOLLOW_PATH, fromHomeToFollow.locomotion().type());
    }

    @Test
    void curiosityApproachBeatsOrdinaryMovementButYieldsToSafetyAndCommands() {
        LocomotionCandidate curiosity = candidate(Locomotion.CURIOSITY_APPROACH);
        Decision ordinary = coordinator.resolve(followSnapshot(false, false, false), State.initial(),
                new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH), candidate(Locomotion.SOCIAL_MOVE),
                        candidate(Locomotion.IDLE_EXPLORE), curiosity), List.of(), List.of()));
        assertSame(curiosity, ordinary.locomotion());

        for (Locomotion higher : List.of(Locomotion.ESCAPE_WATER, Locomotion.ESCAPE_FIRE,
                Locomotion.RESCUE_FALL, Locomotion.IMMOBILE_DOWNED, Locomotion.PASSENGER_CONTROLLED,
                Locomotion.GUARD_INTERPOSE)) {
            Snapshot snapshot = higher == Locomotion.ESCAPE_WATER
                    ? followSnapshot(true, true, false) : followSnapshot(false, true, false);
            assertEquals(higher, coordinator.resolve(snapshot, State.initial(),
                    new Candidates(List.of(curiosity, candidate(higher)), List.of(), List.of()))
                    .locomotion().type(), higher.name());
        }
        assertEquals(Locomotion.NONE, coordinator.resolve(snapshot(Command.HOME), State.initial(),
                only(Locomotion.CURIOSITY_APPROACH)).locomotion().type());
        assertEquals(Locomotion.NONE, coordinator.resolve(snapshot(Command.REST), State.initial(),
                only(Locomotion.CURIOSITY_APPROACH)).locomotion().type());
    }

    @Test
    void waterPauseLatchesAndOnlyDryGroundedSolidFriendReleasesIt() {
        Decision wet = coordinator.resolve(followSnapshot(false, false, true), State.initial(),
                only(Locomotion.FOLLOW_WATER_PAUSE));
        assertTrue(wet.nextState().waterFollowPaused());
        assertEquals(Locomotion.FOLLOW_WATER_PAUSE, wet.locomotion().type());

        Snapshot dryButUnsupported = new Snapshot(Command.FOLLOW, false, true, false, false,
                false, false, false, false, true, false, false, false, false, RecoveryOutcome.NONE);
        State stillPaused = coordinator.resolve(dryButUnsupported, wet.nextState(), Candidates.empty()).nextState();
        assertTrue(stillPaused.waterFollowPaused());

        State released = coordinator.resolve(followSnapshot(false, false, false), stillPaused,
                Candidates.empty()).nextState();
        assertFalse(released.waterFollowPaused());
    }

    @Test
    void barrierRecoveryRequiresConjunctionEvidenceThresholdAndNoCooldown() {
        State ready = new State(false, WATER_BARRIER_STUCK_TICKS - 1,
                REQUIRED_WATER_EVIDENCE_TICKS - 1, 0, false);
        Decision eligible = coordinator.resolve(barrierSnapshot(false, RecoveryOutcome.NONE), ready,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY));
        assertEquals(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY, eligible.locomotion().type());

        Snapshot unsafe = new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                false, false, true, true, true, false, false, RecoveryOutcome.NONE);
        assertEquals(Locomotion.NONE,
                coordinator.resolve(unsafe, ready, only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).locomotion().type());

        State cooling = new State(false, WATER_BARRIER_STUCK_TICKS, REQUIRED_WATER_EVIDENCE_TICKS, 1, false);
        assertEquals(Locomotion.NONE, coordinator.resolve(barrierSnapshot(false, RecoveryOutcome.NONE), cooling,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).locomotion().type());
    }

    @Test
    void waterEvidenceRequiresTwoFreshPositiveObservationsAndPersistsBetweenScans() {
        Snapshot positiveFresh = barrierSnapshot(false, RecoveryOutcome.NONE);
        State first = coordinator.resolve(positiveFresh, State.initial(), Candidates.empty()).nextState();
        assertEquals(1, first.waterEvidenceTicks());

        Snapshot noNewScan = new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                false, false, true, true, false, true, false, RecoveryOutcome.NONE);
        State between = coordinator.resolve(noNewScan, first, Candidates.empty()).nextState();
        assertEquals(1, between.waterEvidenceTicks());

        State second = coordinator.resolve(positiveFresh, between, Candidates.empty()).nextState();
        assertEquals(REQUIRED_WATER_EVIDENCE_TICKS, second.waterEvidenceTicks());
    }

    @Test
    void freshNegativeWaterScanResetsWholeStuckEpisode() {
        State stuck = new State(false, WATER_BARRIER_STUCK_TICKS, REQUIRED_WATER_EVIDENCE_TICKS, 0, false);
        Snapshot ordinaryWallOrFence = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, false, true, false, true, false, false, RecoveryOutcome.NONE);
        State reset = coordinator.resolve(ordinaryWallOrFence, stuck,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).nextState();
        assertEquals(0, reset.noProgressTicks());
        assertEquals(0, reset.waterEvidenceTicks());
        assertEquals(Locomotion.NONE, coordinator.resolve(ordinaryWallOrFence, stuck,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).locomotion().type());
    }

    @Test
    void friendInWaterForcesPauseAndCannotUseBarrierRecovery() {
        State ready = new State(false, WATER_BARRIER_STUCK_TICKS,
                REQUIRED_WATER_EVIDENCE_TICKS, 0, false);
        Snapshot friendWet = new Snapshot(Command.FOLLOW, false, true, true, false,
                false, false, false, false, true, true, true, true, false, RecoveryOutcome.NONE);
        Decision decision = coordinator.resolve(friendWet, ready,
                new Candidates(List.of(candidate(Locomotion.FOLLOW_WATER_PAUSE),
                        candidate(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)), List.of(), List.of()));
        assertEquals(Locomotion.FOLLOW_WATER_PAUSE, decision.locomotion().type());
        assertTrue(decision.nextState().waterFollowPaused());
    }

    @Test
    void missingSafeLandingStartsCooldownWithoutRecoveryCandidate() {
        State ready = new State(false, WATER_BARRIER_STUCK_TICKS,
                REQUIRED_WATER_EVIDENCE_TICKS, 0, false);
        Snapshot noLanding = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, false, true, true, false, false, false, RecoveryOutcome.NO_TARGET);
        Decision decision = coordinator.resolve(noLanding, ready,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY));
        assertEquals(Locomotion.NONE, decision.locomotion().type());
        assertEquals(RETRY_COOLDOWN_TICKS, decision.nextState().recoveryCooldownTicks());
    }

    @Test
    void pathNullNoProgressOrWaterAloneCannotTriggerRecovery() {
        State initial = State.initial();
        Snapshot noEvidence = new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                false, false, true, false, true, true, false, RecoveryOutcome.NONE);
        assertEquals(Locomotion.NONE, coordinator.resolve(noEvidence,
                new State(false, 200, 0, 0, false), only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).locomotion().type());

        Snapshot evidenceWithoutTime = barrierSnapshot(false, RecoveryOutcome.NONE);
        assertEquals(Locomotion.NONE, coordinator.resolve(evidenceWithoutTime, initial,
                only(Locomotion.FOLLOW_WATER_BARRIER_RECOVERY)).locomotion().type());
    }

    @Test
    void progressReachablePathCommandChangeAndLostEvidenceResetEpisode() {
        State stuck = new State(false, 79, 4, 0, true);
        for (Snapshot snapshot : List.of(
                barrierSnapshot(true, RecoveryOutcome.NONE),
                new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                        false, true, true, true, true, true, false, RecoveryOutcome.NONE),
                new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                        false, false, true, false, true, true, false, RecoveryOutcome.NONE),
                new Snapshot(Command.FOLLOW, true, true, false, true, false, false,
                        false, false, true, true, true, true, false, RecoveryOutcome.NONE))) {
            State reset = coordinator.resolve(snapshot, stuck, Candidates.empty()).nextState();
            assertEquals(0, reset.noProgressTicks());
            assertEquals(0, reset.waterEvidenceTicks());
        }
    }

    @Test
    void everyRecoveryOutcomeStartsExactRetryCooldown() {
        for (RecoveryOutcome outcome : List.of(RecoveryOutcome.SUCCESS,
                RecoveryOutcome.NO_TARGET, RecoveryOutcome.ROLLBACK)) {
            State next = coordinator.resolve(barrierSnapshot(false, outcome), State.initial(), Candidates.empty()).nextState();
            assertEquals(RETRY_COOLDOWN_TICKS, next.recoveryCooldownTicks(), outcome.name());
        }
        State decremented = coordinator.resolve(barrierSnapshot(false, RecoveryOutcome.NONE),
                new State(false, 0, 0, 2, false), Candidates.empty()).nextState();
        assertEquals(1, decremented.recoveryCooldownTicks());
    }

    @Test
    void successfulSelfRescueMarksEpisodeAndStartsCooldown() {
        Snapshot rescued = new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                false, false, true, false, false, false, true, RecoveryOutcome.NONE);
        State next = coordinator.resolve(rescued, State.initial(), Candidates.empty()).nextState();
        assertTrue(next.selfRescuedInFollowEpisode());
        assertEquals(RETRY_COOLDOWN_TICKS, next.recoveryCooldownTicks());
    }

    @Test
    void selfRescueLatchBlocksWaterRouteFollowUntilDryReachablePathReleasesIt() {
        State latched = new State(false, 12, 1, 73, true);
        Snapshot waterRouteStillReportedReachableByVanilla = new Snapshot(Command.FOLLOW, false,
                true, false, true, false, false, false, false, true,
                true, false, true, false, RecoveryOutcome.NONE);
        Decision blocked = coordinator.resolve(waterRouteStillReportedReachableByVanilla, latched,
                new Candidates(List.of(candidate(Locomotion.FOLLOW_PATH), candidate(Locomotion.SOCIAL_MOVE),
                        candidate(Locomotion.IDLE_EXPLORE), candidate(Locomotion.GUARD_INTERPOSE),
                        candidate(Locomotion.COMBAT_CHASE)), List.of(), List.of()));
        assertEquals(Locomotion.NONE, blocked.locomotion().type());
        assertTrue(blocked.nextState().selfRescuedInFollowEpisode());

        Snapshot verifiedDryRoute = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, true, true, false, false, false, false, RecoveryOutcome.NONE);
        Decision released = coordinator.resolve(verifiedDryRoute, blocked.nextState(), only(Locomotion.FOLLOW_PATH));
        assertEquals(Locomotion.FOLLOW_PATH, released.locomotion().type());
        assertFalse(released.nextState().selfRescuedInFollowEpisode());

        Snapshot wetEmergency = new Snapshot(Command.FOLLOW, false, true, false, true,
                true, true, false, false, true, true, false, false, false, RecoveryOutcome.NONE);
        Decision safety = coordinator.resolve(wetEmergency, latched,
                new Candidates(List.of(candidate(Locomotion.ESCAPE_WATER),
                        candidate(Locomotion.PASSENGER_CONTROLLED)), List.of(), List.of()));
        assertEquals(Locomotion.ESCAPE_WATER, safety.locomotion().type());
    }

    @Test
    void friendMovementAndTargetRebindResetLatchButOwnRescueMovementDoesNot() {
        State latched = new State(false, 47, 3, 61, true);
        Snapshot friendMoved = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, true, false, false, true, true,
                false, false, false, RecoveryOutcome.NONE);
        State moved = coordinator.resolve(friendMoved, latched, Candidates.empty()).nextState();
        assertEquals(0, moved.noProgressTicks());
        assertEquals(0, moved.waterEvidenceTicks());
        assertFalse(moved.selfRescuedInFollowEpisode());

        Snapshot targetChanged = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, false, true, false, true, true,
                false, false, false, RecoveryOutcome.NONE);
        State rebound = coordinator.resolve(targetChanged, latched, Candidates.empty()).nextState();
        assertEquals(0, rebound.noProgressTicks());
        assertEquals(0, rebound.waterEvidenceTicks());
        assertFalse(rebound.selfRescuedInFollowEpisode());

        Snapshot ownRescueTeleport = new Snapshot(Command.FOLLOW, false, true, false, true,
                false, false, false, false, false, false, true, true,
                false, false, true, RecoveryOutcome.NONE);
        State selfRescued = coordinator.resolve(ownRescueTeleport, latched, Candidates.empty()).nextState();
        assertTrue(selfRescued.selfRescuedInFollowEpisode());
    }

    @Test
    void candidatesAndRecordsAreImmutableAndValidated() {
        ArrayList<LocomotionCandidate> mutable = new ArrayList<>();
        mutable.add(candidate(Locomotion.STOP));
        Candidates candidates = new Candidates(mutable, List.of(), List.of());
        mutable.clear();
        assertEquals(1, candidates.locomotion().size());
        assertThrows(UnsupportedOperationException.class, () -> candidates.locomotion().clear());
        assertThrows(IllegalArgumentException.class, () -> new State(false, -1, 0, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new LocomotionCandidate(Locomotion.FOLLOW_PATH, null, Double.NaN, "bad"));
    }

    @Test
    void coordinatorBytecodeHasNoGameFrameworkOrRandomDependency() throws IOException {
        assertPureBytecode(FadedMovementCoordinator.class);
        for (Class<?> nested : FadedMovementCoordinator.class.getDeclaredClasses()) assertPureBytecode(nested);
    }

    private static void assertPureBytecode(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream stream = type.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            String pool = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertFalse(pool.contains("net/minecraft"), type.getName());
            assertFalse(pool.contains("net/minecraftforge"), type.getName());
            assertFalse(pool.contains("software/bernie"), type.getName());
            assertFalse(pool.contains("net/tslat/smartbrainlib"), type.getName());
            assertFalse(pool.contains("java/util/Random"), type.getName());
        }
    }

    private static Candidates only(Locomotion type) {
        return new Candidates(List.of(candidate(type)), List.of(), List.of());
    }

    private static LocomotionCandidate candidate(Locomotion type) {
        return new LocomotionCandidate(type, "target", 1.0D, "test");
    }

    private static LookCandidate look(Look type) { return new LookCandidate(type, "target", "test"); }
    private static TargetCandidate target(TargetPolicy policy) { return new TargetCandidate(policy, "target", "test"); }

    private static Snapshot snapshot(Command command) {
        return new Snapshot(command, false, true, false, true, false, false,
                false, false, true, false, false, false, false, RecoveryOutcome.NONE);
    }

    private static Snapshot followSnapshot(boolean endermanWet, boolean passenger, boolean friendWet) {
        return new Snapshot(Command.FOLLOW, false, true, friendWet, !friendWet, endermanWet, passenger,
                false, false, true, false, false, false, false, RecoveryOutcome.NONE);
    }

    private static Snapshot barrierSnapshot(boolean progress, RecoveryOutcome outcome) {
        return new Snapshot(Command.FOLLOW, false, true, false, true, false, false,
                progress, false, true, true, true, true, false, outcome);
    }
}
