package pl.fadedpearl.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CarryMultiplayerRenderContractTest {
    @Test
    void carryMovementComesFromServerSyncedStateForEveryObserver() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/entity/FadedEnderman.java"));
        assertTrue(source.contains("EntityDataAccessor<Boolean> CARRY_MOVING"));
        assertTrue(source.contains("entityData.define(CARRY_MOVING, false)"));
        assertTrue(source.contains("syncCarryAnimationMovement()"));
        assertTrue(source.contains("currentX - lastCarryAnimationX"));
        assertTrue(source.contains("carryMovementAnimationTicks = 3"));
        assertTrue(source.contains("entityData.get(CARRY_MOVING) || localRiderMoving"));
        assertFalse(source.contains("getX() - xo"));
    }

    @Test
    void remoteClientsDoNotNeedUnsyncedFriendIdToRenderPassengerPose() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/CarryRenderEvents.java"));
        assertTrue(source.contains("player.getVehicle() instanceof FadedEnderman"));
        assertFalse(source.contains("enderman.isFriend(player)"));
    }

    @Test
    void carriedPlayerRemainsUprightAndBodyYawFollowsCarrierWithoutTiltingCamera() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/CarryRenderEvents.java"));
        assertTrue(source.contains("player.setYBodyRot(carriedYaw)"));
        assertTrue(source.contains("player.yBodyRotO = carriedYaw"));
        assertFalse(source.contains("Axis.ZP"));
        assertFalse(source.contains("setYRot("));
    }
}
