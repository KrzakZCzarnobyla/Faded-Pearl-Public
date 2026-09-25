package pl.fadedpearl.entity.behavior;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanionCombatContractTest {
    @Test
    void combatBlinkUsesMovementAdapterAndLiveSafeLanding() throws Exception {
        String entity = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String blink = entity.substring(entity.indexOf("private boolean tryCombatBlink"),
                entity.indexOf("private boolean blinkWithRider"));
        String melee = entity.substring(entity.indexOf("private final class ArbitratedMeleeAttackGoal"),
                entity.indexOf("private final class FollowFriendGoal"));
        assertTrue(blink.contains("serverLevel.hasChunkAt(feet)"));
        assertTrue(blink.contains("isSafeHomeLanding(serverLevel, feet)"));
        assertTrue(blink.contains("trySafeTeleport("));
        assertTrue(melee.contains("queueLocomotion(FadedMovementCoordinator.Locomotion.COMBAT_CHASE"));
        assertTrue(melee.contains("tryCombatBlink(chaseTarget)"));
        assertTrue(melee.contains("ticksUntilNextAttack = adjustedTickDelay(20)"));
    }

    @Test
    void downedStatusIsSyncedAndNeverChangesReviveInteraction() throws Exception {
        String entity = Files.readString(Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        String renderer = Files.readString(Path.of("src/main/java/pl/fadedpearl/client/FadedEndermanGeoRenderer.java"));
        assertTrue(entity.contains("entityData.define(DOWNED_SECONDS, 0)"));
        assertTrue(entity.contains("CompanionRecoveryStatus.secondsUntilStand(downedTicks)"));
        assertTrue(renderer.contains("status.faded_pearl.downed"));
        assertFalse(renderer.contains("status.faded_pearl.regenerating"));
        assertTrue(entity.contains("case RECOVER_DOWNED ->"));
    }
}
