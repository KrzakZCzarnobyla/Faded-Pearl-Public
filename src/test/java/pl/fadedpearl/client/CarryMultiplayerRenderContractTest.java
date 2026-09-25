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
        assertTrue(source.contains("carrying ? entityData.get(CARRY_MOVING) : state.isMoving()"));
    }

    @Test
    void remoteClientsDoNotNeedUnsyncedFriendIdToRenderPassengerPose() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/pl/fadedpearl/client/CarryRenderEvents.java"));
        assertTrue(source.contains("player.getVehicle() instanceof FadedEnderman"));
        assertFalse(source.contains("enderman.isFriend(player)"));
    }
}
