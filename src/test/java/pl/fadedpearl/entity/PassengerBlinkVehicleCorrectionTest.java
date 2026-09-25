package pl.fadedpearl.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassengerBlinkVehicleCorrectionTest {
    private static final Path SOURCE = Path.of("src/main/java/pl/fadedpearl/entity/FadedEnderman.java");

    @Test
    void sendsVehicleThenPlayerCorrectionsOnlyAfterSuccessfulLandingValidation() throws IOException {
        String source = Files.readString(SOURCE);
        String method = source.substring(source.indexOf("private boolean trySafeTeleportWithPassenger("),
                source.indexOf("private void teleportNearFriend("));

        String rollbackGate = "if (!hasPassenger(rider) || !level().noCollision(this) "
                + "|| level().containsAnyLiquid(getBoundingBox())";
        String vehicleCorrection = "rider.connection.send(new ClientboundMoveVehiclePacket(this));";
        String playerCorrection = "rider.connection.teleport(rider.getX(), rider.getY(), rider.getZ(), "
                + "rider.getYRot(), rider.getXRot());";

        assertEquals(1, occurrences(source, vehicleCorrection));
        assertEquals(1, occurrences(source, playerCorrection));
        assertTrue(method.indexOf(rollbackGate) >= 0);
        assertTrue(method.indexOf("return false;", method.indexOf(rollbackGate)) < method.indexOf(vehicleCorrection));
        assertTrue(method.indexOf(vehicleCorrection) < method.indexOf(playerCorrection));
        assertTrue(method.indexOf(playerCorrection) < method.lastIndexOf("return true;"));
    }

    @Test
    void keepsServerPlayerTypedFlowAndCooldownBehindSuccessfulBlink() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("private boolean blinkWithRider(ServerPlayer rider)"));
        assertTrue(source.contains("private boolean isVisibleSafePassengerLanding(ServerPlayer rider"));
        assertTrue(source.contains("private boolean trySafeTeleportWithPassenger(ServerPlayer rider"));
        assertTrue(source.contains("if (blinkWithRider(player)) setCarriedBlinkCooldown(80);"));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
